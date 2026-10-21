import { NextFunction, Request, Response } from 'express';
import { HttpError } from '../errors/http-error';

/** Último middleware: convierte cualquier error en una respuesta JSON ordenada. */
export function manejarErrores(error: unknown, _req: Request, res: Response, _next: NextFunction) {
  if (error instanceof HttpError) {
    res.status(error.estado).json({ error: error.message });
    return;
  }
  console.error('Error inesperado:', error);
  res.status(500).json({ error: 'Error interno del servidor' });
}
