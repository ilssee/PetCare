import { ResultSetHeader, RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { ConfiguracionCercania, LugarNuevo, UsuarioCercano } from './cercania.types';

/** Capa de acceso a datos del módulo cercanía. */

const aNumeroONulo = (valor: unknown): number | null => (valor === null || valor === undefined ? null : Number(valor));

export async function obtener(usuarioId: number): Promise<ConfiguracionCercania> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT ubicacion_ref_lat AS lat, ubicacion_ref_long AS lng, radio_interes AS radioKm,
            notificar_nuevas_campanas AS campanas, notificar_nuevas_veterinarias AS veterinarias
     FROM preferencias WHERE usuario_id = ?`,
    [usuarioId],
  );
  const f = filas[0];
  return {
    lat: aNumeroONulo(f?.lat),
    lng: aNumeroONulo(f?.lng),
    radioKm: aNumeroONulo(f?.radioKm),
    notificarCampanas: Boolean(f?.campanas),
    notificarVeterinarias: Boolean(f?.veterinarias),
  };
}

/** Guarda la configuración. Todo usuario tiene sus preferencias; el INSERT es solo por si faltara la fila. */
export async function guardar(usuarioId: number, c: ConfiguracionCercania): Promise<void> {
  await pool.query(
    `INSERT INTO preferencias
       (usuario_id, ubicacion_ref_lat, ubicacion_ref_long, radio_interes, notificar_nuevas_campanas, notificar_nuevas_veterinarias)
     VALUES (?, ?, ?, ?, ?, ?)
     ON DUPLICATE KEY UPDATE
       ubicacion_ref_lat = VALUES(ubicacion_ref_lat),
       ubicacion_ref_long = VALUES(ubicacion_ref_long),
       radio_interes = VALUES(radio_interes),
       notificar_nuevas_campanas = VALUES(notificar_nuevas_campanas),
       notificar_nuevas_veterinarias = VALUES(notificar_nuevas_veterinarias)`,
    [usuarioId, c.lat, c.lng, c.radioKm, c.notificarCampanas, c.notificarVeterinarias],
  );
}

/** Usuarios que configuraron ubicación y radio y activaron al menos un tipo de aviso. */
export async function usuariosConAvisos(): Promise<UsuarioCercano[]> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT usuario_id, ubicacion_ref_lat AS lat, ubicacion_ref_long AS lng, radio_interes AS radioKm,
            notificar_nuevas_campanas AS campanas, notificar_nuevas_veterinarias AS veterinarias
     FROM preferencias
     WHERE ubicacion_ref_lat IS NOT NULL AND ubicacion_ref_long IS NOT NULL AND radio_interes IS NOT NULL
       AND (notificar_nuevas_campanas = TRUE OR notificar_nuevas_veterinarias = TRUE)`,
  );
  return filas.map((f) => ({
    usuarioId: f.usuario_id,
    lat: Number(f.lat),
    lng: Number(f.lng),
    radioKm: Number(f.radioKm),
    notificarCampanas: Boolean(f.campanas),
    notificarVeterinarias: Boolean(f.veterinarias),
  }));
}

/** Campañas que aún no terminaron y todavía no se avisaron. */
export async function campanasSinAvisar(hoy: string): Promise<LugarNuevo[]> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT c.id, c.nombre, o.nombre AS organizador, c.ubicacion_lat AS lat, c.ubicacion_long AS lng
     FROM campana c
     JOIN organizador o ON o.id = c.organizador_id
     WHERE c.fecha_notificacion IS NULL AND c.fecha_fin >= ?`,
    [hoy],
  );
  return filas.map((f) => ({
    tipo: 'campana' as const,
    id: f.id,
    nombre: f.nombre,
    organizador: f.organizador,
    lat: Number(f.lat),
    lng: Number(f.lng),
  }));
}

export async function veterinariasSinAvisar(): Promise<LugarNuevo[]> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT v.organizador_id AS id, o.nombre, v.ubicacion_lat AS lat, v.ubicacion_long AS lng
     FROM veterinaria v
     JOIN organizador o ON o.id = v.organizador_id
     WHERE v.fecha_notificacion IS NULL`,
  );
  return filas.map((f) => ({
    tipo: 'veterinaria' as const,
    id: f.id,
    nombre: f.nombre,
    organizador: f.nombre,
    lat: Number(f.lat),
    lng: Number(f.lng),
  }));
}

export async function marcarCampanasAvisadas(ids: number[], hoy: string): Promise<void> {
  if (ids.length === 0) return;
  await pool.query<ResultSetHeader>('UPDATE campana SET fecha_notificacion = ? WHERE id IN (?)', [hoy, ids]);
}

export async function marcarVeterinariasAvisadas(ids: number[], hoy: string): Promise<void> {
  if (ids.length === 0) return;
  await pool.query<ResultSetHeader>('UPDATE veterinaria SET fecha_notificacion = ? WHERE organizador_id IN (?)', [hoy, ids]);
}
