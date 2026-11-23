-- =====================================================================
-- APPMOVIL / APP-CICSA — Esquema completo de base de datos (instalación desde cero)
-- =====================================================================
-- Compatible con MariaDB 10.4+ y MySQL 8.0+
-- Charset: utf8mb4 / Collation: utf8mb4_unicode_ci
-- Zona horaria de la aplicación: UTC-5 (se aplica en api/db_config.php)
--
-- USO:
--   1) Cambia el nombre de la BD si quieres (línea 12).
--   2) mysql -u root -p < database/schema.sql
--      (o pega este archivo en phpMyAdmin → Importar)
--   3) Pon el mismo nombre de BD, usuario y password en api/db_config.php.
--   4) Crea el usuario admin con el INSERT de la línea ~180 (o desderegister.php).
--
-- NOTA: El repositorio no trae scripts de creación para las tablas base.
--       Este archivo las crea todas con todas las columnas que el código usa,
--       de modo que los scripts api/db_migration*.php no tengan nada que agregar.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS claro_ultima_milla
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE claro_ultima_milla;

-- ---------------------------------------------------------------------
-- 1. USUARIOS (satélites, supervisores, auxiliares y admins)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
  id INT AUTO_INCREMENT PRIMARY KEY,

  -- Datos de acceso
  nombre          VARCHAR(100) NOT NULL,
  telefono        VARCHAR(20)  NOT NULL COMMENT 'Usado como usuario de login',
  email           VARCHAR(150) NULL,
  password        VARCHAR(255) NOT NULL COMMENT 'password_hash() / bcrypt',
  rol             VARCHAR(30)  NOT NULL DEFAULT 'satelite' COMMENT 'admin, supervisor, supervisor_lider, satelite, auxiliar',
  zona            VARCHAR(50)  NULL,
  estado          VARCHAR(20)  NOT NULL DEFAULT 'No Disponible' COMMENT 'Disponible, Ocupado, Almorzando, No Disponible',
  session_token   VARCHAR(255) NULL COMMENT 'Login único: se setea en login.php',

  -- Datos personales / operativos
  dni                  VARCHAR(20)  NULL,
  puesto               VARCHAR(50)  NULL,
  licencia             VARCHAR(50)  NULL,
  celular_personal     VARCHAR(20)  NULL,
  distrito_nombre      VARCHAR(100) NULL,
  distrito_id          INT          NULL,
  direccion            VARCHAR(255) NULL,
  fecha_nacimiento     DATE         NULL,
  telefono_emergencia  VARCHAR(20)  NULL,
  foto_perfil          VARCHAR(255) NULL,
  creado_por           INT          NULL,

  -- Disponibilidad / ruta
  disponible        TINYINT(1)  NOT NULL DEFAULT 1,
  ruta_activa       TINYINT(1)  NOT NULL DEFAULT 0,
  ultima_activacion DATETIME    NULL,
  almuerzo_usado    TINYINT(1)  NOT NULL DEFAULT 0,
  en_almuerzo       TINYINT(1)  NOT NULL DEFAULT 0,
  inicio_almuerzo   DATETIME    NULL,

  -- Geolocalización
  lat              DECIMAL(10,8) NULL,
  lng              DECIMAL(11,8) NULL,
  latitud          DECIMAL(10,8) NULL,
  longitud         DECIMAL(11,8) NULL,
  ultima_ubicacion DATETIME      NULL,
  ultima_conexion  DATETIME      NULL,

  -- Progreso temporal de supervisión (start_supervision.php / submit_supervision.php)
  en_supervision                INT          NOT NULL DEFAULT 0,
  id_supervisor_actual          INT          NULL,
  inicio_supervision_actual     DATETIME     NULL,
  sot_supervision_actual        VARCHAR(50)  NULL,
  checklist_supervision_actual  TEXT         NULL,
  comentarios_supervision_actual TEXT        NULL,
  fotos_supervision_actual      TEXT         NULL,

  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_usuarios_rol (rol),
  KEY idx_usuarios_zona (zona),
  KEY idx_usuarios_telefono (telefono)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. SOLICITUDES (SOT / bolsa de tareas)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS solicitudes (
  id INT AUTO_INCREMENT PRIMARY KEY,

  sot               VARCHAR(50)  NOT NULL COMMENT 'Número de orden de trabajo',
  tecnico           VARCHAR(100) NULL,
  motivo            TEXT         NULL,
  material          TEXT         NULL,
  coordenadas       VARCHAR(255) NULL COMMENT 'Coordenadas o link de Google Maps',
  nombre_supervisor VARCHAR(100) NULL,
  zona              VARCHAR(50)  NOT NULL,
  estado            VARCHAR(30)  NOT NULL DEFAULT 'pendiente'
                    COMMENT 'pendiente, aceptada, en_camino, llegado, completada, corregir, rechazada, por_validar',
  tipo              VARCHAR(20)  NOT NULL DEFAULT 'normal' COMMENT 'normal o auxiliar',

  id_satelite         INT          NULL,
  nombre_satelite     VARCHAR(100) NULL,
  comentario_satelite TEXT         NULL,
  foto_evidencia      TEXT         NULL COMMENT 'Rutas relativas separadas por coma',

  fecha_creacion     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_aceptacion   DATETIME NULL,
  fecha_finalizacion DATETIME NULL,

  KEY idx_solicitudes_estado (estado),
  KEY idx_solicitudes_zona (zona),
  KEY idx_solicitudes_satelite (id_satelite)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. MENSAJES (chat privado)
--    ⚠️ Los endpoints get_private_messages.php y send_private_message.php
--       no están incluidos en api/; hay que crearlos (ver la guía).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mensajes (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  emisor_id   INT NOT NULL,
  receptor_id INT NOT NULL,
  mensaje     TEXT NOT NULL,
  fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  leido       TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_emisor (emisor_id),
  KEY idx_receptor (receptor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 4. ASISTENCIAS (ingreso / salida de ruta)
--    Creada también por api/asistencias_schema.php
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS asistencias (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  usuario_id INT NOT NULL,
  evento     VARCHAR(20) NOT NULL COMMENT 'INGRESO o SALIDA',
  fecha      DATE DEFAULT (CURRENT_DATE),
  hora       TIME DEFAULT (CURRENT_TIME),
  latitud    DECIMAL(10,8) NULL,
  longitud   DECIMAL(11,8) NULL,
  CONSTRAINT fk_asistencias_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuarios(id) ON DELETE CASCADE,
  KEY idx_asistencias_fecha (fecha),
  KEY idx_asistencias_usuario (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 5. ZONAS (setup_zones.php inserta las mismas 8 filas)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS zonas (
  id     INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO zonas (nombre) VALUES
  ('SURCO'), ('SAN JUAN DE MIRAFLORES'), ('VILLA EL SALVADOR'),
  ('CHORRILLOS'), ('LURIN'), ('LA MOLINA'), ('ATE'), ('SAN BORJA');

-- ---------------------------------------------------------------------
-- 6. SUPERVISIÓN DE TÉCNICOS
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS supervisor_tecnicos (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  id_supervisor    INT NOT NULL,
  id_tecnico       INT NOT NULL,
  fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_supervisor_tecnico (id_supervisor, id_tecnico)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS supervisiones (
  id INT AUTO_INCREMENT PRIMARY KEY,
  id_supervisor INT NOT NULL,
  id_tecnico    INT NOT NULL,

  -- columnas "nuevas" ( usadas por submit_supervision.php / get_all_supervisiones.php )
  sot         VARCHAR(50) NULL,
  comentarios TEXT        NULL,
  latitud     VARCHAR(50) NULL,
  longitud    VARCHAR(50) NULL,
  puntuacion  INT         NULL,
  checklist   TEXT        NULL,
  evidencia   TEXT        NULL COMMENT 'Nombres de fotos separados por coma',
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- columnas "legacy" (db_migration_supervision.php; se sincronizan en submit)
  sot_referencia VARCHAR(50) NULL,
  foto_url       TEXT        NULL,
  checklist_json TEXT        NULL,
  fecha_supervision DATETIME NULL,

  KEY idx_supervisiones_sup (id_supervisor),
  KEY idx_supervisiones_tec (id_tecnico)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 7. TNPS (evidencias de técnica/operación)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tnps_evidencia (
  id INT AUTO_INCREMENT PRIMARY KEY,
  id_tecnico       INT NULL,
  nombre_tecnico   VARCHAR(255) NULL,
  sot              VARCHAR(50) NULL,
  foto_url         TEXT NOT NULL,
  comentario       TEXT NULL,
  id_supervisor    INT NULL,
  nombre_supervisor VARCHAR(255) NULL,
  estado_validacion VARCHAR(20) NOT NULL DEFAULT 'pendiente',
  comentario_supervisor TEXT NULL,
  fecha_registro   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  latitud          DECIMAL(10,8) NULL,
  longitud         DECIMAL(11,8) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- USUARIO INICIAL
-- =====================================================================
-- La contraseña DEBE guardarse con password_hash(). Ejemplo para crear el
-- hash de 'Admin123' ( MaríaDB / MySQL 8 ):
--
--   SELECT PASSWORD('Admin123');            -- ❌ NO usar (es hash MySQL viejo)
--   php -r "echo password_hash('Admin123', PASSWORD_BCRYPT);"
--
-- Luego copias el resultado ($2y$10$...) y ejecutas:
--
--   INSERT INTO usuarios (nombre, telefono, email, password, rol, zona, estado, disponible)
--   VALUES ('Admin', '987654321', 'admin@ejemplo.com',
--           '<PEGA_AQUI_EL_HASH_BCRYPT>', 'admin', 'LIMA', 'Disponible', 1);
--
-- ⚠️ El dashboard solo acepta usuarios con rol exacto 'admin'
--    (dashboard/src/services/api.js). El teléfono es el usuario de login.
-- =====================================================================
