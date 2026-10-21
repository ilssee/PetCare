import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './mascota.controller';

/** Rutas del módulo mascota, montadas en /mascotas. Todas exigen sesión iniciada. */
export const mascotaRoutes = Router();

mascotaRoutes.use(exigirSesion);

mascotaRoutes.get('/', controlador.listar);
mascotaRoutes.post('/', controlador.crear);
mascotaRoutes.get('/:id', controlador.obtener);
mascotaRoutes.put('/:id', controlador.editar);
mascotaRoutes.post('/:id/inactivar', controlador.inactivar);
mascotaRoutes.post('/:id/reactivar', controlador.reactivar);
mascotaRoutes.delete('/:id', controlador.eliminar);
