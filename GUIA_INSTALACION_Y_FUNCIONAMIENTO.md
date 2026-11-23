# Guía completa de instalación y funcionamiento — APPMOVIL / APP-CICSA

> Documentación práctica para **entender qué hace el sistema**, **instalarlo desde cero** (base de datos, API PHP, dashboard web y app Android) y **verificar que funciona**.
>
> complementaria a [README.md](./README.md) (resumen) y [DOCUMENTACION_TECNICA.md](./DOCUMENTACION_TECNICA.md) (referencia técnica de endpoints y BD).

---

## Índice

1. [Qué es este proyecto](#1-qué-es-este-proyecto)
2. [Cómo funciona internamente](#2-cómo-funciona-internamente)
3. [Requisitos previos](#3-requisitos-previos)
4. [Instalación paso a paso](#4-instalación-paso-a-paso)
   - [4.1 Base de datos](#41-base-de-datos-mariadb--mysql)
   - [4.2 Backend PHP](#42-backend-php)
   - [4.3 Dashboard web](#43-dashboard-web)
   - [4.4 App Android](#44-app-android)
   - [4.5 Verificación final](#45-verificación-final-checklist)
5. [Archivos de configuración crítica](#5-archivos-de-configuración-crítica)
6. [Usuarios de prueba](#6-usuarios-de-prueba)
7. [Solución de problemas](#7-solución-de-problemas)
8. [Seguridad y producción](#8-seguridad-y-producción)
9. [Mantenimiento](#9-mantenimiento)

---

## 1. Qué es este proyecto

**APPMOVIL** es una plataforma de **gestión de campo (Field Service Management)** para una empresa de telecomunicaciones. Sirve para **despachar, monitorear y auditar** al personal que sale a trabajar a la calle ("satélites", "técnicos" y "auxiliares").

En palabras simples:

> Un supervisor crea una orden de trabajo (SOT) en su celular → el sistema la envía a los satélites de la zona correspondiente → el satélite la acepta, viaja, sube fotos como evidencia y la cierra → todo eso queda registrado con hora, ubicación GPS y fotos, y se puede ver en tiempo real desde el dashboard web y exportar a Excel.

### 1.1 Los tres componentes

| Componente | Tecnología | Para quién es | Qué hace |
|---|---|---|---|
| **App Android** (`app/`) | Kotlin + Jetpack Compose + Material 3 | Satélites, supervisores, técnicos, auxiliares y admins | Crear/ejecutar SOTs, GPS, asistencia, chat, evidencias, supervisiones y reportes |
| **Dashboard Web** (`dashboard/`) | React 19 + Vite 8 + Tailwind 4 + Leaflet | Solo administradores | Centro de control: mapa, SOTs, personal, estado de satélites, asistencia, auditoría y reportes |
| **API PHP** (`api/`) | PHP 8 + MySQL/MariaDB | Backend (no visible) | 68 scripts REST que guardan y devuelven todos los datos |

Todos se comunican por **HTTPS** contra `https://www.cicsamovil.com` (Hostinger). La base de datos usa zona horaria **UTC-5 (Perú)**.

### 1.2 Roles y permisos

| Rol | App | Dashboard | Funciones principales |
|---|:---:|:---:|---|
| `admin` | ✅ | ✅ | Todo: mapa en vivo, asistencia, estadísticas, gestión de personal/zonas, TNPS, reportes, registro de usuarios |
| `supervisor` | ✅ | ❌ | Crear/editar SOTs, asignar satélites, ver estado del equipo, corregir/rechazar, supervisar técnicos, chat |
| `supervisor_lider` | ✅ | ❌ | Se trata como supervisor en la app (no tiene acceso al dashboard) |
| `satelite` | ✅ | ❌ | Activar ruta, recibir y ejecutar SOTs, subir fotos, reportar ubicación, almuerzo, chat |
| `auxiliar` | ✅ | ❌ | Igual que satélite pero con tareas tipo "auxiliar" que se validan con un supervisor |
| `tecnico` | ✅ | ❌ | Recibe supervisión del supervisor y sube evidencia TNPS |

> ⚠️ **El dashboard exige que el rol sea exactamente `admin`** (minúsculas). Otros roles, aunque sean administrativos, son rechazados en `dashboard/src/services/api.js:19`.

### 1.3 Funcionalidades por módulo

**Órdenes de trabajo (SOT)**
- Creación con número SOT, técnico, motivo, materiales, coordenadas/link de Maps y zona.
- Filtrado automático por zona: solo los satélites de esa zona reciben la tarea.
- Asignación manual, reasignación, corrección con comentario y rechazo.
- Evidencia fotográfica múltiple (se sube a `api/uploads/evidencias/`).
- Seguimiento tipo Uber (`TicketTrackingScreen`): buscando satélite → en camino → llegado, con mapa y barra de progreso (polling cada 4 s).

**GPS y notificaciones**
- `LocationService`:/envía lat/lng cada **10 s** con `PRIORITY_HIGH_ACCURACY`.
- `SolicitudPollingService`: consulta nuevas SOTs, mensajes y tickets aceptados cada **5 s** y muestra notificaciones con el sonido `alerta_mision.mp3`.
- Ambos son **foreground services** (notificación persistente) con WakeLock para que el celular no los duerma.

**Asistencia**
- Al activar ruta se registra `INGRESO`; al cerrarla, `SALIDA` (con lat/lng).
- Timer de almuerzo de máximo 1 hora con alerta a 5 minutos del final; solo se puede usar una vez por día.
- Cierre automático de sesión/ruta a las **23:00** para satélites.

**Supervisión de campo y TNPS**
- El supervisor elige un técnico, inicia supervisión, completa checklist con puntaje, comentarios y fotos; `submit_supervision.php` mueve las fotos temporales a `api/uploads/supervisiones/` y libera al técnico.
- TNPS: evidencias de encuestas de satisfacción subidas por el técnico y validadas luego por supervisor/admin (`api/uploads/tnps/`).

**Chat privado**
- Contactos con contador de mensajes no leidos, historial y envío; polling cada 5 s.

**Actualización in-app (OTA)**
- Al abrir la app se consulta `check_update.php`; si `versionCode` del servidor es mayor que el de la app, se ofrece descargar el APK desde `apkUrl` e instalarlo con `FileProvider`.

**Dashboard (11 rutas activas)**
`#/dashboard/` (KPIs), `/map`, `/users`, `/solicitudes`, `/auxiliares`, `/satelites`, `/estado-auxiliares`, `/asistencia`, `/supervision`, `/tnps`, `/reports`.
Caché en `localStorage` (`cache_solicitudes`, `cache_users`) con refresco silencioso cada **15 s**.

---

## 2. Cómo funciona internamente

### 2.1 Arquitectura

```
  App Android ──(Retrofit / GSON)──┐
                                  ├── HTTPS ──> API REST (PHP 8) ──> MariaDB/MySQL
  Dashboard Web ──(Axios)─────────┘                          ├──> uploads/ (fotos)
                                                             └──> APK (OTA)

  Base URL móvil : https://www.cicsamovil.com/          (RetrofitClient.kt:9)
  Base URL web   : https://www.cicsamovil.com/api       (api.js:3)
```

### 2.2 Ciclo de vida de una SOT (flujo real según el código)

| Estado | Quién lo dispara | Qué pasa |
|---|---|---|
| `pendiente` | Supervisor/auxiliar al crear | Se guarda en `solicitudes` con `fecha_creacion = NOW()`. Se cuentan satélites de la zona y disponibles. |
| `aceptada` | Satélite acepta | Se asigna `id_satelite` + `nombre_satelite`, se limpia comentario y se graba `fecha_aceptacion`. |
| `en_camino` | Satélite | El supervisor recibe alerta sonora (`check_accepted_tickets.php`, `sinceSeconds=15`). |
| `llegado` | Satélite | Se muestra en tracking. |
| `completada` | Satélite | Graba `fecha_finalizacion = NOW()`. |
| `corregir` | Supervisor | Guarda `comentario_satelite`; al volver a `pendiente` se limpian satélite y fechas. |
| `rechazada` | Supervisor | Termina el flujo con comentario. |
| `por_validar` / `auxiliar` | Auxiliares | La tarea va a un supervisor para validación. |

### 2.3 Base de datos (tablas)

| Tabla | Para qué sirve |
|---|---|
| `usuarios` | Personas, rol, zona, estado, GPS, sesión y progreso temporal de supervisión |
| `solicitudes` | Bolsa de tareas SOT (estado, fotos, fechas, satélite asignado) |
| `asistencias` | Eventos INGRESO/SALIDA con lat/lng |
| `mensajes` | Chat privado (emisor, receptor, texto, fecha, leído) |
| `zonas` | 8 zonas por defecto (SURCO, LURIN, ATE, etc.) |
| `supervisor_tecnicos` | Relación supervisor ↔ técnicos |
| `supervisiones` | Checklists, puntaje, fotos y coordenadas de auditorías |
| `tnps_evidencia` | Fotos/comentarios de TNPS con estado de validación |

El repo **no incluía archivos `.sql`**; se agregó [`database/schema.sql`](./database/schema.sql) con el DDL completo y las 8 zonas. Está probado en MariaDB 10.4.32.

### 2.4 Mapa de endpoints (los que más se usan)

| Módulo | Endpoints |
|---|---|
| Login / usuarios | `login.php`, `register.php`, `get_user_info.php`, `update_profile.php`, `change_password.php`, `admin_get_all_users.php`, `update_user_zone.php` |
| Solicitudes | `get_solicitudes.php`, `create_solicitud.php`, `update_solicitud_data.php`, `update_solicitud_status.php`, `upload_evidencia.php`, `delete_evidencia.php`, `check_accepted_tickets.php`, `get_ticket_tracking.php` |
| GPS / estado | `update_location.php`, `get_users_locations.php`, `update_status.php`, `get_satellites_status.php`, `reset_lunch_status.php` |
| Asistencia | `attendance.php`, `get_attendance.php`, `export_asistencias_mensual.php`, `export_horarios_ruta.php` |
| Chat | `get_contacts.php`, `mark_messages_read.php`, ⚠️ `get_private_messages.php` y `send_private_message.php` (ver §7) |
| Supervisión | `start_supervision.php`, `update_supervision_progress.php`, `upload_supervision_temp_photo.php`, `submit_supervision.php`, `get_all_supervisiones.php`, `export_supervisiones.php` |
| TNPS | `upload_tnps_evidencia.php`, `get_tnps_evidencias.php`, `validate_tnps_evidencia.php` |
| Reportes / sistema | `export_solicitudes.php`, `get_tops_performance.php`, `get_supervisor_productivity.php`, `check_update.php` |

La lista completa de los 68 archivos está en la [documentación técnica](./DOCUMENTACION_TECNICA.md#53-catálogo-completo-de-endpoints).

---

## 3. Requisitos previos

### 3.1 Versiones del entorno donde se compiló y probó

| Herramienta | Versión verificada |
|---|---|
| Node.js | **v22.17.1** |
| npm | **10.9.2** |
| PHP (CLI) | **8.2.12** (extensión `mysqli` obligatoria; `cURL` opcional) |
| MariaDB | **10.4.32** (recomendada sobre MySQL 8, ver §7) |
| Java | JDK 11 en PATH, pero Gradle usa el **JBR de Android Studio** (`gradle.properties:30`) |
| Gradle wrapper | **9.2.1** |
| Android Gradle Plugin | **9.0.1** |
| Kotlin | **2.0.21** |
| Android SDK | Platform **36** (release 36.1), `minSdk 24` |
| Emulador usado en pruebas | `Medium_Phone_API_36.1` |

### 3.2 Qué hay que instalar

1. **Android Studio** (con SDK Platform 36 y emulador, o un celular físico con depuración USB).
2. **Node.js 20+ y npm** (para el dashboard).
3. **PHP 8** + **MariaDB 10.4+** (o XAMPP/WAMP que incluya ambos). En producción: hosting con PHP y MySQL (Hostinger en el proyecto original).
4. **Git** (el repo ya está inicializado; `git config core.longpaths true` si aparece "Filename too long").
5. Opcional para pruebas locales: `adb` (viene con Android Studio) y un navegador.

---

## 4. Instalación paso a paso

### 4.1 Base de datos (MariaDB / MySQL)

**1. Crear la BD e importar el esquema**

El nombre usado en el entorno de prueba es `claro_ultima_milla`; cámbialo si quieres.

```bash
mysql -u root -p < database/schema.sql
```

O en phpMyAdmin: crear la BD con collation `utf8mb4_unicode_ci` → pestaña **Importar** → elegir `database/schema.sql`.

El script crea las 8 tablas y carga las zonas. Verificado: `asistencias`, `mensajes`, `solicitudes`, `supervisiones`, `supervisor_tecnicos`, `tnps_evidencia`, `usuarios`, `zonas`.

**2. Crear un usuario de BD para la app (opcional si usas root en local)**

```sql
CREATE USER 'cicsa_user'@'localhost' IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP ON claro_ultima_milla.* TO 'cicsa_user'@'localhost';
FLUSH PRIVILEGES;
```

> Los permisos `CREATE`/`ALTER` son necesarios porque **varios endpoints crean/parchean tablas en tiempo de ejecución** (`login.php` agrega `session_token`, `start_supervision.php` agrega columnas, etc.).

**3. Crear el usuario administrador inicial**

La contraseña debe guardarse con `password_hash()` de PHP (bcrypt):

```bash
php -r "echo password_hash('Admin123', PASSWORD_BCRYPT);"
```

Copia el hash resultante (`$2y$10$...`) y ejecuta:

```sql
INSERT INTO usuarios (nombre, telefono, email, password, rol, zona, estado, disponible)
VALUES ('Admin', '987654321', 'admin@ejemplo.com',
        '<PEGA_AQUI_EL_HASH>', 'admin', 'LIMA', 'Disponible', 1);
```

El **teléfono es el usuario de login**. Para que entre al dashboard el rol debe ser exactamente `admin`.

**4. Ejecutar los scripts de migración (si importaste `schema.sql`, la mayoría no hará nada)**

Visitar en el navegador, en este orden:

| # | Script | Qué asegura |
|---|---|---|
| 1 | `asistencias_schema.php` | Tabla `asistencias` + columnas base de `usuarios` |
| 2 | `admin_update_schema.php` | `fecha_aceptacion`, `fecha_finalizacion`, geolocalización |
| 3 | `db_migration_auxiliar.php` | `solicitudes.tipo` |
| 4 | `migrar_comentarios.php` | `solicitudes.comentario_satelite` |
| 5 | `db_migration.php` | `usuarios.almuerzo_usado` |
| 6 | `setup_zones.php` | Tabla `zonas` + 8 filas |
| 7 | `db_migration_supervision.php` | `supervisor_tecnicos` y `supervisiones` |
| 8 | `db_migration_supervision_progress.php`, `db_migration_sot_supervision.php` | Columnas de progreso de supervisión |
| 9 | `db_migration_tnps.php` | Tabla `tnps_evidencia` |

> Si usaste `database/schema.sql`, puedes ir directo a la verificación: `test_db.php`, `check_schema.php`, `debug_db.php`.

### 4.2 Backend PHP

**1. Copiar los archivos**

- En producción: subir todo `api/` a `public_html/api/`.
- En local: dejar la carpeta `api/` donde está y levantar el servidor embebido:
  ```bash
  php -S 127.0.0.1:8099 -t api
  ```

**2. Configurar `api/db_config.php` (líneas 14-17)**

```php
$host   = 'localhost';            // o 127.0.0.1
$user   = 'cicsa_user';           // o root en local
$pass   = 'CAMBIA_ESTA_CLAVE';    // o '' en local
$dbname = 'claro_ultima_milla';
```

Ese mismo archivo ya configura CORS (`*`), charset `utf8mb4` y `SET time_zone = '-05:00'`.

**3. Permisos de subida de fotos**

El usuario del servidor web debe poder escribir en `api/`. Las carpetas se crean solas:

| Carpeta | Creada por | Uso |
|---|---|---|
| `api/uploads/evidencias/` | `upload_evidencia.php` | Fotos de SOT |
| `api/uploads/supervisiones_temp/` | `upload_supervision_temp_photo.php` | Fotos temporales |
| `api/uploads/supervisiones/` | `submit_supervision.php` | Fotos finales de auditoría |
| `api/uploads/tnps/` | `upload_tnps_evidencia.php` | Evidencia TNPS |

En `php.ini`: `file_uploads = On`, `upload_max_filesize` y `post_max_size` suficientes, `log_errors = On` (para `debug_supervision.log`).

**4. Configurar la versión del APK (OTA) en `api/check_update.php`**

```php
$updateInfo = [
    "versionCode" => 4,
    "versionName" => "1.0",
    "apkUrl" => "https://www.cicsamovil.com/api/app-debug.apk",
    "notes" => "..."
];
```

Sube el APK a `public_html/api/app-debug.apk` y aumenta `versionCode` **por encima de 4** para que la app lo detecte (actualmente ambos son 4, así que no salta actualización).

**5. Revisar URLs hardcodeadas al cambiar de dominio**

| Archivo | Línea | Valor a cambiar |
|---|---|---|
| `api/upload_tnps_evidencia.php` | 50 | `https://www.cicsamovil.com/api/uploads/tnps/` |
| `dashboard/src/pages/TNPSMonitoring.jsx` | 53 | misma URL de fotos TNPS |
| `app/.../utils/ReportExporter.kt` | 67, 80 | URLs `http://www.cicsamovil.com/api/export_*` |
| `app/.../ui/solicitudes/SolicitudViewModel.kt` | 362 | `https://tecsolperu.com/api/uploads/supervisiones_temp/` |

### 4.3 Dashboard web

```bash
cd dashboard
npm install          # omitir si usas el node_modules incluido en el repo
```

**Desarrollo**

1. Editar `dashboard/src/services/api.js:3` si la API no es la de producción:
   ```js
   const API_BASE_URL = 'http://127.0.0.1:8099'; // prueba local
   ```
2. `npm run dev` → http://localhost:5173 (o el puerto que indique Vite).
3. Entrar con un usuario `admin`.

**Producción**

```bash
npm run build     # genera dashboard/dist/
npm run preview   # para probar el build localmente
```

Copiar el contenido de `dashboard/dist/` a `public_html/` del servidor web. `vite.config.js` usa `base: './'`, así que funciona en subcarpetas. Antes del build, **dejar `API_BASE_URL` apuntando a producción**.

> `npm run lint` ejecuta ESLint. El build actual avisa de un chunk > 500 kB (no es un error).

### 4.4 App Android

**1. Abrir el proyecto**

Abrir la **carpeta raíz** (`APPMOVIL/`) en Android Studio. El wrapper bajará Gradle 9.2.1. Verificar:

- `local.properties:10` → `sdk.dir=C:\Users\laptop\AppData\Local\Android\Sdk` (ajusta a tu ruta).
- `gradle.properties:30` → `org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr` (ajusta o elimina para usar tu JDK).
- Instalar SDK Platform **36** y aceptar licencias (`sdkmanager --licenses`).

**2. Configurar la URL de la API y la API Key**

- `app/src/main/java/com/example/appmovil/data/remote/RetrofitClient.kt:9`:
  ```kotlin
  const val BASE_URL = "https://www.cicsamovil.com/"
  ```
  (para local: `http://10.0.2.2:8099/` desde emulador; `http://<IP-PC>:8099/` desde celular físico).
- `app/src/main/AndroidManifest.xml:65`: reemplazar la API Key de Google Maps por una propia con **Maps SDK for Android** habilitado.

**3. Permisos declarados en el manifest**

`INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `REQUEST_INSTALL_PACKAGES` y `CAMERA`.

La app **pide permisos en runtime** (ubicación y notificaciones en Android 13+) y **se bloquea si detecta Opciones de Desarrollador activadas** (`DeveloperOptionsChecker.kt`) — úsala en un emulador o celular sin ese modo.

**4. Compilar e instalar**

```bash
gradlew.bat :app:assembleDebug                       # Windows
./gradlew :app:assembleDebug                         # Linux/macOS
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

APK resultante: `app/build/outputs/apk/debug/app-debug.apk` (`applicationId` `com.example.appmovil`, `versionCode 4`, `minSdk 24`).

**5. Navegación de la app (rutas Compose)**

`login` → `register` → `home/{userId}/{userName}/{userRol}/{userZona}` → `create_solicitud/{userName}/{userRol}` → `ticket_tracking/{solicitudId}/{sot}/{tecnico}/{isAlreadyAccepted}`.

Si hay sesión guardada en SharedPreferences `user_session`, se entra directo a `home`.

### 4.5 Verificación final (checklist)

| Prueba | Cómo verificarlo | Resultado esperado |
|---|---|---|
| BD | Visitar `test_db.php` y `debug_db.php` | Sin errores, muestra columnas y usuarios |
| Login API | `POST /login.php` con `{"celular":"987654321","password":"Admin123"}` | `status: success`, `rol: admin`, `sessionToken` |
| Dashboard | Abrir la URL y entrar con el admin | Carga el centro de control y el mapa |
| Listado | `GET /admin_get_all_users.php` | JSON con usuarios |
| App | Abrir en emulador, iniciar sesión | Pantalla home sin crashes |
| GPS | Activar ruta como satélite | Filas nuevas en `historial_ubicaciones` / `logs_gps` y `asistencias` |
| Fotos | Subir evidencia | Archivos en `api/uploads/evidencias/` |
| Build | `gradlew :app:assembleDebug` y `npm run build` | BUILD SUCCESSFUL / dist generado |

---

## 5. Archivos de configuración crítica

| Archivo | Qué controla |
|---|---|
| `database/schema.sql` | Creación de BD y tablas (nuevo) |
| `api/db_config.php` | Credenciales MySQL, CORS, charset y zona horaria |
| `api/check_update.php` | Versión y URL del APK para OTA |
| `app/.../data/remote/RetrofitClient.kt` | URL base de la API en la app |
| `dashboard/src/services/api.js` | URL base de la API y validación de rol `admin` |
| `app/build.gradle.kts` | `applicationId`, `versionCode`, `versionName`, SDK |
| `app/src/main/AndroidManifest.xml` | Permisos, servicios, FileProvider y API Key de Maps |
| `local.properties` / `gradle.properties` | Ruta del SDK de Android y del JDK de Gradle |
| `dashboard/vite.config.js` | `base: './'` y plugins de Vite/Tailwind |

---

## 6. Usuarios de prueba

> Estos usuarios existen **solo en la base de datos local de desarrollo**. No asumas que están en producción.

| Uso | Usuario (teléfono) | Contraseña | Rol |
|---|---|---|---|
| App Android (principal) | `999999999` | `123456` | `supervisor_lider` |
| Dashboard (creado para probar) | `987654321` | `Admin123` | `admin` |

**Importante:** el usuario `supervisor_lider` no entra al dashboard porque el rol no es `admin`. Para el dashboard usa el segundo.

---

## 7. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Faltan datos obligatorios` al hacer login con `curl -d` | PowerShell/curl no envía el JSON como se espera | Usa `--data-binary @archivo.json` con `Content-Type: application/json` |
| `Unknown column 'zona' / 'celular_personal'` | BD desactualizada | Importa `database/schema.sql` o ejecuta las migraciones del §4.1 |
| `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` falla | MySQL 5.7/8.0 no soporta esa sintaxis | Usa **MariaDB 10.4+** o ejecuta los `ALTER` manualmente con `information_schema` |
| `check_users_schema.php` da error de include | Línea 2 dice `include 'api/db_config.php'` | Cámbialo a `include 'db_config.php'` |
| El dashboard dice "Acceso denegado" | El rol no es exactamente `admin` | Crea/cambia el usuario a rol `admin` |
| No llegan fotos TNPS | URL hardcodeada a `cicsamovil.com` | Ajusta `upload_tnps_evidencia.php:50` y `TNPSMonitoring.jsx:53` |
| Chat no funciona | Faltan `get_private_messages.php` y `send_private_message.php` en `api/` (la app sí los llama) | Crear esos endpoints sobre la tabla `mensajes`; verificar con `get_contacts.php` |
| App bloqueada al abrir | Opciones de Desarrollador activadas | Desactívalas o usa un emulador |
| `SocketTimeoutException` / error TLS `SEC_E_INTERNAL_ERROR` contra `cicsamovil.com` | El sitio no es accesible desde esa red o falla el TLS de Windows | Probar con API local (`php -S`) y `BASE_URL` apuntando a `10.0.2.2`/IP local |
| `git: Filename too long` | Rutas largas de `node_modules`/build en Windows | `git config core.longpaths true` |
| La app no muestra actualización OTA | `versionCode` del servidor (4) = el de la app (4) | Subir `versionCode` en `check_update.php` y dejar el APK accesible |
| `Reports.jsx` siempre descarga asistencia | `handleDownload` solo llama a `exportAttendance` | Usar directamente las URLs de `api.js` para SOT/horarios |

---

## 8. Seguridad y producción

Estado actual y recomendaciones:

- ⚠️ **API Key de Google Maps hardcodeada** en `AndroidManifest.xml:65` y expuesta en un repo público → **rotarla** y restringirla por SHA-1 + paquete `com.example.appmovil`.
- ⚠️ **CORS abierto** (`Access-Control-Allow-Origin: *`) → limitarlo al dominio del dashboard.
- ⚠️ **Autenticación por ID de usuario** sin JWT; hay un `session_token` de login único en `usuarios`, pero conviene tokens firmados.
- ⚠️ **Contraseñas**: verificar que todo usuario use `password_hash()` (bcrypt), nunca texto plano.
- ⚠️ **Subida de archivos**: validar tipo MIME y tamaño máximo; sanitizar nombres.
- ⚠️ `debug_tnps.php` activa `display_errors` → quitarlo de producción.
- ✅ Agregar HTTPS válido, backups diarios de MySQL, logs de auditoría y rate limiting en `login.php`.
- ✅ Activar R8/ProGuard (`isMinifyEnabled = false` actualmente) en el release.

---

## 9. Mantenimiento

| Frecuencia | Tarea |
|---|---|
| Diaria | Revisar que los foreground services sigan activos y que lleguen ubicaciones |
| Semanal | Revisar logs PHP (`error_log`, `debug_supervision.log`) |
| Mensual | Exportar y archivar reportes de asistencia; limpiar fotos antiguas de `uploads/` |
| Trimestral | Actualizar dependencias npm (`npm audit`) y Gradle; revisar el `versionCode` del OTA |

**Archivos temporales del repo**: `backup_diseno_primero/` tiene respaldos `.bak` de un diseño anterior; `api/extracted_sat/` y `api/satelites_temp/` son restos de importación de un XLSX. No afectan la compilación.

---

## 10. Referencias

- [README.md](./README.md) — resumen rápido del proyecto.
- [DOCUMENTACION_TECNICA.md](./DOCUMENTACION_TECNICA.md) — 666 líneas con endpoints, esquemas, diagramas y checklist de despliegue.
- [database/schema.sql](./database/schema.sql) — DDL completo probado en MariaDB 10.4.32.
- Repositorio: https://github.com/bramirezv/APP-CICSA
