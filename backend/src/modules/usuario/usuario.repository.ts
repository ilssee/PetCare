import { ResultSetHeader, RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { UsuarioPublico, UsuarioRegistro } from './usuario.types';

/** Capa de acceso a datos: aquí vive todo el SQL del módulo usuario. */

export async function buscarPorCorreo(correo: string): Promise<UsuarioRegistro | null> {
  const [filas] = await pool.query<RowDataPacket[]>(
    'SELECT id, nombre, correo, contrasena FROM usuario WHERE correo = ?',
    [correo],
  );
  return (filas[0] as UsuarioRegistro) ?? null;
}

export async function buscarPorId(id: number): Promise<UsuarioPublico | null> {
  const [filas] = await pool.query<RowDataPacket[]>(
    'SELECT id, nombre, correo FROM usuario WHERE id = ?',
    [id],
  );
  return (filas[0] as UsuarioPublico) ?? null;
}

/**
 * Crea el usuario y sus preferencias iniciales (relación 1 a 1) en una sola transacción:
 * o se guardan las dos filas, o ninguna.
 */
export async function crear(nombre: string, correo: string, hash: string): Promise<UsuarioPublico> {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();

    const [resultado] = await conexion.query<ResultSetHeader>(
      'INSERT INTO usuario (nombre, correo, contrasena) VALUES (?, ?, ?)',
      [nombre, correo, hash],
    );
    const id = resultado.insertId;

    // Preferencias sin configurar: los avisos de cercanía empiezan desactivados.
    await conexion.query(
      `INSERT INTO preferencias (usuario_id, notificar_nuevas_campanas, notificar_nuevas_veterinarias)
       VALUES (?, FALSE, FALSE)`,
      [id],
    );

    await conexion.commit();
    return { id, nombre, correo };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}
