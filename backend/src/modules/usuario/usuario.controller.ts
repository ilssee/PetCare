import { Request, Response } from 'express';
import * as servicio from './usuario.service';

/** Capa HTTP: recibe la petición, llama al servicio y responde. No contiene lógica de negocio. */

export async function registrar(req: Request, res: Response) {
  const usuario = await servicio.registrar(req.body ?? {});
  res.status(201).json(usuario);
}

export async function iniciarSesion(req: Request, res: Response) {
  const respuesta = await servicio.iniciarSesion(req.body ?? {});
  res.json(respuesta);
}

export async function obtenerPerfil(_req: Request, res: Response) {
  const usuario = await servicio.obtenerPerfil(res.locals.usuarioId);
  res.json(usuario);
}
