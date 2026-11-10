-- =====================================================================
-- PetCare · 08_direccion.sql
-- Para bases de datos que ya existían: agrega la dirección escrita (por ejemplo "Av. Los Olivos 123, Lima")
-- a veterinaria y a campaña. Se muestra en el perfil de la veterinaria.
-- Se puede ejecutar varias veces: solo agrega la columna si todavía no existe.
-- (Las bases nuevas ya la traen desde 01_crear_tablas.sql.)
-- =====================================================================

USE petcare;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'veterinaria' AND COLUMN_NAME = 'direccion');
SET @sentencia = IF(@existe = 0,
  "ALTER TABLE veterinaria ADD COLUMN direccion VARCHAR(200) NULL COMMENT 'Dirección escrita, por ejemplo: Av. Los Olivos 123, Lima' AFTER ubicacion_long",
  'SELECT 1');
PREPARE ejecutar FROM @sentencia;
EXECUTE ejecutar;
DEALLOCATE PREPARE ejecutar;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'campana' AND COLUMN_NAME = 'direccion');
SET @sentencia = IF(@existe = 0,
  "ALTER TABLE campana ADD COLUMN direccion VARCHAR(200) NULL COMMENT 'Dirección escrita del lugar de la campaña' AFTER ubicacion_long",
  'SELECT 1');
PREPARE ejecutar FROM @sentencia;
EXECUTE ejecutar;
DEALLOCATE PREPARE ejecutar;
