import { HttpError } from '../../common/errors/http-error';
import { esFechaReal, fechaDeHoy } from '../../common/fechas';
import { textoOpcional } from '../../common/texto';
import * as fotos from './mascota.fotos';
import * as repositorio from './mascota.repository';
import { DatosMascota, EstadoMascota, MascotaRegistro, MascotaRespuesta } from './mascota.types';

/** Capa de lógica de negocio del módulo mascota. Los mensajes siguen el ERS (RF05 y RF06). */

const ESPECIES = ['perro', 'gato', 'conejo'];
const SEXOS = ['macho', 'hembra'];

const CAMPO_OBLIGATORIO = 'Este campo es obligatorio';

/** Edad calculada a partir de la fecha de nacimiento (dato derivado: no se guarda). */
export function calcularEdadTexto(fechaNacimiento: string | null): string | null {
  if (!fechaNacimiento) return null;

  const [a1, m1, d1] = fechaNacimiento.split('-').map(Number);
  const hoy = new Date();
  let meses = (hoy.getFullYear() - a1) * 12 + (hoy.getMonth() + 1 - m1);
  if (hoy.getDate() < d1) meses -= 1;

  if (meses < 1) return 'Menos de 1 mes';
  if (meses < 12) return `${meses} ${meses === 1 ? 'mes' : 'meses'}`;
  const anios = Math.floor(meses / 12);
  return `${anios} ${anios === 1 ? 'año' : 'años'}`;
}

function validarDatos(entrada: Record<string, unknown>): DatosMascota {
  // Obligatorios: nombre y sexo
  const nombre = String(entrada.nombre ?? '').trim();
  const sexo = String(entrada.sexo ?? '').trim().toLowerCase();
  if (nombre === '' || sexo === '') throw new HttpError(400, CAMPO_OBLIGATORIO);
  if (nombre.length > 50) throw new HttpError(400, 'El nombre debe tener entre 1 y 50 caracteres');
  if (!SEXOS.includes(sexo)) throw new HttpError(400, 'El sexo debe ser macho o hembra');

  // Opcionales: raza, fecha de nacimiento y peso (se validan solo si vienen)
  const raza = textoOpcional(entrada.raza);
  if (raza !== null && raza.length > 80) throw new HttpError(400, 'La raza debe tener hasta 80 caracteres');

  const fechaNacimiento = textoOpcional(entrada.fechaNacimiento);
  if (fechaNacimiento !== null) {
    if (!esFechaReal(fechaNacimiento)) {
      throw new HttpError(400, 'La fecha de nacimiento no es válida (formato AAAA-MM-DD)');
    }
    if (fechaNacimiento > fechaDeHoy()) {
      throw new HttpError(400, 'La fecha de nacimiento no puede ser futura');
    }
  }

  let peso: number | null = null;
  if (textoOpcional(entrada.peso) !== null) {
    peso = Number(entrada.peso);
    if (!Number.isFinite(peso) || peso <= 0 || peso > 200) {
      throw new HttpError(400, 'Ingrese un peso válido');
    }
    peso = Math.round(peso * 100) / 100;
  }

  return { nombre, raza, fechaNacimiento, sexo: sexo as DatosMascota['sexo'], peso };
}

function aRespuesta(mascota: MascotaRegistro): MascotaRespuesta {
  return { ...mascota, edadTexto: calcularEdadTexto(mascota.fechaNacimiento) };
}

async function obtenerPropia(id: number, usuarioId: number): Promise<MascotaRegistro> {
  const mascota = await repositorio.buscarPorId(id, usuarioId);
  // Si no existe o es de otro usuario, la respuesta es la misma: no se revela que existe.
  if (!mascota) throw new HttpError(404, 'Mascota no encontrada');
  return mascota;
}

export async function listar(usuarioId: number): Promise<MascotaRespuesta[]> {
  return (await repositorio.listarPorUsuario(usuarioId)).map(aRespuesta);
}

export async function obtener(id: number, usuarioId: number): Promise<MascotaRespuesta> {
  return aRespuesta(await obtenerPropia(id, usuarioId));
}

export async function crear(usuarioId: number, entrada: Record<string, unknown>): Promise<MascotaRespuesta> {
  const especie = String(entrada.especie ?? '').trim().toLowerCase();
  if (especie === '') throw new HttpError(400, CAMPO_OBLIGATORIO);
  if (!ESPECIES.includes(especie)) {
    throw new HttpError(400, 'La especie debe ser perro, gato o conejo');
  }
  const datos = validarDatos(entrada);

  const especieId = await repositorio.buscarIdEspecie(especie);
  if (!especieId) throw new HttpError(500, 'La especie no está cargada en la base de datos');

  const id = await repositorio.crear(usuarioId, especieId, datos);
  return obtener(id, usuarioId);
}

/** La especie no se puede cambiar (RF06): de ella dependen los cuidados recomendados. */
export async function editar(
  id: number,
  usuarioId: number,
  entrada: Record<string, unknown>,
): Promise<MascotaRespuesta> {
  await obtenerPropia(id, usuarioId);
  await repositorio.actualizar(id, usuarioId, validarDatos(entrada));
  return obtener(id, usuarioId);
}

export async function cambiarEstado(
  id: number,
  usuarioId: number,
  estado: EstadoMascota,
): Promise<MascotaRespuesta> {
  await obtenerPropia(id, usuarioId);
  await repositorio.cambiarEstado(id, usuarioId, estado);
  return obtener(id, usuarioId);
}

export async function eliminar(id: number, usuarioId: number): Promise<void> {
  const mascota = await obtenerPropia(id, usuarioId);
  await repositorio.eliminar(id, usuarioId);
  await fotos.borrarArchivo(mascota.foto); // también se borra su foto
}

/** Guarda la foto de la mascota (reemplaza la anterior si había una). */
export async function guardarFoto(
  id: number,
  usuarioId: number,
  archivo: Express.Multer.File | undefined,
): Promise<MascotaRespuesta> {
  const mascota = await obtenerPropia(id, usuarioId);
  if (!archivo) throw new HttpError(400, 'Seleccione una imagen');

  const formato = fotos.detectarFormato(archivo.buffer);
  if (!formato) throw new HttpError(400, 'La imagen debe ser JPG, PNG o WebP');

  const ruta = await fotos.guardarArchivo(archivo.buffer, formato);
  await repositorio.actualizarFoto(id, usuarioId, ruta);
  await fotos.borrarArchivo(mascota.foto);
  return obtener(id, usuarioId);
}

export async function quitarFoto(id: number, usuarioId: number): Promise<MascotaRespuesta> {
  const mascota = await obtenerPropia(id, usuarioId);
  await repositorio.actualizarFoto(id, usuarioId, null);
  await fotos.borrarArchivo(mascota.foto);
  return obtener(id, usuarioId);
}
