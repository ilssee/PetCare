import { HttpError } from '../../common/errors/http-error';
import { validarResena } from './resena.logica';
import * as repositorio from './resena.repository';
import { ListadoResenas, OrdenResenas, ResenaVista } from './resena.types';

/** Reglas del módulo reseña: RF44 (consultar), RF45 (escribir) y RF46 (editar y eliminar las propias). */

export const MENSAJE_NO_SE_PUDO = 'La operación no pudo realizarse';

const ORDENES: OrdenResenas[] = ['recientes', 'calificadas'];

async function exigirOrganizador(id: number) {
  if (!(await repositorio.existeOrganizador(id))) {
    throw new HttpError(404, 'No se encontró la veterinaria u organizador');
  }
}

export async function listar(usuarioId: number, organizadorId: number, orden: unknown): Promise<ListadoResenas> {
  const elegido = orden === undefined || orden === '' ? 'recientes' : (orden as OrdenResenas);
  if (!ORDENES.includes(elegido)) throw new HttpError(400, `El orden "${String(orden)}" no es válido para las reseñas`);
  await exigirOrganizador(organizadorId);
  return repositorio.listar(organizadorId, usuarioId, elegido);
}

export async function crear(usuarioId: number, organizadorId: number, datos: Record<string, unknown>): Promise<ResenaVista> {
  const validadas = validarResena(datos);
  await exigirOrganizador(organizadorId);
  const id = await repositorio.crear(usuarioId, organizadorId, validadas);
  const creada = await repositorio.buscar(id, usuarioId);
  if (!creada) throw new HttpError(500, MENSAJE_NO_SE_PUDO);
  return creada;
}

/**
 * RF46: solo el autor puede editar o eliminar su reseña. Si la reseña no existe o es de otro usuario,
 * se responde lo mismo, sin revelar de quién es, y no se cambia nada.
 */
export async function editar(usuarioId: number, id: number, datos: Record<string, unknown>): Promise<ResenaVista> {
  const validadas = validarResena(datos);
  if (!(await repositorio.esDelUsuario(id, usuarioId))) throw new HttpError(404, MENSAJE_NO_SE_PUDO);
  await repositorio.actualizar(id, usuarioId, validadas);
  const actualizada = await repositorio.buscar(id, usuarioId);
  if (!actualizada) throw new HttpError(404, MENSAJE_NO_SE_PUDO);
  return actualizada;
}

export async function eliminar(usuarioId: number, id: number): Promise<void> {
  if (!(await repositorio.esDelUsuario(id, usuarioId))) throw new HttpError(404, MENSAJE_NO_SE_PUDO);
  await repositorio.eliminar(id, usuarioId);
}
