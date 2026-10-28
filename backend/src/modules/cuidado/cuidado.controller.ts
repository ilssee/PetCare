import { Request, Response } from 'express';
import { idDeLaRuta } from '../../common/ruta';
import * as servicio from './cuidado.service';

/** Capa HTTP del módulo cuidado. */

export async function activarRecomendados(req: Request, res: Response) {
  const respuesta = await servicio.activarRecomendados(idDeLaRuta(req), res.locals.usuarioId, req.body ?? {});
  res.status(201).json(respuesta);
}
