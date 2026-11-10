-- =====================================================================
-- PetCare · 01_crear_tablas.sql
-- Crea el esquema "petcare" y sus 23 tablas, a partir del DER y del
-- diccionario de datos (Anexo I: modelo de datos del repositorio centralizado).
-- 
-- Requiere MySQL 8.0.16 o superior.
-- No borra nada: usa CREATE ... IF NOT EXISTS.
--
-- Notas de diseño:
--  * Atributos derivados (edad, calificación promedio, total de reseñas,
--    estado de la campaña) NO se guardan: se calculan en el backend.
--  * Atributos multivaluados (días de disponibilidad, fotos, teléfonos,
--    redes sociales) van en tablas aparte.
--  * Fotos: se guarda la ruta (texto) del archivo, no la imagen.
--  * La contraseña se guarda cifrada (hash), nunca en texto plano.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS petcare
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE petcare;

-- ---------------------------------------------------------------------
-- MÓDULO DE SEGUIMIENTO
-- ---------------------------------------------------------------------

-- Usuario: dueño de mascota registrado
CREATE TABLE IF NOT EXISTS usuario (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(100) NOT NULL,
  correo      VARCHAR(255) NOT NULL,
  contrasena  VARCHAR(255) NOT NULL COMMENT 'Contraseña cifrada (hash)',
  PRIMARY KEY (id),
  UNIQUE KEY uq_usuario_correo (correo)
) ENGINE=InnoDB;

-- Preferencias: recordatorios y cercanía (relación 1 a 1 con usuario)
CREATE TABLE IF NOT EXISTS preferencias (
  id                           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id                   INT UNSIGNED NOT NULL,
  ubicacion_ref_lat            DECIMAL(9,6) NULL,
  ubicacion_ref_long           DECIMAL(9,6) NULL,
  radio_interes                DECIMAL(5,1) NULL COMMENT 'Kilómetros',
  notificar_nuevas_campanas    BOOLEAN NOT NULL,
  notificar_nuevas_veterinarias BOOLEAN NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_preferencias_usuario (usuario_id),
  CONSTRAINT fk_preferencias_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT ck_preferencias_radio CHECK (radio_interes IS NULL OR radio_interes > 0)
) ENGINE=InnoDB;

-- Días de la semana en que el usuario acepta recordatorios (multivaluado)
CREATE TABLE IF NOT EXISTS preferencias_dia (
  preferencias_id INT UNSIGNED NOT NULL,
  dia ENUM('lunes','martes','miercoles','jueves','viernes','sabado','domingo') NOT NULL,
  PRIMARY KEY (preferencias_id, dia),
  CONSTRAINT fk_prefdia_preferencias FOREIGN KEY (preferencias_id)
    REFERENCES preferencias (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Especie: catálogo (perro, gato, conejo)
CREATE TABLE IF NOT EXISTS especie (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_especie_nombre (nombre)
) ENGINE=InnoDB;

-- Mascota
CREATE TABLE IF NOT EXISTS mascota (
  id               INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id       INT UNSIGNED NOT NULL,
  especie_id       INT UNSIGNED NOT NULL,
  nombre           VARCHAR(50)  NOT NULL,
  raza             VARCHAR(80)  NULL COMMENT 'Opcional (ERS RF05)',
  fecha_nacimiento DATE NULL COMMENT 'Opcional (ERS RF05). No puede ser futura: se valida en el backend',
  sexo             ENUM('macho','hembra') NOT NULL,
  peso             DECIMAL(5,2) NULL COMMENT 'Kilogramos. Opcional (ERS RF05)',
  foto             VARCHAR(255) NULL COMMENT 'Ruta del archivo de imagen (opcional)',
  estado           ENUM('activa','inactiva') NOT NULL DEFAULT 'activa',
  PRIMARY KEY (id),
  KEY ix_mascota_usuario (usuario_id),
  CONSTRAINT fk_mascota_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_mascota_especie FOREIGN KEY (especie_id)
    REFERENCES especie (id),
  CONSTRAINT ck_mascota_peso CHECK (peso > 0)
) ENGINE=InnoDB;

-- Regla: recomendación preventiva (cada regla aplica a una sola especie)
CREATE TABLE IF NOT EXISTS regla (
  id                   INT UNSIGNED NOT NULL AUTO_INCREMENT,
  especie_id           INT UNSIGNED NOT NULL,
  nombre               VARCHAR(150) NOT NULL,
  descripcion          TEXT NOT NULL,
  importancia          TEXT NOT NULL,
  fecha_incorporacion  DATE NOT NULL,
  fecha_notificacion   DATE NULL COMMENT 'Vacía hasta que se avisa a los usuarios',
  sexo_aplicable       ENUM('macho','hembra') NULL COMMENT 'Vacío si aplica a ambos',
  PRIMARY KEY (id),
  KEY ix_regla_especie (especie_id),
  CONSTRAINT fk_regla_especie FOREIGN KEY (especie_id)
    REFERENCES especie (id)
) ENGINE=InnoDB;

-- Cuidado: tarea de salud de una mascota (nace de una regla o la define el usuario)
CREATE TABLE IF NOT EXISTS cuidado (
  id                INT UNSIGNED NOT NULL AUTO_INCREMENT,
  mascota_id        INT UNSIGNED NOT NULL,
  regla_id          INT UNSIGNED NULL,
  nombre            VARCHAR(150) NOT NULL,
  descripcion       TEXT NOT NULL,
  comentario        TEXT NULL,
  modo_termino      ENUM('una_vez','varias_veces','hasta_fecha','indefinido') NOT NULL,
  frecuencia_valor  SMALLINT UNSIGNED NULL,
  frecuencia_unidad ENUM('dias','semanas','meses','anios') NULL,
  total_repeticiones SMALLINT UNSIGNED NULL,
  fecha_limite      DATE NULL,
  fecha_recurrente  DATE NOT NULL COMMENT 'Próxima fecha; se recalcula al completar',
  aviso_anticipado  BOOLEAN NOT NULL,
  estado            ENUM('pendiente','finalizado','eliminado') NOT NULL DEFAULT 'pendiente',
  PRIMARY KEY (id),
  KEY ix_cuidado_mascota_estado (mascota_id, estado),
  KEY ix_cuidado_fecha_recurrente (fecha_recurrente),
  CONSTRAINT fk_cuidado_mascota FOREIGN KEY (mascota_id)
    REFERENCES mascota (id) ON DELETE CASCADE,
  CONSTRAINT fk_cuidado_regla FOREIGN KEY (regla_id)
    REFERENCES regla (id) ON DELETE SET NULL,
  -- Los campos dependen del modo de término:
  CONSTRAINT ck_cuidado_frecuencia CHECK (
    modo_termino = 'una_vez'
    OR (frecuencia_valor IS NOT NULL AND frecuencia_unidad IS NOT NULL)),
  CONSTRAINT ck_cuidado_repeticiones CHECK (
    (modo_termino = 'varias_veces') = (total_repeticiones IS NOT NULL)),
  CONSTRAINT ck_cuidado_limite CHECK (
    (modo_termino = 'hasta_fecha') = (fecha_limite IS NOT NULL))
) ENGINE=InnoDB;

-- Actividad: ocasión en que un cuidado fue completado (historial)
CREATE TABLE IF NOT EXISTS actividad (
  id                INT UNSIGNED NOT NULL AUTO_INCREMENT,
  cuidado_id        INT UNSIGNED NOT NULL,
  fecha_planificada DATE NOT NULL,
  fecha_realizacion DATE NOT NULL,
  estado            ENUM('completada','anulada') NOT NULL DEFAULT 'completada',
  nota              TEXT NULL,
  PRIMARY KEY (id),
  KEY ix_actividad_cuidado (cuidado_id),
  CONSTRAINT fk_actividad_cuidado FOREIGN KEY (cuidado_id)
    REFERENCES cuidado (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- MÓDULO DE GEOLOCALIZACIÓN
-- ---------------------------------------------------------------------

-- Organizador: supertipo de Veterinaria y Organizador externo
CREATE TABLE IF NOT EXISTS organizador (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(150) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Organizador externo: no es una veterinaria (p. ej. una institución pública)
CREATE TABLE IF NOT EXISTS organizador_externo (
  organizador_id INT UNSIGNED NOT NULL,
  PRIMARY KEY (organizador_id),
  CONSTRAINT fk_orgext_organizador FOREIGN KEY (organizador_id)
    REFERENCES organizador (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Veterinaria: establecimiento con ubicación fija
CREATE TABLE IF NOT EXISTS veterinaria (
  organizador_id INT UNSIGNED NOT NULL,
  ubicacion_lat  DECIMAL(9,6) NOT NULL,
  ubicacion_long DECIMAL(9,6) NOT NULL,
  tipo           ENUM('municipal','privada') NOT NULL,
  fecha_notificacion DATE NULL COMMENT 'Vacía hasta que se avisa a los usuarios cercanos (RF40)',
  PRIMARY KEY (organizador_id),
  CONSTRAINT fk_veterinaria_organizador FOREIGN KEY (organizador_id)
    REFERENCES organizador (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Atributos multivaluados de Veterinaria
CREATE TABLE IF NOT EXISTS veterinaria_foto (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  veterinaria_id INT UNSIGNED NOT NULL,
  ruta           VARCHAR(255) NOT NULL COMMENT 'Ruta del archivo de imagen',
  PRIMARY KEY (id),
  KEY ix_vetfoto_veterinaria (veterinaria_id),
  CONSTRAINT fk_vetfoto_veterinaria FOREIGN KEY (veterinaria_id)
    REFERENCES veterinaria (organizador_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS veterinaria_telefono (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  veterinaria_id INT UNSIGNED NOT NULL,
  telefono       VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  KEY ix_vettel_veterinaria (veterinaria_id),
  CONSTRAINT fk_vettel_veterinaria FOREIGN KEY (veterinaria_id)
    REFERENCES veterinaria (organizador_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS veterinaria_red_social (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  veterinaria_id INT UNSIGNED NOT NULL,
  enlace         VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  KEY ix_vetred_veterinaria (veterinaria_id),
  CONSTRAINT fk_vetred_veterinaria FOREIGN KEY (veterinaria_id)
    REFERENCES veterinaria (organizador_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Horario de atención: franjas por día de la semana
CREATE TABLE IF NOT EXISTS horario_atencion (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  veterinaria_id INT UNSIGNED NOT NULL,
  dia            ENUM('lunes','martes','miercoles','jueves','viernes','sabado','domingo') NOT NULL,
  hora_apertura  TIME NOT NULL,
  hora_cierre    TIME NOT NULL,
  PRIMARY KEY (id),
  KEY ix_horario_veterinaria (veterinaria_id),
  CONSTRAINT fk_horario_veterinaria FOREIGN KEY (veterinaria_id)
    REFERENCES veterinaria (organizador_id) ON DELETE CASCADE,
  CONSTRAINT ck_horario_horas CHECK (hora_cierre > hora_apertura)
) ENGINE=InnoDB;

-- Catálogo de servicios y sus categorías
CREATE TABLE IF NOT EXISTS categoria_servicio (
  id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categoria_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS servicio (
  id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  categoria_id INT UNSIGNED NOT NULL,
  nombre       VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_servicio_nombre (nombre),
  KEY ix_servicio_categoria (categoria_id),
  CONSTRAINT fk_servicio_categoria FOREIGN KEY (categoria_id)
    REFERENCES categoria_servicio (id)
) ENGINE=InnoDB;

-- Campaña de salud
CREATE TABLE IF NOT EXISTS campana (
  id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
  organizador_id  INT UNSIGNED NOT NULL,
  nombre          VARCHAR(150) NOT NULL,
  descripcion     TEXT NOT NULL,
  fecha_inicio    DATE NOT NULL,
  fecha_fin       DATE NOT NULL,
  hora_apertura   TIME NOT NULL,
  hora_cierre     TIME NOT NULL,
  ubicacion_lat   DECIMAL(9,6) NOT NULL,
  ubicacion_long  DECIMAL(9,6) NOT NULL,
  modalidad_costo ENUM('gratuita','de_pago') NOT NULL,
  fecha_notificacion DATE NULL COMMENT 'Vacía hasta que se avisa a los usuarios cercanos (RF39)',
  PRIMARY KEY (id),
  KEY ix_campana_organizador (organizador_id),
  KEY ix_campana_fechas (fecha_inicio, fecha_fin),
  CONSTRAINT fk_campana_organizador FOREIGN KEY (organizador_id)
    REFERENCES organizador (id) ON DELETE CASCADE,
  CONSTRAINT ck_campana_fechas CHECK (fecha_fin >= fecha_inicio),
  CONSTRAINT ck_campana_horas  CHECK (hora_cierre > hora_apertura)
) ENGINE=InnoDB;

-- Reseña: un usuario puede escribir varias reseñas sobre un organizador
CREATE TABLE IF NOT EXISTS resena (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id     INT UNSIGNED NOT NULL,
  organizador_id INT UNSIGNED NOT NULL,
  calificacion   TINYINT UNSIGNED NOT NULL,
  comentario     TEXT NULL,
  fecha          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_resena_organizador (organizador_id),
  KEY ix_resena_usuario (usuario_id),
  CONSTRAINT fk_resena_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_resena_organizador FOREIGN KEY (organizador_id)
    REFERENCES organizador (id) ON DELETE CASCADE,
  CONSTRAINT ck_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- TABLAS PUENTE (relaciones muchos a muchos)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS veterinaria_servicio (
  veterinaria_id INT UNSIGNED NOT NULL,
  servicio_id    INT UNSIGNED NOT NULL,
  PRIMARY KEY (veterinaria_id, servicio_id),
  CONSTRAINT fk_vetserv_veterinaria FOREIGN KEY (veterinaria_id)
    REFERENCES veterinaria (organizador_id) ON DELETE CASCADE,
  CONSTRAINT fk_vetserv_servicio FOREIGN KEY (servicio_id)
    REFERENCES servicio (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS veterinaria_especie (
  veterinaria_id INT UNSIGNED NOT NULL,
  especie_id     INT UNSIGNED NOT NULL,
  PRIMARY KEY (veterinaria_id, especie_id),
  CONSTRAINT fk_vetesp_veterinaria FOREIGN KEY (veterinaria_id)
    REFERENCES veterinaria (organizador_id) ON DELETE CASCADE,
  CONSTRAINT fk_vetesp_especie FOREIGN KEY (especie_id)
    REFERENCES especie (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS campana_servicio (
  campana_id  INT UNSIGNED NOT NULL,
  servicio_id INT UNSIGNED NOT NULL,
  PRIMARY KEY (campana_id, servicio_id),
  CONSTRAINT fk_campserv_campana FOREIGN KEY (campana_id)
    REFERENCES campana (id) ON DELETE CASCADE,
  CONSTRAINT fk_campserv_servicio FOREIGN KEY (servicio_id)
    REFERENCES servicio (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS campana_especie (
  campana_id INT UNSIGNED NOT NULL,
  especie_id INT UNSIGNED NOT NULL,
  PRIMARY KEY (campana_id, especie_id),
  CONSTRAINT fk_campesp_campana FOREIGN KEY (campana_id)
    REFERENCES campana (id) ON DELETE CASCADE,
  CONSTRAINT fk_campesp_especie FOREIGN KEY (especie_id)
    REFERENCES especie (id) ON DELETE CASCADE
) ENGINE=InnoDB;
