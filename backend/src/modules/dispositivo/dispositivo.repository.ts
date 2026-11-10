import { RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';

/** Capa de acceso a datos del módulo dispositivo: los celulares donde el usuario recibe notificaciones. */

/**
 * Guarda el token del celular. Si ese token ya existía (por ejemplo, otro usuario inició sesión
 * en el mismo celular), pasa a ser del usuario actual.
 */
export async function registrar(usuarioId: number, token: string): Promise<void> {
  await pool.query(
    'INSERT INTO dispositivo (usuario_id, token) VALUES (?, ?) ON DUPLICATE KEY UPDATE usuario_id = VALUES(usuario_id)',
    [usuarioId, token],
  );
}

/** Quita el token del usuario (al cerrar sesión). Solo borra si el token es suyo. */
export async function quitarDeUsuario(usuarioId: number, token: string): Promise<void> {
  await pool.query('DELETE FROM dispositivo WHERE usuario_id = ? AND token = ?', [usuarioId, token]);
}

/** Quita todos los celulares del usuario (al cambiar su contraseña se cierran sus sesiones). */
export async function quitarTodosDeUsuario(usuarioId: number): Promise<void> {
  await pool.query('DELETE FROM dispositivo WHERE usuario_id = ?', [usuarioId]);
}

export async function tokensDeUsuario(usuarioId: number): Promise<string[]> {
  const [filas] = await pool.query<RowDataPacket[]>('SELECT token FROM dispositivo WHERE usuario_id = ?', [usuarioId]);
  return filas.map((f) => f.token as string);
}

/** Borra tokens que Firebase ya no reconoce (la app se desinstaló o el token venció). */
export async function quitarTokens(tokens: string[]): Promise<void> {
  if (tokens.length === 0) return;
  await pool.query('DELETE FROM dispositivo WHERE token IN (?)', [tokens]);
}
