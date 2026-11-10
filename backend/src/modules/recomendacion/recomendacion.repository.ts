import { RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { Coincidencia } from './recomendacion.avisos';

/** Capa de acceso a datos de las notificaciones por nuevas recomendaciones (RF26). */

/** Reglas incorporadas al repositorio de las que aún no se avisó a los usuarios. */
export async function reglasSinNotificar(): Promise<number[]> {
  const [filas] = await pool.query<RowDataPacket[]>('SELECT id FROM regla WHERE fecha_notificacion IS NULL ORDER BY id');
  return filas.map((fila) => fila.id as number);
}

/**
 * Mascotas activas a las que les corresponde alguna de esas reglas (misma especie y, si la regla lo exige,
 * mismo sexo).
 */
export async function coincidencias(reglasIds: number[]): Promise<Coincidencia[]> {
  if (reglasIds.length === 0) return [];
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT m.usuario_id AS usuarioId, m.id AS mascotaId, m.nombre AS mascota, r.id AS reglaId
     FROM regla r
     JOIN mascota m ON m.especie_id = r.especie_id
     WHERE r.id IN (?)
       AND m.estado = 'activa'
       AND (r.sexo_aplicable IS NULL OR r.sexo_aplicable = m.sexo)
     ORDER BY m.id, r.id`,
    [reglasIds],
  );
  return filas as Coincidencia[];
}

/** Deja constancia de que las reglas ya se avisaron (fecha de notificación): dejan de ser "nuevas". */
export async function marcarNotificadas(reglasIds: number[], fecha: string): Promise<void> {
  if (reglasIds.length === 0) return;
  await pool.query('UPDATE regla SET fecha_notificacion = ? WHERE id IN (?)', [fecha, reglasIds]);
}
