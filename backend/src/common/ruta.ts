import { Request } from 'express';
import { HttpError } from './errors/http-error';

function leerId(req: Request, mensaje: string): number {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) throw new HttpError(400, mensaje);
  return id;
}

/** Lee el id de la mascota que viene en la ruta (/mascotas/:id/...). */
export function idDeLaRuta(req: Request): number {
  return leerId(req, 'El id de la mascota no es válido');
}

/** Lee el id del cuidado que viene en la ruta (/cuidados/:id). */
export function idDelCuidado(req: Request): number {
  return leerId(req, 'El id del cuidado no es válido');
}
