import { NextFunction, Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { env } from '../../config/env';
import { HttpError } from '../errors/http-error';

/**
 * Protege una ruta: exige un token válido en la cabecera "Authorization: Bearer <token>".
 * Si es válido, deja el id del usuario en res.locals.usuarioId para que lo use el controlador.
 */
export function exigirSesion(req: Request, res: Response, next: NextFunction) {
  const cabecera = req.headers.authorization ?? '';
  const [tipo, token] = cabecera.split(' ');

  if (tipo !== 'Bearer' || !token) {
    throw new HttpError(401, 'Falta el token de sesión');
  }

  try {
    const datos = jwt.verify(token, env.jwt.secreto) as { id: number };
    res.locals.usuarioId = datos.id;
    next();
  } catch {
    throw new HttpError(401, 'Token inválido o vencido');
  }
}
