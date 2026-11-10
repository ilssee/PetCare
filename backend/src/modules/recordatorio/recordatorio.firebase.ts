import { Messaging } from 'firebase-admin/messaging';
import * as dispositivos from '../dispositivo/dispositivo.repository';
import { Notificacion } from './recordatorio.avisos';
import { Enviador } from './recordatorio.enviador';

/** Códigos con los que Firebase avisa que el celular ya no existe (app desinstalada o token vencido). */
const TOKEN_INVALIDO = ['messaging/registration-token-not-registered', 'messaging/invalid-registration-token'];

/** Canal de notificaciones de Android donde la app muestra los recordatorios. */
export const CANAL_RECORDATORIOS = 'recordatorios';

/** Los avisos de lugares cercanos abren el mapa; los demás abren los cuidados. */
function seccionDe(tipo: Notificacion['tipo']): string {
  return tipo === 'campana_cercana' || tipo === 'veterinaria_cercana' ? 'mapa' : 'cuidados';
}

/** Entrega los avisos como notificaciones push a todos los celulares del usuario, usando Firebase. */
export class EnviadorFirebase implements Enviador {
  constructor(private readonly mensajeria: Messaging) {}

  /**
   * Devuelve true si al menos un celular recibió el aviso. Sin celulares registrados no hay a dónde
   * enviar, así que devuelve false.
   */
  async enviar(aviso: Notificacion): Promise<boolean> {
    const tokens = await dispositivos.tokensDeUsuario(aviso.usuarioId);
    if (tokens.length === 0) return false;

    const respuesta = await this.mensajeria.sendEachForMulticast({
      tokens,
      notification: { title: aviso.titulo, body: aviso.cuerpo },
      // La sección dice qué pestaña abre la app al tocar el aviso.
      data: { tipo: aviso.tipo, cuidadoId: String(aviso.cuidadoId), seccion: seccionDe(aviso.tipo) },
      android: { priority: 'high', notification: { channelId: CANAL_RECORDATORIOS } },
    });

    // Los celulares que Firebase ya no reconoce se borran para no volver a intentarlo.
    const caducados = tokens.filter((_, i) => {
      const codigo = respuesta.responses[i].error?.code;
      return codigo !== undefined && TOKEN_INVALIDO.includes(codigo);
    });
    await dispositivos.quitarTokens(caducados);

    return respuesta.successCount > 0;
  }
}
