import { Request, Response } from 'express';
import * as servicio from './mapa.service';

/** Capa HTTP del módulo mapa. */

export async function veterinarias(req: Request, res: Response) {
  res.json(await servicio.veterinarias(req.query));
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
