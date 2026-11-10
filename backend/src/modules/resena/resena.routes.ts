import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './resena.controller';

/**
 * Rutas del módulo reseña, montadas en /resenas. Todas exigen sesión iniciada.
 *
 * GET    /resenas/organizadores/:id?orden=recientes|calificadas   reseñas de una veterinaria u organizador (RF44)
 * POST   /resenas/organizadores/:id   { calificacion, comentario }   escribir una reseña (RF45)
 * PUT    /resenas/:id                 { calificacion, comentario }   editar una reseña propia (RF46)
 * DELETE /resenas/:id                                                eliminar una reseña propia (RF46)
 */
export const resenaRoutes = Router();

resenaRoutes.use(exigirSesion);

resenaRoutes.get('/organizadores/:id', controlador.listar);
resenaRoutes.post('/organizadores/:id', controlador.crear);
resenaRoutes.put('/:id', controlador.editar);
resenaRoutes.delete('/:id', controlador.eliminar);
