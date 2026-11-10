import { ResultSetHeader, RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { promedioRedondeado } from './resena.logica';
import { DatosResena, ListadoResenas, OrdenResenas, ResenaVista } from './resena.types';

/** Capa de acceso a datos del módulo reseña. */

const ORDEN_SQL: Record<OrdenResenas, string> = {
  recientes: 'r.fecha DESC, r.id DESC',
  calificadas: 'r.calificacion DESC, r.fecha DESC, r.id DESC',
};

export async function existeOrganizador(id: number): Promise<boolean> {
  const [filas] = await pool.query<RowDataPacket[]>('SELECT 1 FROM organizador WHERE id = ?', [id]);
  return filas.length > 0;
}

/** Las reseñas de un organizador en el orden pedido, con su promedio y total. [limite] recorta la lista, no el resumen. */
export async function listar(
  organizadorId: number,
  usuarioId: number,
  orden: OrdenResenas,
  limite?: number,
): Promise<ListadoResenas> {
  const [resumen] = await pool.query<RowDataPacket[]>(
    'SELECT COUNT(*) AS total, COALESCE(SUM(calificacion), 0) AS suma FROM resena WHERE organizador_id = ?',
    [organizadorId],
  );
  const total = Number(resumen[0].total);

  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT r.id, u.nombre AS autor, r.calificacion, r.comentario,
            DATE_FORMAT(r.fecha, '%Y-%m-%d') AS fecha, (r.usuario_id = ?) AS esMia
     FROM resena r
     JOIN usuario u ON u.id = r.usuario_id
     WHERE r.organizador_id = ?
     ORDER BY ${ORDEN_SQL[orden]}
     ${limite ? 'LIMIT ?' : ''}`,
    limite ? [usuarioId, organizadorId, limite] : [usuarioId, organizadorId],
  );
  const resenas: ResenaVista[] = filas.map((f) => ({
    id: f.id,
    autor: f.autor,
    calificacion: f.calificacion,
    comentario: f.comentario,
    fecha: f.fecha,
    esMia: Boolean(f.esMia),
  }));
  return { promedio: promedioRedondeado(Number(resumen[0].suma), total), total, resenas };
}

export async function crear(usuarioId: number, organizadorId: number, datos: DatosResena): Promise<number> {
  const [resultado] = await pool.query<ResultSetHeader>(
    'INSERT INTO resena (usuario_id, organizador_id, calificacion, comentario) VALUES (?, ?, ?, ?)',
    [usuarioId, organizadorId, datos.calificacion, datos.comentario],
  );
  return resultado.insertId;
}

/** Devuelve true si la reseña existe y es del usuario. */
export async function esDelUsuario(id: number, usuarioId: number): Promise<boolean> {
  const [filas] = await pool.query<RowDataPacket[]>('SELECT 1 FROM resena WHERE id = ? AND usuario_id = ?', [id, usuarioId]);
  return filas.length > 0;
}

export async function actualizar(id: number, usuarioId: number, datos: DatosResena): Promise<void> {
  await pool.query('UPDATE resena SET calificacion = ?, comentario = ? WHERE id = ? AND usuario_id = ?', [
    datos.calificacion,
    datos.comentario,
    id,
    usuarioId,
  ]);
}

export async function eliminar(id: number, usuarioId: number): Promise<void> {
  await pool.query('DELETE FROM resena WHERE id = ? AND usuario_id = ?', [id, usuarioId]);
}

/** La reseña recién creada o editada, tal como la ve su autor. */
export async function buscar(id: number, usuarioId: number): Promise<ResenaVista | null> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `SELECT r.id, u.nombre AS autor, r.calificacion, r.comentario,
            DATE_FORMAT(r.fecha, '%Y-%m-%d') AS fecha, (r.usuario_id = ?) AS esMia
     FROM resena r
     JOIN usuario u ON u.id = r.usuario_id
     WHERE r.id = ?`,
    [usuarioId, id],
  );
  const f = filas[0];
  if (!f) return null;
  return { id: f.id, autor: f.autor, calificacion: f.calificacion, comentario: f.comentario, fecha: f.fecha, esMia: Boolean(f.esMia) };
}
