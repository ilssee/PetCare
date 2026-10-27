import fs from 'fs';
import path from 'path';

/** Carpeta donde se guardan las fotos subidas. Se sirve en la ruta pública /uploads. */
export const carpetaUploads = path.resolve(__dirname, '../../uploads');
export const carpetaFotosMascotas = path.join(carpetaUploads, 'mascotas');

fs.mkdirSync(carpetaFotosMascotas, { recursive: true });

export const TAMANO_MAXIMO_FOTO = 5 * 1024 * 1024; // 5 MB
