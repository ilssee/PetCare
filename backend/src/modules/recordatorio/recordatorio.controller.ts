import { Request, Response } from 'express';
import * as servicio from './recordatorio.service';

/** Capa HTTP del módulo recordatorio. */

export async function obtener(_req: Request, res: Response) {
  res.json(await servicio.obtenerConfiguracion(res.locals.usuarioId));
}

export async function guardar(req: Request, res: Response) {
  res.json(await servicio.guardarConfiguracion(res.locals.usuarioId, req.body ?? {}));
}
