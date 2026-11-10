import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { cert, getApps, initializeApp } from 'firebase-admin/app';
import { getMessaging, Messaging } from 'firebase-admin/messaging';
import { env } from './env';

/**
 * Conexión con Firebase Cloud Messaging, el servicio que entrega las notificaciones push al celular.
 * Usa la clave de cuenta de servicio que se descarga desde la consola de Firebase (archivo secreto,
 * fuera de GitHub). Devuelve null si el archivo no existe, para que el servidor pueda arrancar igual.
 */
let mensajeriaCreada: Messaging | null | undefined;

export function mensajeria(): Messaging | null {
  if (mensajeriaCreada !== undefined) return mensajeriaCreada;

  const ruta = resolve(env.firebase.clave);
  if (!existsSync(ruta)) {
    console.warn(`No se encontró la clave de Firebase en ${ruta}: los recordatorios no se enviarán al celular.`);
    mensajeriaCreada = null;
    return null;
  }

  if (getApps().length === 0) {
    initializeApp({ credential: cert(JSON.parse(readFileSync(ruta, 'utf-8'))) });
  }
  mensajeriaCreada = getMessaging();
  return mensajeriaCreada;
}
