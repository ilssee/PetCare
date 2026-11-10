import { RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { CuidadoPorAvisar } from './recordatorio.avisos';
import { DIAS, Dia } from './recordatorio.dias';

/** Capa de acceso a datos del módulo recordatorio. */

/** Días elegidos por el usuario; si no eligió ninguno, la lista viene vacía. */
export async function obtenerDias(usuarioId: number): Promise<Dia[]> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT d.dia
     FROM preferencias p
     JOIN preferencias_dia d ON d.preferencias_id = p.id
     WHERE p.usuario_id = ?`,
    [usuarioId],
  );
  const guardados = new Set(filas.map((f) => f.dia as Dia));
  return DIAS.filter((dia) => guardados.has(dia)); // en orden de lunes a domingo
}

/** Reemplaza los días del usuario por [dias], en una sola transacción. */
export async function guardarDias(usuarioId: number, dias: Dia[]): Promise<void> {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();

    // Todo usuario registrado tiene sus preferencias; esto es solo por si faltara la fila.
    await conexion.query(
      `INSERT IGNORE INTO preferencias (usuario_id, notificar_nuevas_campanas, notificar_nuevas_veterinarias)
       VALUES (?, FALSE, FALSE)`,
      [usuarioId],
    );
    const [filas] = await conexion.query<RowDataPacket[]>('SELECT id FROM preferencias WHERE usuario_id = ?', [usuarioId]);
    const preferenciasId = filas[0].id as number;

    await conexion.query('DELETE FROM preferencias_dia WHERE preferencias_id = ?', [preferenciasId]);
    for (const dia of dias) {
      await conexion.query('INSERT INTO preferencias_dia (preferencias_id, dia) VALUES (?, ?)', [preferenciasId, dia]);
    }

    await conexion.commit();
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

/**
 * Cuidados pendientes de mascotas activas que podrían necesitar un aviso en [hoy]: los que ya llegaron
 * y los que vencen dentro de la próxima semana (el aviso anticipado se adelanta como máximo 7 días).
 * Los cuidados de una mascota inactiva no se avisan.
 */
export async function cuidadosParaAvisar(hoy: string): Promise<CuidadoPorAvisar[]> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT m.usuario_id AS usuarioId, c.id AS cuidadoId, c.nombre AS cuidado, m.nombre AS mascota,
            c.fecha_recurrente AS fechaRecurrente, c.aviso_anticipado AS avisoAnticipado
     FROM cuidado c
     JOIN mascota m ON m.id = c.mascota_id
     WHERE c.estado = 'pendiente' AND m.estado = 'activa'
       AND c.fecha_recurrente <= DATE_ADD(?, INTERVAL 7 DAY)
     ORDER BY m.usuario_id, c.fecha_recurrente, c.id`,
    [hoy],
  );
  return filas.map((f) => ({ ...f, avisoAnticipado: Boolean(f.avisoAnticipado) })) as CuidadoPorAvisar[];
}

/** Los días elegidos por cada uno de los usuarios indicados (solo los que configuraron alguno). */
export async function diasDeUsuarios(usuarioIds: number[]): Promise<Map<number, Dia[]>> {
  const porUsuario = new Map<number, Dia[]>();
  if (usuarioIds.length === 0) return porUsuario;
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT p.usuario_id AS usuarioId, d.dia
     FROM preferencias p
     JOIN preferencias_dia d ON d.preferencias_id = p.id
     WHERE p.usuario_id IN (?)`,
    [usuarioIds],
  );
  for (const fila of filas) {
    const dias = porUsuario.get(fila.usuarioId) ?? [];
    dias.push(fila.dia as Dia);
    porUsuario.set(fila.usuarioId, dias);
  }
  return porUsuario;
}
