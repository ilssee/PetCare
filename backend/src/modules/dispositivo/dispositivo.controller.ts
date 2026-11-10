import { Request, Response } from 'express';
import * as servicio from './dispositivo.service';

/** Capa HTTP del módulo dispositivo. */

export async function registrar(req: Request, res: Response) {
  await servicio.registrar(res.locals.usuarioId, req.body ?? {});
  res.status(204).send();
}

export async function darDeBaja(req: Request, res: Response) {
  await servicio.darDeBaja(res.locals.usuarioId, req.body ?? {});
  res.status(204).send();
}
