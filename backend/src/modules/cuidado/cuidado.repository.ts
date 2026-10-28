import { ResultSetHeader, RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { CuidadoRespuesta, NuevoCuidado } from './cuidado.types';

/** Capa de acceso a datos del módulo cuidado. */

const SELECT_CUIDADO = `
  SELECT id, mascota_id AS mascotaId, regla_id AS reglaId, nombre, descripcion,
         comentario AS nota, modo_termino AS modoTermino,
         frecuencia_valor AS frecuenciaValor, frecuencia_unidad AS frecuenciaUnidad,
         total_repeticiones AS totalRepeticiones, fecha_limite AS fechaLimite,
         fecha_recurrente AS fechaRecurrente, aviso_anticipado AS avisoAnticipado, estado
  FROM cuidado`;

/**
 * Guarda varios cuidados de una mascota en una sola transacción:
 * o se activan todos los seleccionados, o ninguno.
 */
export async function crearVarios(mascotaId: number, cuidados: NuevoCuidado[]): Promise<number[]> {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();

    const ids: number[] = [];
    for (const c of cuidados) {
      const [resultado] = await conexion.query<ResultSetHeader>(
        `INSERT INTO cuidado
           (mascota_id, regla_id, nombre, descripcion, comentario, modo_termino,
            frecuencia_valor, frecuencia_unidad, total_repeticiones, fecha_limite,
            fecha_recurrente, aviso_anticipado)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          mascotaId, c.reglaId, c.nombre, c.descripcion, c.nota, c.modoTermino,
          c.frecuenciaValor, c.frecuenciaUnidad, c.totalRepeticiones, c.fechaLimite,
          c.fechaInicio, c.avisoAnticipado,
        ],
      );
      ids.push(resultado.insertId);
    }

    await conexion.commit();
    return ids;
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

export async function listarPorIds(ids: number[]): Promise<CuidadoRespuesta[]> {
  if (ids.length === 0) return [];
  const [filas] = await pool.query<RowDataPacket[]>(
    `${SELECT_CUIDADO} WHERE id IN (?) ORDER BY fecha_recurrente, id`,
    [ids],
  );
  // MySQL devuelve el BOOLEAN como 0 o 1.
  return filas.map((fila) => ({ ...fila, avisoAnticipado: Boolean(fila.avisoAnticipado) })) as CuidadoRespuesta[];
}
