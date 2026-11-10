import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './cercania.controller';

/**
 * Rutas del módulo cercanía, montadas en /cercania. Exigen sesión iniciada.
 *
 * GET /cercania  devuelve { lat, lng, radioKm, notificarCampanas, notificarVeterinarias }
 * PUT /cercania  guarda esos mismos datos. lat, lng y radioKm son opcionales (null para borrarlos),
 *                pero sin ellos no se puede activar ningún aviso.
 */
export const cercaniaRoutes = Router();

cercaniaRoutes.use(exigirSesion);

cercaniaRoutes.get('/', controlador.obtener);
cercaniaRoutes.put('/', controlador.guardar);
