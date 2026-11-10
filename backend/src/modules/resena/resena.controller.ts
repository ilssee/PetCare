import { Request, Response } from 'express';
import { HttpError } from '../../common/errors/http-error';
import * as servicio from './resena.service';

/** Capa HTTP del módulo reseña. El id del usuario lo deja la verificación de sesión. */

function idDeLaRuta(req: Request): number {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) throw new HttpError(400, 'El id no es válido');
  return id;
}

export async function listar(req: Request, res: Response) {
  res.json(await servicio.listar(res.locals.usuarioId, idDeLaRuta(req), req.query.orden));
}

export async function crear(req: Request, res: Response) {
  const resena = await servicio.crear(res.locals.usuarioId, idDeLaRuta(req), req.body ?? {});
  res.status(201).json(resena);
}

export async function editar(req: Request, res: Response) {
  res.json(await servicio.editar(res.locals.usuarioId, idDeLaRuta(req), req.body ?? {}));
}

export async function eliminar(req: Request, res: Response) {
  await servicio.eliminar(res.locals.usuarioId, idDeLaRuta(req));
  res.status(204).send();
}
