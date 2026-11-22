import { ResultSetHeader, RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { DatosMascota, EstadoMascota, MascotaRegistro } from './mascota.types';

/** Capa de acceso a datos del módulo mascota. Todas las consultas filtran por usuario_id. */

const SELECT_MASCOTA = `
  SELECT m.id, m.nombre, e.nombre AS especie, m.raza, m.fecha_nacimiento AS fechaNacimiento,
         m.sexo, m.peso, m.foto, m.estado,
         (SELECT COUNT(*) FROM cuidado c
           WHERE c.mascota_id = m.id AND c.estado <> 'eliminado') AS totalCuidados
  FROM mascota m
  JOIN especie e ON e.id = m.especie_id`;

export async function listarPorUsuario(usuarioId: number): Promise<MascotaRegistro[]> {
  // Las activas primero (el orden de un ENUM sigue el orden en que se definió).
  const [filas] = await pool.query<RowDataPacket[]>(
    `${SELECT_MASCOTA} WHERE m.usuario_id = ? ORDER BY m.estado, m.nombre`,
    [usuarioId],
  );
  return filas as MascotaRegistro[];
}

export async function buscarPorId(id: number, usuarioId: number): Promise<MascotaRegistro | null> {
  const [filas] = await pool.query<RowDataPacket[]>(
    `${SELECT_MASCOTA} WHERE m.id = ? AND m.usuario_id = ?`,
    [id, usuarioId],
  );
  return (filas[0] as MascotaRegistro) ?? null;
}

export async function buscarIdEspecie(nombre: string): Promise<number | null> {
  const [filas] = await pool.query<RowDataPacket[]>('SELECT id FROM especie WHERE nombre = ?', [nombre]);
  return filas[0] ? (filas[0].id as number) : null;
}

export async function crear(usuarioId: number, especieId: number, datos: DatosMascota): Promise<number> {
  const [resultado] = await pool.query<ResultSetHeader>(
    `INSERT INTO mascota (usuario_id, especie_id, nombre, raza, fecha_nacimiento, sexo, peso)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [usuarioId, especieId, datos.nombre, datos.raza, datos.fechaNacimiento, datos.sexo, datos.peso],
  );
  return resultado.insertId;
}

/** Devuelve true si la mascota existía y pertenecía al usuario. */
export async function actualizar(id: number, usuarioId: number, datos: DatosMascota): Promise<boolean> {
  const [resultado] = await pool.query<ResultSetHeader>(
    `UPDATE mascota SET nombre = ?, raza = ?, fecha_nacimiento = ?, sexo = ?, peso = ?
     WHERE id = ? AND usuario_id = ?`,
    [datos.nombre, datos.raza, datos.fechaNacimiento, datos.sexo, datos.peso, id, usuarioId],
  );
  return resultado.affectedRows > 0;
}

export async function actualizarFoto(id: number, usuarioId: number, ruta: string | null): Promise<boolean> {
  const [resultado] = await pool.query<ResultSetHeader>(
    'UPDATE mascota SET foto = ? WHERE id = ? AND usuario_id = ?',
    [ruta, id, usuarioId],
  );
  return resultado.affectedRows > 0;
}

export async function cambiarEstado(id: number, usuarioId: number, estado: EstadoMascota): Promise<boolean> {
  const [resultado] = await pool.query<ResultSetHeader>(
    'UPDATE mascota SET estado = ? WHERE id = ? AND usuario_id = ?',
    [estado, id, usuarioId],
  );
  return resultado.affectedRows > 0;
}

export async function eliminar(id: number, usuarioId: number): Promise<boolean> {
  const [resultado] = await pool.query<ResultSetHeader>(
    'DELETE FROM mascota WHERE id = ? AND usuario_id = ?',
    [id, usuarioId],
  );
  return resultado.affectedRows > 0;
}
