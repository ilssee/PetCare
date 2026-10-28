import { Request } from 'express';
import { HttpError } from './errors/http-error';

/** Lee el id de la mascota que viene en la ruta (/mascotas/:id/...). */
export function idDeLaRuta(req: Request): number {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) throw new HttpError(400, 'El id de la mascota no es válido');
  return id;
}
