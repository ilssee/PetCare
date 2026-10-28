import { ResultSetHeader, RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import {
  ActividadRespuesta,
  ConteoCuidados,
  CuidadoRespuesta,
  FiltroEstado,
  NuevoCuidado,
} from './cuidado.types';

/** Capa de acceso a datos del módulo cuidado. */

// Los cuidados recomendados traen además la importancia de su regla (los personalizados, null).
const SELECT_CUIDADO = `
  SELECT c.id, c.mascota_id AS mascotaId, c.regla_id AS reglaId, c.nombre, c.descripcion,
         r.importancia, c.comentario AS nota, c.modo_termino AS modoTermino,
         c.frecuencia_valor AS frecuenciaValor, c.frecuencia_unidad AS frecuenciaUnidad,
         c.total_repeticiones AS totalRepeticiones, c.fecha_limite AS fechaLimite,
         c.fecha_recurrente AS fechaRecurrente, c.aviso_anticipado AS avisoAnticipado, c.estado
  FROM cuidado c
  LEFT JOIN regla r ON r.id = c.regla_id`;

/** MySQL devuelve el BOOLEAN como 0 o 1. */
function aCuidados(filas: RowDataPacket[]): CuidadoRespuesta[] {
  return filas.map((fila) => ({ ...fila, avisoAnticipado: Boolean(fila.avisoAnticipado) })) as CuidadoRespuesta[];
}

/**
 * Guarda varios cuidados de una mascota en una sola transacción:
 * o se guardan todos, o ninguno.
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
    `${SELECT_CUIDADO} WHERE c.id IN (?) ORDER BY c.fecha_recurrente, c.id`,
    [ids],
  );
  return aCuidados(filas);
}

/** Cuidados de una mascota según el estado elegido. Pendientes primero, por fecha más próxima. */
export async function listarPorMascota(mascotaId: number, filtro: FiltroEstado): Promise<CuidadoRespuesta[]> {
  const estados = filtro === 'pendientes' ? ['pendiente'] : filtro === 'finalizados' ? ['finalizado'] : ['pendiente', 'finalizado'];
  const [filas] = await pool.query<RowDataPacket[]>(
    `${SELECT_CUIDADO}
     WHERE c.mascota_id = ? AND c.estado IN (?)
     ORDER BY FIELD(c.estado, 'pendiente', 'finalizado'), c.fecha_recurrente, c.nombre`,
    [mascotaId, estados],
  );
  return aCuidados(filas);
}

export async function contarPorEstado(mascotaId: number): Promise<ConteoCuidados> {
  const [filas] = await pool.query<RowDataPacket[]>(
    'SELECT estado, COUNT(*) AS total FROM cuidado WHERE mascota_id = ? AND estado <> ? GROUP BY estado',
    [mascotaId, 'eliminado'],
  );
  const de = (estado: string) => Number(filas.find((f) => f.estado === estado)?.total ?? 0);
  const pendientes = de('pendiente');
  const finalizados = de('finalizado');
  return { todos: pendientes + finalizados, pendientes, finalizados };
}

/** Un cuidado, solo si pertenece a una mascota del usuario. */
export async function buscarPropio(id: number, usuarioId: number): Promise<CuidadoRespuesta | null> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `${SELECT_CUIDADO}
     JOIN mascota m ON m.id = c.mascota_id
     WHERE c.id = ? AND m.usuario_id = ? AND c.estado <> 'eliminado'`,
    [id, usuarioId],
  );
  return aCuidados(filas)[0] ?? null;
}

/** Historial de varios cuidados: de la actividad más reciente a la más antigua. */
export async function listarActividades(cuidadoIds: number[]): Promise<ActividadRespuesta[]> {
  if (cuidadoIds.length === 0) return [];
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT id, cuidado_id AS cuidadoId, fecha_planificada AS fechaPlanificada,
            fecha_realizacion AS fechaRealizacion, estado, nota
     FROM actividad
     WHERE cuidado_id IN (?)
     ORDER BY fecha_realizacion DESC, id DESC`,
    [cuidadoIds],
  );
  return filas as ActividadRespuesta[];
}

export interface DatosEdicion {
  nombre: string;
  descripcion: string;
  nuevo: NuevoCuidado;
}

/** La fecha de inicio ingresada pasa a ser la próxima fecha del cuidado. */
export async function actualizar(id: number, datos: DatosEdicion): Promise<void> {
  const c = datos.nuevo;
  await pool.query(
    `UPDATE cuidado
     SET nombre = ?, descripcion = ?, comentario = ?, modo_termino = ?, frecuencia_valor = ?,
         frecuencia_unidad = ?, total_repeticiones = ?, fecha_limite = ?, fecha_recurrente = ?,
         aviso_anticipado = ?
     WHERE id = ?`,
    [
      datos.nombre, datos.descripcion, c.nota, c.modoTermino, c.frecuenciaValor,
      c.frecuenciaUnidad, c.totalRepeticiones, c.fechaLimite, c.fechaInicio,
      c.avisoAnticipado, id,
    ],
  );
}

/** Elimina el cuidado; su historial de actividades se borra con él (ON DELETE CASCADE). */
export async function eliminar(id: number): Promise<void> {
  await pool.query('DELETE FROM cuidado WHERE id = ?', [id]);
}
