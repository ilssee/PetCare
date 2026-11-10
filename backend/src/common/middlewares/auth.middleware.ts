import { NextFunction, Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { env } from '../../config/env';
import { obtenerHash } from '../../modules/usuario/usuario.repository';
import { HttpError } from '../errors/http-error';
import { huellaDeContrasena } from '../sesion';

/**
 * Protege una ruta: exige un token válido en la cabecera "Authorization: Bearer <token>".
 * La sesión no vence con el tiempo: dura hasta que el usuario cierra sesión o cambia su contraseña.
 * Por eso, además de la firma, se comprueba que la contraseña del usuario siga siendo la misma con la
 * que se emitió el token. Si es válido, deja el id del usuario en res.locals.usuarioId.
 */
export async function exigirSesion(req: Request, res: Response, next: NextFunction) {
  const cabecera = req.headers.authorization ?? '';
  const [tipo, token] = cabecera.split(' ');

  if (tipo !== 'Bearer' || !token) {
    throw new HttpError(401, 'Falta el token de sesión');
  }

  let datos: { id: number; h?: string };
  try {
    datos = jwt.verify(token, env.jwt.secreto) as { id: number; h?: string };
  } catch {
    throw new HttpError(401, 'Token inválido');
  }

  const hashGuardado = await obtenerHash(datos.id);
  if (!hashGuardado || !datos.h || datos.h !== huellaDeContrasena(hashGuardado)) {
    throw new HttpError(401, 'La sesión ya no es válida. Inicie sesión nuevamente');
  }

  res.locals.usuarioId = datos.id;
  next();
}
