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
      // Solo datos (sin bloque "notification"): así la app arma siempre el aviso, también con la app
      // cerrada, y puede poner de ícono el dibujo de la mascota o el de la campaña o veterinaria.
      // El título y el texto viajan igual que antes. La sección dice qué pestaña abre la app al tocarlo.
      data: {
        tipo: aviso.tipo,
        cuidadoId: String(aviso.cuidadoId),
        seccion: seccionDe(aviso.tipo),
        titulo: aviso.titulo,
        cuerpo: aviso.cuerpo,
        especie: aviso.especie ?? '',
        variante: aviso.variante ?? '',
        // Qué pantalla se abre al tocar el aviso (vacíos si solo se abre la sección).
        lugarId: aviso.lugarId ? String(aviso.lugarId) : '',
        veterinariaId: aviso.veterinariaId ? String(aviso.veterinariaId) : '',
        mascotaId: aviso.mascotaId ? String(aviso.mascotaId) : '',
        reglas: (aviso.reglasIds ?? []).join(','),
        canal: CANAL_RECORDATORIOS,
      },
      android: { priority: 'high' },
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
