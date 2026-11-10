import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './mapa.controller';

/**
 * Rutas del módulo mapa, montadas en /mapa. Todas exigen sesión iniciada y reciben sus datos en la URL.
 *
 * Parámetros comunes (todos opcionales):
 *  - lat, lng: ubicación del usuario. Con ellas se calcula "distanciaKm" y se puede ordenar por distancia.
 *  - norte, sur, este, oeste: área visible del mapa; solo se devuelve lo que está dentro.
 *
 * GET /mapa/veterinarias  tipo=municipal,privada  especie=perro,gato  servicio=1,2
 *                         disponibleAhora=true  ordenar=distancia|calificacion|nombre
 * GET /mapa/campanas      estado=proxima,activa  especie  servicio  costo=gratuita,de_pago
 *                         disponibleAhora=true  ordenar=inicio|fin|distancia|calificacion
 * GET /mapa/buscar        texto=...   (veterinarias, entidades organizadoras y campañas por nombre)
 * GET /mapa/filtros       especies y servicios disponibles para armar los filtros
 *
 * Dentro de un mismo filtro basta cumplir una opción (perro o gato); entre filtros distintos se deben cumplir todos.
 */
export const mapaRoutes = Router();

mapaRoutes.use(exigirSesion);

mapaRoutes.get('/veterinarias', controlador.veterinarias);
mapaRoutes.get('/campanas', controlador.campanas);
mapaRoutes.get('/buscar', controlador.buscar);
mapaRoutes.get('/filtros', controlador.filtros);
