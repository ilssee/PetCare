import { NextFunction, Request, Response } from 'express';
import multer from 'multer';
import { HttpError } from '../errors/http-error';

/** Último middleware: convierte cualquier error en una respuesta JSON ordenada. */
export function manejarErrores(error: unknown, _req: Request, res: Response, _next: NextFunction) {
  if (error instanceof HttpError) {
    res.status(error.estado).json({ error: error.message });
    return;
  }
  if (error instanceof multer.MulterError) {
    const mensaje = error.code === 'LIMIT_FILE_SIZE' ? 'La imagen no puede pesar más de 5 MB' : 'No se pudo procesar la imagen';
    res.status(400).json({ error: mensaje });
    return;
  }
  console.error('Error inesperado:', error);
  res.status(500).json({ error: 'Error interno del servidor' });
}
