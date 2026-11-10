import { Request, Response } from 'express';
import * as servicio from './cercania.service';

/** Capa HTTP del módulo cercanía. */

export async function obtener(_req: Request, res: Response) {
  res.json(await servicio.obtener(res.locals.usuarioId));
}

export async function guardar(req: Request, res: Response) {
  res.json(await servicio.guardar(res.locals.usuarioId, req.body ?? {}));
}
