import { Router } from 'express';
import multer from 'multer';
import { TAMANO_MAXIMO_FOTO } from '../../config/uploads';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './mascota.controller';

/** Rutas del módulo mascota, montadas en /mascotas. Todas exigen sesión iniciada. */
export const mascotaRoutes = Router();

mascotaRoutes.use(exigirSesion);

// La foto llega como archivo en el campo "foto" (multipart) y se mantiene en memoria hasta validarla.
const subida = multer({ storage: multer.memoryStorage(), limits: { fileSize: TAMANO_MAXIMO_FOTO, files: 1 } });

mascotaRoutes.get('/', controlador.listar);
mascotaRoutes.post('/', controlador.crear);
mascotaRoutes.get('/:id', controlador.obtener);
mascotaRoutes.put('/:id', controlador.editar);
mascotaRoutes.post('/:id/inactivar', controlador.inactivar);
mascotaRoutes.post('/:id/reactivar', controlador.reactivar);
mascotaRoutes.post('/:id/foto', subida.single('foto'), controlador.subirFoto);
mascotaRoutes.delete('/:id/foto', controlador.quitarFoto);
mascotaRoutes.delete('/:id', controlador.eliminar);
