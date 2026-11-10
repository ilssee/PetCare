import { HttpError } from '../../common/errors/http-error';
import * as repositorio from './dispositivo.repository';

/** Capa de lógica de negocio del módulo dispositivo. */

const MAXIMO_TOKEN = 512;

function validarToken(entrada: Record<string, unknown>): string {
  const token = String(entrada.token ?? '').trim();
  if (token === '') throw new HttpError(400, 'Este campo es obligatorio');
  if (token.length > MAXIMO_TOKEN) throw new HttpError(400, 'El token del dispositivo no es válido');
  return token;
}

/** La app envía el token de su celular al iniciar sesión (y cada vez que Firebase lo renueva). */
export async function registrar(usuarioId: number, entrada: Record<string, unknown>): Promise<void> {
  await repositorio.registrar(usuarioId, validarToken(entrada));
}

/** Al cerrar sesión, el celular deja de recibir los avisos de ese usuario. */
export async function darDeBaja(usuarioId: number, entrada: Record<string, unknown>): Promise<void> {
  await repositorio.quitarDeUsuario(usuarioId, validarToken(entrada));
}
