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

function estadosDe(filtro: FiltroEstado): string[] {
  return filtro === 'pendientes' ? ['pendiente'] : filtro === 'finalizados' ? ['finalizado'] : ['pendiente', 'finalizado'];
}

/**
 * RF13: cuidados de todas las mascotas ACTIVAS del usuario (o solo de una), por fecha más próxima.
 * Los cuidados de una mascota inactiva no se muestran en el cronograma (RF07).
 */
export async function listarDeUsuario(
  usuarioId: number,
  mascotaId: number | null,
  filtro: FiltroEstado,
): Promise<CuidadoRespuesta[]> {
  const parametros: unknown[] = [usuarioId, estadosDe(filtro)];
  let filtroMascota = '';
  if (mascotaId !== null) {
    filtroMascota = 'AND c.mascota_id = ?';
    parametros.push(mascotaId);
  }
  const [filas] = await pool.query<RowDataPacket[]>(
    `${SELECT_CUIDADO}
     JOIN mascota m ON m.id = c.mascota_id
     WHERE m.usuario_id = ? AND m.estado = 'activa' AND c.estado IN (?) ${filtroMascota}
     ORDER BY FIELD(c.estado, 'pendiente', 'finalizado'), c.fecha_recurrente, c.nombre`,
    parametros,
  );
  return aCuidados(filas);
}

/** Cuidados de una mascota según el estado elegido. Pendientes primero, por fecha más próxima. */
export async function listarPorMascota(mascotaId: number, filtro: FiltroEstado): Promise<CuidadoRespuesta[]> {
  const estados = estadosDe(filtro);
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

/** Cuántos cuidados tiene el usuario en total (de todas sus mascotas, activas o no), por estado. */
export async function contarDeUsuario(usuarioId: number): Promise<ConteoCuidados> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT c.estado, COUNT(*) AS total
     FROM cuidado c
     JOIN mascota m ON m.id = c.mascota_id
     WHERE m.usuario_id = ? AND c.estado <> 'eliminado'
     GROUP BY c.estado`,
    [usuarioId],
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

/**
 * Historial de varios cuidados: de la actividad más reciente a la más antigua.
 * Las actividades anuladas (RF19) ya no forman parte del historial.
 */
export async function listarActividades(cuidadoIds: number[]): Promise<ActividadRespuesta[]> {
  if (cuidadoIds.length === 0) return [];
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT id, cuidado_id AS cuidadoId, fecha_planificada AS fechaPlanificada,
            fecha_realizacion AS fechaRealizacion, estado, nota
     FROM actividad
     WHERE cuidado_id IN (?) AND estado = 'completada'
     ORDER BY fecha_realizacion DESC, id DESC`,
    [cuidadoIds],
  );
  return filas as ActividadRespuesta[];
}

/** Lo que cambia en el cuidado cuando se completa una actividad o se anula. */
export interface CambiosCuidado {
  fechaRecurrente: string;
  totalRepeticiones: number | null;
  estado: 'pendiente' | 'finalizado';
}

/** RF16: guarda la actividad y actualiza el cuidado en una sola transacción. */
export async function registrarActividad(
  cuidadoId: number,
  actividad: { fechaPlanificada: string; fechaRealizacion: string; nota: string | null },
  cambios: CambiosCuidado,
): Promise<number> {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();
    const [resultado] = await conexion.query<ResultSetHeader>(
      `INSERT INTO actividad (cuidado_id, fecha_planificada, fecha_realizacion, estado, nota)
       VALUES (?, ?, ?, 'completada', ?)`,
      [cuidadoId, actividad.fechaPlanificada, actividad.fechaRealizacion, actividad.nota],
    );
    await conexion.query(
      'UPDATE cuidado SET fecha_recurrente = ?, total_repeticiones = ?, estado = ? WHERE id = ?',
      [cambios.fechaRecurrente, cambios.totalRepeticiones, cambios.estado, cuidadoId],
    );
    await conexion.commit();
    return resultado.insertId;
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

export interface ActividadPropia extends ActividadRespuesta {
  mascotaId: number;
}

/** Una actividad, solo si su cuidado pertenece a una mascota del usuario. */
export async function buscarActividadPropia(id: number, usuarioId: number): Promise<ActividadPropia | null> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT a.id, a.cuidado_id AS cuidadoId, a.fecha_planificada AS fechaPlanificada,
            a.fecha_realizacion AS fechaRealizacion, a.estado, a.nota, c.mascota_id AS mascotaId
     FROM actividad a
     JOIN cuidado c ON c.id = a.cuidado_id
     JOIN mascota m ON m.id = c.mascota_id
     WHERE a.id = ? AND m.usuario_id = ?`,
    [id, usuarioId],
  );
  return (filas[0] as ActividadPropia) ?? null;
}

/** Id de la última actividad completada del cuidado (la registrada más recientemente). */
export async function ultimaActividadCompletada(cuidadoId: number): Promise<number | null> {
  const [filas] = await pool.query<RowDataPacket[]>(
    "SELECT MAX(id) AS id FROM actividad WHERE cuidado_id = ? AND estado = 'completada'",
    [cuidadoId],
  );
  return filas[0]?.id ?? null;
}

/** RF18: agrega, modifica (texto) o elimina (null) la nota de una actividad. */
export async function actualizarNota(id: number, nota: string | null): Promise<void> {
  await pool.query('UPDATE actividad SET nota = ? WHERE id = ?', [nota, id]);
}

/** RF19: marca la actividad como anulada y, si corresponde, revierte el cuidado a su estado anterior. */
export async function anularActividad(id: number, cuidadoId: number, revertir: CambiosCuidado | null): Promise<void> {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();
    await conexion.query("UPDATE actividad SET estado = 'anulada' WHERE id = ?", [id]);
    if (revertir) {
      await conexion.query(
        'UPDATE cuidado SET fecha_recurrente = ?, total_repeticiones = ?, estado = ? WHERE id = ?',
        [revertir.fechaRecurrente, revertir.totalRepeticiones, revertir.estado, cuidadoId],
      );
    }
    await conexion.commit();
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
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
