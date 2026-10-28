import { Request, Response } from 'express';
import { idDeLaRuta, idDelCuidado } from '../../common/ruta';
import * as servicio from './cuidado.service';

/** Capa HTTP del módulo cuidado. */

export async function listar(req: Request, res: Response) {
  res.json(await servicio.listar(idDeLaRuta(req), res.locals.usuarioId, req.query.estado));
}

export async function listarCronograma(req: Request, res: Response) {
  res.json(await servicio.listarCronograma(res.locals.usuarioId, req.query.mascotaId, req.query.estado));
}

export async function activarRecomendados(req: Request, res: Response) {
  const respuesta = await servicio.activarRecomendados(idDeLaRuta(req), res.locals.usuarioId, req.body ?? {});
  res.status(201).json(respuesta);
}

export async function crearPersonalizado(req: Request, res: Response) {
  const respuesta = await servicio.crearPersonalizado(idDeLaRuta(req), res.locals.usuarioId, req.body ?? {});
  res.status(201).json(respuesta);
}

export async function obtener(req: Request, res: Response) {
  res.json(await servicio.obtener(idDelCuidado(req), res.locals.usuarioId));
}

export async function editar(req: Request, res: Response) {
  res.json(await servicio.editar(idDelCuidado(req), res.locals.usuarioId, req.body ?? {}));
}

export async function eliminar(req: Request, res: Response) {
  await servicio.eliminar(idDelCuidado(req), res.locals.usuarioId);
  res.status(204).send();
}
