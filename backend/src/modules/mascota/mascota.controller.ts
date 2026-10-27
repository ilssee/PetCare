import { Request, Response } from 'express';
import { HttpError } from '../../common/errors/http-error';
import * as servicio from './mascota.service';

/** Capa HTTP del módulo mascota. El id del usuario lo deja la verificación de sesión. */

function idDeLaRuta(req: Request): number {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) throw new HttpError(400, 'El id de la mascota no es válido');
  return id;
}

export async function listar(_req: Request, res: Response) {
  res.json(await servicio.listar(res.locals.usuarioId));
}

export async function obtener(req: Request, res: Response) {
  res.json(await servicio.obtener(idDeLaRuta(req), res.locals.usuarioId));
}

export async function crear(req: Request, res: Response) {
  const mascota = await servicio.crear(res.locals.usuarioId, req.body ?? {});
  res.status(201).json(mascota);
}

export async function editar(req: Request, res: Response) {
  res.json(await servicio.editar(idDeLaRuta(req), res.locals.usuarioId, req.body ?? {}));
}

export async function inactivar(req: Request, res: Response) {
  res.json(await servicio.cambiarEstado(idDeLaRuta(req), res.locals.usuarioId, 'inactiva'));
}

export async function reactivar(req: Request, res: Response) {
  res.json(await servicio.cambiarEstado(idDeLaRuta(req), res.locals.usuarioId, 'activa'));
}

export async function subirFoto(req: Request, res: Response) {
  res.json(await servicio.guardarFoto(idDeLaRuta(req), res.locals.usuarioId, req.file));
}

export async function quitarFoto(req: Request, res: Response) {
  res.json(await servicio.quitarFoto(idDeLaRuta(req), res.locals.usuarioId));
}

export async function eliminar(req: Request, res: Response) {
  await servicio.eliminar(idDeLaRuta(req), res.locals.usuarioId);
  res.status(204).send();
}
