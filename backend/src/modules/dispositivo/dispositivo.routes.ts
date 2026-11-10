import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './dispositivo.controller';

/** Celulares del usuario para las notificaciones push, montado en /dispositivos. Exige sesión iniciada. */
export const dispositivoRoutes = Router();

dispositivoRoutes.use(exigirSesion);
dispositivoRoutes.post('/', controlador.registrar);
dispositivoRoutes.post('/baja', controlador.darDeBaja); // al cerrar sesión
