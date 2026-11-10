import { Notificacion } from './recordatorio.avisos';

/**
 * Quien entrega un aviso al usuario. La lógica de cuándo avisar no depende de cómo se entrega:
 * hoy se muestra en consola y más adelante se envía como notificación push con Firebase.
 * Devuelve true si el aviso se entregó.
 */
export interface Enviador {
  enviar(aviso: Notificacion): Promise<boolean>;
}

/** Sustituto sin Firebase: muestra el aviso en la consola del servidor. */
export class EnviadorEnConsola implements Enviador {
  async enviar(aviso: Notificacion): Promise<boolean> {
    console.log(`[recordatorio] usuario ${aviso.usuarioId} · ${aviso.titulo}: ${aviso.cuerpo}`);
    return true;
  }
}
