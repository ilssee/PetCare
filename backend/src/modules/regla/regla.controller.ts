import { Request, Response } from 'express';
import { idDeLaRuta } from '../../common/ruta';
import * as servicio from './regla.service';

/** Capa HTTP del módulo regla. */

/**
 * Sin parámetros devuelve solo las recomendaciones que faltan por agregar (propuesta de cronograma).
 * Con ?incluirAgregadas=true devuelve todas, marcando las que ya están en el cronograma (información preventiva).
 */
export async function listarDisponibles(req: Request, res: Response) {
  const id = idDeLaRuta(req);
  if (req.query.incluirAgregadas === 'true') {
    res.json(await servicio.listarConEstado(id, res.locals.usuarioId));
    return;
  }
  res.json(await servicio.listarDisponibles(id, res.locals.usuarioId));
}
