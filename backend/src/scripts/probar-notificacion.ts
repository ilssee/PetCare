import { pool } from '../config/database';
import { mensajeria } from '../config/firebase';
import { buscarPorCorreo } from '../modules/usuario/usuario.repository';
import * as dispositivos from '../modules/dispositivo/dispositivo.repository';
import { EnviadorFirebase } from '../modules/recordatorio/recordatorio.firebase';

/**
 * Manda una notificación de prueba a los celulares de un usuario, para comprobar que Firebase funciona.
 * Uso: npm run notificacion:probar -- correo@ejemplo.com
 */
async function main() {
  const correo = process.argv[2];
  if (!correo) throw new Error('Indica el correo del usuario: npm run notificacion:probar -- correo@ejemplo.com');

  const conexion = mensajeria();
  if (!conexion) throw new Error('No hay clave de Firebase (backend/firebase-clave.json).');

  const usuario = await buscarPorCorreo(correo.trim().toLowerCase());
  if (!usuario) throw new Error(`No existe un usuario con el correo ${correo}.`);

  const celulares = await dispositivos.tokensDeUsuario(usuario.id);
  console.log(`${usuario.nombre} tiene ${celulares.length} celular(es) registrado(s).`);
  if (celulares.length === 0) {
    throw new Error('Ese usuario aún no registró ningún celular: abre la app con su sesión iniciada y acepta las notificaciones.');
  }

  const entregado = await new EnviadorFirebase(conexion).enviar({
    usuarioId: usuario.id,
    cuidadoId: 0,
    tipo: 'pendiente',
    titulo: 'Prueba de PetCare',
    cuerpo: 'Si ves este mensaje, las notificaciones funcionan.',
  });
  console.log(entregado ? 'Notificación enviada. Revisa tu celular.' : 'Firebase no pudo entregarla a ningún celular.');
}

main()
  .catch((error) => {
    console.error(error.message ?? error);
    process.exitCode = 1;
  })
  .finally(() => pool.end());
