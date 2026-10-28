import { Request, Response } from 'express';
import { idDeLaRuta } from '../../common/ruta';
import * as servicio from './regla.service';

/** Capa HTTP del módulo regla. */

export async function listarDisponibles(req: Request, res: Response) {
  res.json(await servicio.listarDisponibles(idDeLaRuta(req), res.locals.usuarioId));
}
