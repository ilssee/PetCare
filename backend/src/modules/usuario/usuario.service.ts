import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { env } from '../../config/env';
import { HttpError } from '../../common/errors/http-error';
import { huellaDeContrasena } from '../../common/sesion';
import * as dispositivos from '../dispositivo/dispositivo.repository';
import * as repositorio from './usuario.repository';
import { DatosLogin, DatosRegistro, UsuarioPublico } from './usuario.types';

/** Capa de lógica de negocio: valida, cifra, compara y genera el token. */

const COSTO_CIFRADO = 10;
const FORMATO_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validarRegistro(datos: Partial<DatosRegistro>): DatosRegistro {
  const nombre = (datos.nombre ?? '').trim();
  const correo = (datos.correo ?? '').trim().toLowerCase();
  const contrasena = datos.contrasena ?? '';

  // RF01: si algún campo está vacío, el mensaje es el mismo para todos.
  if (nombre === '' || correo === '' || contrasena === '') {
    throw new HttpError(400, 'Todos los campos son obligatorios');
  }
  if (nombre.length < 2 || nombre.length > 100) {
    throw new HttpError(400, 'El nombre debe tener entre 2 y 100 caracteres');
  }
  if (!FORMATO_CORREO.test(correo) || correo.length > 255) {
    throw new HttpError(400, 'El correo no tiene un formato válido');
  }
  if (contrasena.length < 8 || contrasena.length > 64) {
    throw new HttpError(400, 'La contraseña debe tener entre 8 y 64 caracteres');
  }
  return { nombre, correo, contrasena };
}

export async function registrar(datos: Partial<DatosRegistro>): Promise<UsuarioPublico> {
  const { nombre, correo, contrasena } = validarRegistro(datos);

  if (await repositorio.buscarPorCorreo(correo)) {
    throw new HttpError(409, 'El correo ya está en uso. Intente con otro');
  }

  const hash = await bcrypt.hash(contrasena, COSTO_CIFRADO);
  return repositorio.crear(nombre, correo, hash);
}

export async function iniciarSesion(datos: Partial<DatosLogin>) {
  const correo = (datos.correo ?? '').trim().toLowerCase();
  const contrasena = datos.contrasena ?? '';

  // RF02: si algún campo está vacío, el mensaje es el mismo para ambos.
  if (correo === '' || contrasena === '') {
    throw new HttpError(400, 'Todos los campos son obligatorios');
  }

  const usuario = await repositorio.buscarPorCorreo(correo);
  // Mismo mensaje si falla el correo o la contraseña: no revela cuál de los dos es.
  if (!usuario || !(await bcrypt.compare(contrasena, usuario.contrasena))) {
    throw new HttpError(401, 'Correo o contraseña incorrectos');
  }

  // Sin vencimiento (ERS: la sesión dura hasta cerrar sesión o cambiar la contraseña).
  const token = jwt.sign({ id: usuario.id, h: huellaDeContrasena(usuario.contrasena) }, env.jwt.secreto);
  return {
    token,
    usuario: { id: usuario.id, nombre: usuario.nombre, correo: usuario.correo },
  };
}

export async function obtenerPerfil(id: number): Promise<UsuarioPublico> {
  const usuario = await repositorio.buscarPorId(id);
  if (!usuario) {
    throw new HttpError(404, 'Usuario no encontrado');
  }
  return usuario;
}

const CAMPO_OBLIGATORIO = 'Este campo es obligatorio';

/** RF04: el usuario edita su nombre y su correo. */
export async function actualizarPerfil(id: number, datos: Partial<{ nombre: string; correo: string }>): Promise<UsuarioPublico> {
  const nombre = (datos.nombre ?? '').trim();
  const correo = (datos.correo ?? '').trim().toLowerCase();

  if (nombre === '' || correo === '') throw new HttpError(400, CAMPO_OBLIGATORIO);
  if (nombre.length < 2 || nombre.length > 100) {
    throw new HttpError(400, 'El nombre debe tener entre 2 y 100 caracteres');
  }
  if (!FORMATO_CORREO.test(correo) || correo.length > 255) {
    throw new HttpError(400, 'El correo no tiene un formato válido');
  }

  const existente = await repositorio.buscarPorCorreo(correo);
  if (existente && existente.id !== id) {
    throw new HttpError(409, 'El correo ya está en uso. Intente con otro');
  }

  try {
    await repositorio.actualizarDatos(id, nombre, correo);
  } catch (error) {
    // Por si otro usuario tomó el correo justo en este instante.
    if ((error as { code?: string }).code === 'ER_DUP_ENTRY') {
      throw new HttpError(409, 'El correo ya está en uso. Intente con otro');
    }
    throw error;
  }
  return obtenerPerfil(id);
}

/** RF04: cambio de contraseña. Se pide la actual para confirmar que es el dueño de la cuenta. */
export async function cambiarContrasena(
  id: number,
  datos: Partial<{ contrasenaActual: string; contrasenaNueva: string }>,
): Promise<void> {
  const actual = datos.contrasenaActual ?? '';
  const nueva = datos.contrasenaNueva ?? '';

  if (actual === '' || nueva === '') throw new HttpError(400, CAMPO_OBLIGATORIO);
  if (nueva.length < 8 || nueva.length > 64) {
    throw new HttpError(400, 'La contraseña debe tener entre 8 y 64 caracteres');
  }

  const hashGuardado = await repositorio.obtenerHash(id);
  if (!hashGuardado || !(await bcrypt.compare(actual, hashGuardado))) {
    throw new HttpError(400, 'La contraseña actual es incorrecta');
  }
  if (actual === nueva) {
    throw new HttpError(400, 'La nueva contraseña debe ser distinta de la actual');
  }

  // Con la contraseña nueva las sesiones anteriores dejan de ser válidas (ver common/sesion.ts), así que
  // también se retiran los celulares para que no sigan recibiendo notificaciones sin sesión.
  await repositorio.cambiarContrasena(id, await bcrypt.hash(nueva, COSTO_CIFRADO));
  await dispositivos.quitarTodosDeUsuario(id);
}
