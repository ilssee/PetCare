-- =====================================================================
-- PetCare · 04_reglas_preventivas.sql
-- Repositorio de reglas preventivas (R01 a R28) tomado del documento
-- "Documento de revisión de cuidados" (Tablas 4, 5, 6 y 9).
-- El id de cada regla coincide con su número en el documento (R01 = 1).
-- Se puede ejecutar varias veces: actualiza el texto de las reglas sin
-- tocar fecha_incorporacion ni fecha_notificacion.
-- Requiere haber ejecutado antes 01_crear_tablas.sql y 02_datos_iniciales.sql.
-- =====================================================================

USE petcare;
SET NAMES utf8mb4;

-- R01 · C01 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (1, (SELECT id FROM especie WHERE nombre = 'perro'), 'Vacuna contra parvovirus',
  'Protege frente a una enfermedad viral que afecta principalmente el intestino y la médula ósea, y puede transmitirse por contacto con perros infectados, sus heces o ambientes contaminados.',
  'Es importante porque puede ser mortal, especialmente en cachorros y perros no vacunados, y el virus puede permanecer durante largos periodos en ambientes contaminados.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R02 · C02 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (2, (SELECT id FROM especie WHERE nombre = 'perro'), 'Vacuna contra moquillo canino (Distemper)',
  'Protege frente a una enfermedad viral altamente contagiosa que puede afectar los sistemas respiratorio, digestivo y nervioso del perro.',
  'Es importante porque puede evolucionar a cuadros graves y producir complicaciones neurológicas de difícil tratamiento.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R03 · C03 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (3, (SELECT id FROM especie WHERE nombre = 'perro'), 'Vacuna contra hepatitis',
  'Protege frente a una enfermedad viral contagiosa que puede afectar principalmente los vasos sanguíneos, el hígado, los riñones, el bazo y los pulmones.',
  'Es importante porque puede causar cuadros graves, especialmente en perros jóvenes.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R04 · C04 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (4, (SELECT id FROM especie WHERE nombre = 'perro'), 'Vacuna contra parainfluenza',
  'Protege frente a una infección respiratoria asociada a la tos de las perreras, que puede propagarse rápidamente entre perros que conviven en espacios cerrados.',
  'Es importante porque, aunque suele ser leve, puede generar complicaciones respiratorias más graves, especialmente en animales vulnerables.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R05 · C05 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (5, (SELECT id FROM especie WHERE nombre = 'perro'), 'Vacuna contra leptospirosis',
  'Protege frente a una enfermedad bacteriana que puede afectar distintos órganos, principalmente los riñones y el hígado.',
  'Es importante porque puede provocar daños graves o la muerte y también puede transmitirse de los animales a las personas.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R06 · C06 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (6, (SELECT id FROM especie WHERE nombre = 'perro'), 'Vacuna contra rabia',
  'Protege frente a una enfermedad viral grave que afecta el sistema nervioso central y se transmite mediante la saliva a través de mordeduras o arañazos, siendo el perro el reservorio natural del virus.',
  'Es importante porque la rabia es una enfermedad mortal si no se previene a tiempo, pero es prevenible mediante la vacunación. Vacunar al perro evita que enferme y previene la transmisión del virus a las personas y a otros animales del hogar.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R07 · C13 · gato
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (7, (SELECT id FROM especie WHERE nombre = 'gato'), 'Vacuna contra panleucopenia',
  'Protege frente a una enfermedad viral altamente contagiosa que afecta a los gatos y provoca una disminución de los glóbulos blancos.',
  'Es importante porque puede ser mortal, especialmente en gatitos, y el virus puede permanecer en el ambiente durante más de un año.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R08 · C14 · gato
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (8, (SELECT id FROM especie WHERE nombre = 'gato'), 'Vacuna contra herpesvirus',
  'Protege frente a una enfermedad que afecta principalmente las vías respiratorias superiores, los ojos y la nariz del gato.',
  'Es importante porque el virus puede permanecer inactivo de por vida y reactivarse durante periodos de estrés.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R09 · C15 · gato
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (9, (SELECT id FROM especie WHERE nombre = 'gato'), 'Vacuna contra calicivirus',
  'Protege frente a una infección que puede afectar principalmente la boca y las vías respiratorias del gato.',
  'Es importante porque los gatos recuperados pueden continuar eliminando y propagando el virus durante un periodo prolongado.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R10 · C16 · gato
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (10, (SELECT id FROM especie WHERE nombre = 'gato'), 'Vacuna contra rabia',
  'Protege frente a la rabia en el gato, quien puede contagiarse de forma accidental al ser mordido por un perro u otro animal infectado.',
  'Es importante porque, al igual que en los perros, protege al felino de una infección viral mortal y evita que se convierta en un riesgo de transmisión para la salud pública y para los miembros del hogar.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R11 · C24 · conejo · hembra
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (11, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Esterilización preventiva',
  'Procedimiento quirúrgico que consiste en la extirpación de los ovarios y el útero mediante una incisión abdominal, bajo anestesia general.',
  'Elimina o reduce drásticamente el riesgo de cáncer uterino y ovárico, disminuye las conductas agresivas y evita gestaciones no deseadas.',
  CURDATE(), 'hembra')
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R12 · C25 · conejo · macho
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (12, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Castración preventiva',
  'Procedimiento quirúrgico que consiste en la extirpación de los testículos bajo anestesia general.',
  'Elimina el riesgo de cáncer testicular, reduce la agresividad y el marcaje territorial con orina, y evita gestaciones no deseadas.',
  CURDATE(), 'macho')
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R13 · C07 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (13, (SELECT id FROM especie WHERE nombre = 'perro'), 'Limpieza dental en casa',
  'Consiste en cepillar los dientes del perro con un cepillo y una pasta dental formulados para perros.',
  'Es la medida más eficaz para evitar la acumulación de placa y sarro entre las limpiezas profesionales, y ayuda a prevenir enfermedades de las encías.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R14 · C08 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (14, (SELECT id FROM especie WHERE nombre = 'perro'), 'Limpieza dental profesional',
  'Procedimiento realizado por el veterinario que incluye la limpieza profunda y el pulido de los dientes, y que puede incluir radiografías para evaluar la raíz y el hueso de la mandíbula. El veterinario determina cuándo realizarlo según el estado de la boca de cada perro.',
  'Permite limpiar debajo de la encía, donde se originan la mayoría de las infecciones dentales, y previene dolor, pérdida de dientes y que la enfermedad periodontal afecte a órganos como los riñones, el hígado y el corazón.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R15 · C09 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (15, (SELECT id FROM especie WHERE nombre = 'perro'), 'Cepillado del pelaje',
  'Consiste en cepillar periódicamente el pelaje para retirar pelo suelto, prevenir enredos y mantener el manto limpio.',
  'Ayuda a evitar la formación de enredos y a reducir el riesgo de irritación de la piel e infecciones asociadas al pelo enredado.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R16 · C11 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (16, (SELECT id FROM especie WHERE nombre = 'perro'), 'Baño',
  'Consiste en el baño de higiene con champú formulado para mascotas.',
  'Ayuda a reducir el riesgo de irritación de la piel e infecciones secundarias asociadas al pelo enredado o sucio.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R17 · C10 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (17, (SELECT id FROM especie WHERE nombre = 'perro'), 'Limpieza de oídos',
  'Consiste en limpiar e inspeccionar suavemente la parte interna de las orejas y el canal auditivo utilizando un algodón húmedo o un paño suave.',
  'Previene la acumulación de cerumen y humedad que pueden desencadenar infecciones auditivas y evaluar oportunamente la salud del oído para acudir al veterinario ante cualquier alteración.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R18 · C17 · gato
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (18, (SELECT id FROM especie WHERE nombre = 'gato'), 'Limpieza dental en casa',
  'Consiste en cepillar los dientes del gato para mantener la salud de sus dientes y encías. Suele requerir más paciencia y acostumbrarlo de forma gradual, y conviene consultar con el veterinario la técnica adecuada.',
  'Ayuda a prevenir la acumulación de placa, que puede provocar enfermedades de las encías y pérdida de dientes.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R19 · C18 · gato
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (19, (SELECT id FROM especie WHERE nombre = 'gato'), 'Limpieza dental profesional',
  'Procedimiento realizado por el veterinario que incluye la limpieza profunda y el pulido de los dientes del gato, y que puede incluir radiografías para evaluar la raíz y el hueso de la mandíbula. El veterinario determina cuándo realizarlo según el estado de la boca de cada gato.',
  'Permite limpiar debajo de la encía, donde se originan la mayoría de las infecciones dentales, y previene dolor, pérdida de dientes y que la enfermedad periodontal afecte a órganos como los riñones, el hígado y el corazón.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R20 · C20 · conejo
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (20, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Revisión general',
  'Consiste en una revisión médica completa del estado general del conejo, realizada por un veterinario con experiencia en conejos, que incluye la evaluación de sus dientes molares y premolares. Se recomienda hacerla desde que se adopta al conejo y luego de forma periódica.',
  'Permite detectar a tiempo alteraciones dentales, de peso o de temperatura antes de que causen mayor sufrimiento y facilita un tratamiento más oportuno. Al adoptar, la revisión ayuda a descubrir si el conejo ya tiene algún problema de salud y a definir con el veterinario su plan de cuidados, ya que su salud no está garantizada aunque se informen los problemas conocidos.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R21 · C21 · conejo
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (21, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Revisión y corte de uñas',
  'Consiste en revisar y recortar periódicamente las uñas del conejo.',
  'Previene problemas asociados a su crecimiento excesivo.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R22 · C22 · conejo
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (22, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Limpieza del área de eliminación',
  'Consiste en mantener higiénica el área de eliminación del conejo.',
  'Ayuda a mantener un entorno saludable y a prevenir infecciones.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R23 · C23 · conejo
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (23, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Limpieza del alojamiento, comederos y bebederos',
  'Consiste en mantener higiénicos el alojamiento, comederos y bebederos del conejo. El alojamiento debe ser lo más amplio posible y el conejo no debería permanecer siempre dentro de él: necesita salir todos los días a un corral de ejercicio.',
  'Ayuda a mantener un entorno saludable y a prevenir infecciones. Además, un espacio amplio y salir de la jaula todos los días le permiten expresar sus conductas naturales y reducen su estrés.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R24 · C26 · conejo
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (24, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Alimentación adecuada',
  'Consiste en proporcionar al conejo una alimentación adecuada a sus necesidades, basada principalmente en heno y complementada con otros alimentos recomendados.',
  'Una dieta pobre en fibra es la causa más común de que los dientes crezcan en exceso, lo que provoca dolor, llagas en la boca, dificultad para comer y problemas digestivos graves. Como las necesidades cambian según la etapa de vida, conviene que el veterinario indique la dieta y consultarle antes de introducir alimentos nuevos.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R25 · C27 · conejo
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (25, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Hidratación constante',
  'Consiste en proporcionar al conejo agua limpia y disponible en todo momento, preferentemente en un recipiente abierto.',
  'Es importante para mantener una adecuada hidratación y prevenir problemas relacionados con la falta de agua.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R26 · C28 · conejo
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (26, (SELECT id FROM especie WHERE nombre = 'conejo'), 'Revisión dental en casa',
  'Consiste en observar periódicamente los incisivos (los dientes de adelante) del conejo levantando suavemente sus labios, sin forzar nunca la apertura de la boca, ya que sus dientes crecen de forma continua y necesitan desgastarse. Además de los incisivos, el conejo tiene 22 muelas, entre premolares y molares, al fondo de la boca.',
  'Permite detectar a tiempo dientes demasiado largos, desviados o rotos, evitando que provoquen heridas en la boca, salivación, infecciones o dificultad para comer. Los premolares y molares son muy difíciles de ver y forzar la boca puede lesionar al conejo, por lo que es mejor que los revise un veterinario especialista.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R27 · C12 · perro
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (27, (SELECT id FROM especie WHERE nombre = 'perro'), 'Revisión general',
  'Consiste en llevar al perro al veterinario para un control completo de su estado general. Se recomienda hacerlo apenas se adopta y luego de forma periódica; los cachorros y los perros mayores necesitan revisiones más frecuentes.',
  'Permite que el veterinario detecte a tiempo problemas de salud, con análisis de sangre o radiografías cuando sea necesario. Al adoptar un perro, la revisión asegura la continuidad de sus cuidados y permite definir con el veterinario un plan preventivo adaptado a sus necesidades, para prevenir enfermedades futuras.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- R28 · C19 · gato
INSERT INTO regla (id, especie_id, nombre, descripcion, importancia, fecha_incorporacion, sexo_aplicable)
VALUES (28, (SELECT id FROM especie WHERE nombre = 'gato'), 'Revisión general',
  'Consiste en llevar al gato al veterinario para un chequeo completo de su estado general. Se recomienda hacerlo apenas se adopta y luego de forma periódica; los gatitos y los gatos mayores necesitan revisiones más frecuentes.',
  'Permite que el veterinario vigile la salud del gato y detecte de forma temprana signos de enfermedades, por ejemplo con análisis de sangre periódicos en gatos mayores. Al adoptar un gato, la revisión permite definir con el veterinario un plan preventivo adaptado a sus necesidades y a su estilo de vida, incluidos los tratamientos y pruebas contra parásitos.',
  CURDATE(), NULL)
ON DUPLICATE KEY UPDATE especie_id = VALUES(especie_id), nombre = VALUES(nombre),
  descripcion = VALUES(descripcion), importancia = VALUES(importancia),
  sexo_aplicable = VALUES(sexo_aplicable);

-- Comprobación: 28 reglas en total (12 perro, 7 gato, 9 conejo)
SELECT e.nombre AS especie, COUNT(*) AS reglas FROM regla r
JOIN especie e ON e.id = r.especie_id GROUP BY e.nombre;
