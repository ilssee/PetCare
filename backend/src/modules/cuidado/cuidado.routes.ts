import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './cuidado.controller';

/** Cuidados de una mascota, montados en /mascotas porque cada cuidado pertenece a una mascota. */
export const cuidadoDeMascotaRoutes = Router();

cuidadoDeMascotaRoutes.get('/:id/cuidados', exigirSesion, controlador.listar);
cuidadoDeMascotaRoutes.post('/:id/cuidados', exigirSesion, controlador.crearPersonalizado);
cuidadoDeMascotaRoutes.post('/:id/cuidados/recomendados', exigirSesion, controlador.activarRecomendados);

/** Un cuidado concreto, montado en /cuidados. */
export const cuidadoRoutes = Router();

cuidadoRoutes.use(exigirSesion);
cuidadoRoutes.get('/:id', controlador.obtener);
cuidadoRoutes.put('/:id', controlador.editar);
cuidadoRoutes.delete('/:id', controlador.eliminar);
