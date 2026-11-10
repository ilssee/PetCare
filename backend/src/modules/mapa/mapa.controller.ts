import { Request, Response } from 'express';
import { HttpError } from '../../common/errors/http-error';
import * as servicio from './mapa.service';

/** Capa HTTP del módulo mapa. */

export async function veterinarias(req: Request, res: Response) {
  res.json(await servicio.veterinarias(req.query));
}

export async function perfilDeVeterinaria(req: Request, res: Response) {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) throw new HttpError(400, 'El id de la veterinaria no es válido');
  res.json(await servicio.perfil(id, res.locals.usuarioId, req.query));
}

export async function detalleDeCampana(req: Request, res: Response) {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) throw new HttpError(400, 'El id de la campaña no es válido');
  res.json(await servicio.detalleDeUnaCampana(id, req.query));
}

export async function campanas(req: Request, res: Response) {
  res.json(await servicio.campanas(req.query));
}

export async function buscar(req: Request, res: Response) {
  res.json(await servicio.buscar(req.query));
}

export async function filtros(_req: Request, res: Response) {
  res.json(await servicio.filtros());
}
