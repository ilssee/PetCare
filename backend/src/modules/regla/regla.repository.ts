import { RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { ReglaDisponible } from './regla.types';

/** Capa de acceso a datos del módulo regla. */

/**
 * Reglas que aplican a la mascota (misma especie y, si la regla lo exige, mismo sexo)
 * y que todavía no están en su cronograma. Si el usuario elimina un cuidado recomendado,
 * su regla vuelve a aparecer aquí (RF15).
 */
export async function listarDisponibles(mascotaId: number): Promise<ReglaDisponible[]> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT r.id, r.nombre, r.descripcion, r.importancia
     FROM regla r
     JOIN mascota m ON m.especie_id = r.especie_id
     WHERE m.id = ?
       AND (r.sexo_aplicable IS NULL OR r.sexo_aplicable = m.sexo)
       AND NOT EXISTS (
         SELECT 1 FROM cuidado c
         WHERE c.mascota_id = m.id AND c.regla_id = r.id AND c.estado <> 'eliminado')
     ORDER BY r.id`,
    [mascotaId],
  );
  return filas as ReglaDisponible[];
}
