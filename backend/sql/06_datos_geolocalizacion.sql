-- =====================================================================
-- PetCare · 06_datos_geolocalizacion.sql
-- Datos de ejemplo del módulo de geolocalización (HU08 y HU09). Reproducen los del prototipo de Figma.
-- TODOS LOS NOMBRES SON FICTICIOS; solo las coordenadas están en Lima.
--
-- IMPORTANTE: al empezar BORRA los datos de ejemplo anteriores (organizadores, veterinarias,
-- campañas, reseñas y servicios con id del 1 al 100) y los vuelve a crear. Se puede ejecutar
-- varias veces. Las fechas de las campañas se calculan desde el día en que se ejecuta, así
-- siempre hay campañas Activas y Próximas.
-- Estado de una campaña: Próxima (aún no empieza), Activa (en curso), Finalizada (ya terminó).
-- =====================================================================

USE petcare;

-- ---------------------------------------------------------------------
-- Limpieza de los datos de ejemplo (el orden respeta las claves foráneas)
-- ---------------------------------------------------------------------
DELETE FROM organizador WHERE id BETWEEN 1 AND 100; -- también borra veterinarias, campañas, horarios y reseñas
DELETE FROM servicio WHERE id BETWEEN 1 AND 100;
DELETE FROM categoria_servicio WHERE id BETWEEN 1 AND 100;

-- ---------------------------------------------------------------------
-- Categorías y servicios (los del prototipo)
-- ---------------------------------------------------------------------
INSERT INTO categoria_servicio (id, nombre) VALUES
  (1, 'Medicina preventiva'),
  (2, 'Especialidad'),
  (3, 'Bienestar e higiene');

INSERT INTO servicio (id, categoria_id, nombre) VALUES
  (1,  1, 'Vacunación'),
  (2,  1, 'Desparasitación'),
  (3,  1, 'Asesoría nutricional'),
  (4,  1, 'Control de peso'),
  (5,  1, 'Microchip'),
  (6,  1, 'Certificado de salud'),
  (7,  2, 'Cirugía'),
  (8,  2, 'Medicina interna'),
  (9,  2, 'Neurología'),
  (10, 2, 'Oftalmología'),
  (11, 2, 'Dermatología'),
  (12, 2, 'Odontología'),
  (13, 2, 'Imagenología'),
  (14, 2, 'Cardiología'),
  (15, 2, 'Oncología'),
  (16, 2, 'Traumatología'),
  (17, 3, 'Baño'),
  (18, 3, 'Corte de uñas'),
  (19, 3, 'Limpieza de oídos'),
  (20, 3, 'Cepillado'),
  (21, 3, 'Antipulgas'),
  (22, 3, 'Corte de pelo'),
  (23, 3, 'Spa');

-- ---------------------------------------------------------------------
-- Organizadores: 1 a 6 son veterinarias; 7, 8 y 9 son entidades externas
-- ---------------------------------------------------------------------
INSERT INTO organizador (id, nombre) VALUES
  (1, 'Clínica Veterinaria Patitas'),
  (2, 'Centro Veterinario Municipal Norte'),
  (3, 'Huellitas Pet Salud'),
  (4, 'Vet Conejos y Más'),
  (5, 'Centro de Zoonosis Municipal Sur'),
  (6, 'Clínica Animal Vida'),
  (7, 'ONG Huellas Unidas'),
  (8, 'Municipalidad Distrital Sur'),
  (9, 'Facultad de Veterinaria (campaña universitaria)');

INSERT INTO organizador_externo (organizador_id) VALUES (7), (8), (9);

INSERT INTO veterinaria (organizador_id, ubicacion_lat, ubicacion_long, tipo) VALUES
  (1, -12.066000, -77.042000, 'privada'),
  (2, -12.045000, -77.043000, 'municipal'),
  (3, -12.089000, -77.030000, 'privada'),
  (4, -12.105000, -77.056000, 'privada'),
  (5, -12.030000, -77.080000, 'municipal'),
  (6, -12.121000, -77.030000, 'privada');

-- ---------------------------------------------------------------------
-- Horarios de atención
--  1  Lun a Sáb 9:00–19:00 (sábado hasta las 13:00)    2  Lun a Vie 8:00–15:00
--  3  Todos los días 10:00–20:00                       4  Mar a Sáb 10:00–18:00
--  5  Lun a Vie 8:00–14:00                             6  24 horas, todos los días
-- ---------------------------------------------------------------------
INSERT INTO horario_atencion (veterinaria_id, dia, hora_apertura, hora_cierre)
SELECT v.id, d.dia, v.apertura, v.cierre
FROM (
  SELECT 1 AS id, '09:00:00' AS apertura, '19:00:00' AS cierre
  UNION ALL SELECT 2, '08:00:00', '15:00:00'
  UNION ALL SELECT 3, '10:00:00', '20:00:00'
  UNION ALL SELECT 4, '10:00:00', '18:00:00'
  UNION ALL SELECT 5, '08:00:00', '14:00:00'
  UNION ALL SELECT 6, '00:00:00', '23:59:00'
) v
JOIN (
  SELECT 1 AS veterinaria_id, 'lunes' AS dia UNION ALL SELECT 1, 'martes' UNION ALL SELECT 1, 'miercoles'
  UNION ALL SELECT 1, 'jueves' UNION ALL SELECT 1, 'viernes'
  UNION ALL SELECT 2, 'lunes' UNION ALL SELECT 2, 'martes' UNION ALL SELECT 2, 'miercoles'
  UNION ALL SELECT 2, 'jueves' UNION ALL SELECT 2, 'viernes'
  UNION ALL SELECT 3, 'lunes' UNION ALL SELECT 3, 'martes' UNION ALL SELECT 3, 'miercoles'
  UNION ALL SELECT 3, 'jueves' UNION ALL SELECT 3, 'viernes' UNION ALL SELECT 3, 'sabado' UNION ALL SELECT 3, 'domingo'
  UNION ALL SELECT 4, 'martes' UNION ALL SELECT 4, 'miercoles' UNION ALL SELECT 4, 'jueves'
  UNION ALL SELECT 4, 'viernes' UNION ALL SELECT 4, 'sabado'
  UNION ALL SELECT 5, 'lunes' UNION ALL SELECT 5, 'martes' UNION ALL SELECT 5, 'miercoles'
  UNION ALL SELECT 5, 'jueves' UNION ALL SELECT 5, 'viernes'
  UNION ALL SELECT 6, 'lunes' UNION ALL SELECT 6, 'martes' UNION ALL SELECT 6, 'miercoles'
  UNION ALL SELECT 6, 'jueves' UNION ALL SELECT 6, 'viernes' UNION ALL SELECT 6, 'sabado' UNION ALL SELECT 6, 'domingo'
) d ON d.veterinaria_id = v.id;

-- El sábado la clínica 1 atiende solo hasta la 1:00 p. m.
INSERT INTO horario_atencion (veterinaria_id, dia, hora_apertura, hora_cierre)
VALUES (1, 'sabado', '09:00:00', '13:00:00');

-- ---------------------------------------------------------------------
-- Especies y servicios que ofrece cada veterinaria
-- ---------------------------------------------------------------------
INSERT INTO veterinaria_especie (veterinaria_id, especie_id)
SELECT v.id, e.id
FROM (
  SELECT 1 AS id, 'perro' AS especie UNION ALL SELECT 1, 'gato' UNION ALL SELECT 1, 'conejo'
  UNION ALL SELECT 2, 'perro' UNION ALL SELECT 2, 'gato'
  UNION ALL SELECT 3, 'perro' UNION ALL SELECT 3, 'gato' UNION ALL SELECT 3, 'conejo'
  UNION ALL SELECT 4, 'conejo' UNION ALL SELECT 4, 'gato'
  UNION ALL SELECT 5, 'perro' UNION ALL SELECT 5, 'gato'
  UNION ALL SELECT 6, 'perro' UNION ALL SELECT 6, 'gato' UNION ALL SELECT 6, 'conejo'
) v
JOIN especie e ON e.nombre = v.especie;

INSERT INTO veterinaria_servicio (veterinaria_id, servicio_id) VALUES
  (1, 1), (1, 2), (1, 7), (1, 17), (1, 18),
  (2, 1), (2, 2), (2, 7),
  (3, 1), (3, 2), (3, 8), (3, 21), (3, 18),
  (4, 2), (4, 8), (4, 12), (4, 18),
  (5, 1), (5, 2);

-- La clínica 6 ofrece todo el catálogo.
INSERT INTO veterinaria_servicio (veterinaria_id, servicio_id)
SELECT 6, id FROM servicio WHERE id BETWEEN 1 AND 23;

-- ---------------------------------------------------------------------
-- Campañas. Las fechas se calculan desde hoy.
--  1 Próxima  de pago   veterinaria 1, cerca de ella (otra dirección)
--  2 Activa   gratuita  veterinaria 2, en SU MISMA ubicación  (probar RF28)
--  3 Activa   gratuita  ONG Huellas Unidas, sin veterinaria   (probar RF29)
--  4 Próxima  gratuita  Municipalidad Distrital Sur
--  5 Próxima  gratuita  Facultad de Veterinaria
-- ---------------------------------------------------------------------
INSERT INTO campana (id, organizador_id, nombre, descripcion, fecha_inicio, fecha_fin,
                     hora_apertura, hora_cierre, ubicacion_lat, ubicacion_long, modalidad_costo)
VALUES
  (1, 1, 'Jornada de esterilización solidaria',
      'Cirugías de esterilización a precio reducido. Con cita previa y ayuno de 8 horas.',
      CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 4 DAY,
      '09:00:00', '16:00:00', -12.064500, -77.040500, 'de_pago'),
  (2, 2, 'Vacunación antirrábica gratuita',
      'Aplicación de la vacuna antirrábica a perros y gatos mayores de 3 meses. Lleva a tu mascota con correa o transportadora.',
      CURDATE() - INTERVAL 1 DAY, CURDATE() + INTERVAL 3 DAY,
      '08:00:00', '23:00:00', -12.045000, -77.043000, 'gratuita'),
  (3, 7, 'Feria de adopción y desparasitación',
      'Feria con adopciones responsables y desparasitación gratuita para todas las mascotas.',
      CURDATE() - INTERVAL 1 DAY, CURDATE() + INTERVAL 2 DAY,
      '08:00:00', '23:00:00', -12.078000, -77.070000, 'gratuita'),
  (4, 8, 'Campaña de vacunación canina',
      'Vacunación canina gratuita en el local comunal.',
      CURDATE() + INTERVAL 9 DAY, CURDATE() + INTERVAL 10 DAY,
      '09:00:00', '15:00:00', -12.095000, -77.025000, 'gratuita'),
  (5, 9, 'Chequeo gratuito de conejos',
      'Revisión general y dental de conejos a cargo de estudiantes supervisados.',
      CURDATE() + INTERVAL 12 DAY, CURDATE() + INTERVAL 12 DAY,
      '10:00:00', '14:00:00', -12.056000, -77.087000, 'gratuita');

INSERT INTO campana_servicio (campana_id, servicio_id) VALUES
  (1, 7), (2, 1), (3, 2), (3, 21), (4, 1), (5, 8);

INSERT INTO campana_especie (campana_id, especie_id)
SELECT c.id, e.id
FROM (
  SELECT 1 AS id, 'perro' AS especie UNION ALL SELECT 1, 'gato'
  UNION ALL SELECT 2, 'perro' UNION ALL SELECT 2, 'gato'
  UNION ALL SELECT 3, 'perro' UNION ALL SELECT 3, 'gato'
  UNION ALL SELECT 4, 'perro'
  UNION ALL SELECT 5, 'conejo'
) c
JOIN especie e ON e.nombre = c.especie;

-- ---------------------------------------------------------------------
-- Reseñas de ejemplo (para mostrar y ordenar por calificación). Se asignan al primer usuario
-- registrado; si todavía no hay usuarios, no se inserta ninguna.
--   Clínica Veterinaria Patitas: 5, 5 y 4 (promedio 4.7)   Centro Municipal Norte: 4 y 3 (3.5)
-- ---------------------------------------------------------------------
INSERT INTO resena (id, usuario_id, organizador_id, calificacion, comentario)
SELECT r.id, u.id, r.organizador_id, r.calificacion, r.comentario
FROM (SELECT id FROM usuario ORDER BY id LIMIT 1) u
CROSS JOIN (
  SELECT 1 AS id, 1 AS organizador_id, 5 AS calificacion, 'Atención rápida y muy amable.' AS comentario
  UNION ALL SELECT 2, 1, 5, 'Excelente trato con mi gato.'
  UNION ALL SELECT 3, 1, 4, 'Buen servicio, un poco de espera.'
  UNION ALL SELECT 4, 2, 4, 'Buena atención en las campañas.'
  UNION ALL SELECT 5, 2, 3, 'Atienden bien pero hay mucha cola.'
  UNION ALL SELECT 6, 3, 4, 'Instalaciones limpias.'
  UNION ALL SELECT 7, 5, 3, 'Atención correcta.'
  UNION ALL SELECT 8, 6, 5, 'Muy profesionales, atienden a toda hora.'
  UNION ALL SELECT 9, 6, 4, 'Buen servicio de emergencias.'
  UNION ALL SELECT 10, 7, 5, 'Gran trabajo con las adopciones.'
) r;
