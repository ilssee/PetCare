import { createHash } from 'crypto';

/**
 * Huella de la contraseña cifrada de un usuario. Va dentro del token de sesión: si el usuario cambia
 * su contraseña, la huella cambia y los tokens anteriores dejan de servir (la sesión se cierra).
 */
export function huellaDeContrasena(hashGuardado: string): string {
  return createHash('sha256').update(hashGuardado).digest('hex').slice(0, 16);
}
