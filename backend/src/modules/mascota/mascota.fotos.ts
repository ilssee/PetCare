import { randomUUID } from 'crypto';
import fs from 'fs/promises';
import path from 'path';
import { carpetaFotosMascotas } from '../../config/uploads';

/** Manejo de los archivos de foto de las mascotas (la base de datos solo guarda la ruta). */

export type FormatoImagen = 'jpg' | 'png' | 'webp';

/** Detecta el formato real mirando los primeros bytes; no se confía en lo que diga el cliente. */
export function detectarFormato(datos: Buffer): FormatoImagen | null {
  if (datos.length > 12 && datos[0] === 0xff && datos[1] === 0xd8 && datos[2] === 0xff) return 'jpg';
  if (datos.length > 12 && datos.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) {
    return 'png';
  }
  if (datos.length > 12 && datos.subarray(0, 4).toString('ascii') === 'RIFF' && datos.subarray(8, 12).toString('ascii') === 'WEBP') {
    return 'webp';
  }
  return null;
}

/** Guarda la imagen con un nombre aleatorio (imposible de adivinar) y devuelve su ruta, ej. "mascotas/9f3a...jpg". */
export async function guardarArchivo(datos: Buffer, formato: FormatoImagen): Promise<string> {
  const nombre = `${randomUUID()}.${formato}`;
  await fs.writeFile(path.join(carpetaFotosMascotas, nombre), datos);
  return `mascotas/${nombre}`;
}

/** Borra un archivo de foto. Solo toca la carpeta de fotos, sea cual sea la ruta recibida. */
export async function borrarArchivo(ruta: string | null): Promise<void> {
  if (!ruta) return;
  await fs.rm(path.join(carpetaFotosMascotas, path.basename(ruta)), { force: true });
}
