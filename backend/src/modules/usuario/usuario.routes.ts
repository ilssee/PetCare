import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './usuario.controller';

/** Rutas del módulo usuario, montadas en /usuarios. */
export const usuarioRoutes = Router();

usuarioRoutes.post('/registro', controlador.registrar);
usuarioRoutes.post('/login', controlador.iniciarSesion);
usuarioRoutes.get('/yo', exigirSesion, controlador.obtenerPerfil);
