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
cuidadoRoutes.get('/', controlador.listarCronograma); // cronograma de todas las mascotas activas
cuidadoRoutes.get('/conteo', controlador.contar); // total de cuidados del usuario (debe ir antes de /:id)
cuidadoRoutes.get('/:id', controlador.obtener);
cuidadoRoutes.put('/:id', controlador.editar);
cuidadoRoutes.delete('/:id', controlador.eliminar);
cuidadoRoutes.post('/:id/actividades', controlador.completar); // marcar como completado (RF16 y RF17)

/** Una actividad del historial, montada en /actividades. */
export const actividadRoutes = Router();

actividadRoutes.use(exigirSesion);
actividadRoutes.put('/:id/nota', controlador.guardarNota); // agregar o editar la nota (RF18)
actividadRoutes.delete('/:id/nota', controlador.eliminarNota); // eliminar la nota (RF18)
actividadRoutes.post('/:id/anular', controlador.anular); // anular la actividad (RF19)
