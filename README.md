# APPMOVIL — Sistema de Gestión de Campo y Despacho de Satélites

> Plataforma **Field Service Management (FSM)** para gestionar, despachar y monitorear en tiempo real al personal de campo ("Satélites"): órdenes de servicio (SOT), GPS en vivo, asistencia, chat interno y reportes Excel.

**Versión:** 1.0 (`versionCode: 4`)

📘 **Empieza aquí:** [Guía completa de instalación y funcionamiento](./GUIA_INSTALACION_Y_FUNCIONAMIENTO.md) — explica qué hace el sistema y cómo instalar BD, API, dashboard y app paso a paso.
📖 Referencia técnica: [DOCUMENTACION_TECNICA.md](./DOCUMENTACION_TECNICA.md) · 🗄️ Esquema listo para importar: [database/schema.sql](./database/schema.sql)

---

## ¿Qué hace el sistema?

Un supervisor crea una **Solicitud de Servicio (SOT)** indicando técnico, motivo, materiales, coordenadas y zona. La solicitud queda pendiente y los satélites de esa zona la ven en su app (polling cada 5 s), la aceptan, reportan "en camino" / "llegado", suben fotos de evidencia y la finalizan. Mientras tanto, la app envía la ubicación GPS cada 10 s, registra ingresos/salidas de ruta (asistencia) y permite chat privado con supervisores y administradores.

El **dashboard web** es el centro de control corporativo: mapa con todas las ubicaciones, estado de los satélites, gestión de solicitudes y personal, asistencia, productividad, monitoreo TNPS y descarga de reportes Excel.

### Roles

| Rol | Dónde opera | Qué puede hacer |
|---|---|---|
| **Admin** | App + Dashboard | Todo: mapa GPS, asistencia, estadísticas, gestión de personal/zonas, reportes, registro de usuarios |
| **Supervisor** | App | Crear/editar SOTs, ver estado de satélites, corregir/rechazar, chat |
| **Satélite** | App | Activar ruta, aceptar y ejecutar SOTs, subir evidencia, reportar ubicación, chat, timer de almuerzo |

---

## Componentes del repositorio

| Carpeta | Tecnología | Descripción |
|---|---|---|
| [`app/`](./app/) | Kotlin · Jetpack Compose · Material 3 | App Android (móvil) para satélites, supervisores y admins |
| [`dashboard/`](./dashboard/) | React 19 · Vite 8 · Tailwind 4 · Leaflet | Panel web de administración |
| [`api/`](./api/) | PHP 8 · MySQL | 68 scripts de API REST, migraciones, reportes y utilidades |
| [`database/`](./database/) | SQL | `schema.sql` con el DDL completo de las 8 tablas y las zonas iniciales |
| `GUIA_INSTALACION_Y_FUNCIONAMIENTO.md` | Markdown | Guía práctica: qué hace el sistema e instalación paso a paso |
| `DOCUMENTACION_TECNICA.md` | Markdown | Especificación completa: endpoints, BD, flujos, despliegue y seguridad |
| `PRESENTACION_APPMOVIL.doc` | Word | Presentación del producto |

### Arquitectura

```
   App Android (Retrofit)  ──┐
                            ├── HTTPS ──> API REST (PHP) ──> MySQL
   Dashboard Web (Axios)  ──┘                        └──> Fotos / APKs
```

- **Servidor de producción:** `https://www.cicsamovil.com` (Hostinger)
- **API:** `https://www.cicsamovil.com/api/` · **Zona horaria:** UTC-5
- **Base URL del móvil:** `app/.../data/remote/RetrofitClient.kt` (`https://www.cicsamovil.com/`)
- **Base URL del dashboard:** `dashboard/src/services/api.js` (`https://www.cicsamovil.com/api`)

---

## Funcionalidades principales

**App Android**
- Login persistente, registro y perfil de usuario.
- Servicio foreground de **GPS** (cada 10 s) y de **polling** de solicitudes/mensajes (cada 5 s) con notificaciones.
- Flujo de SOT: `pendiente → aceptada → en_camino → llegado → finalizada` (con corregida/rechazada).
- Evidencia fotográfica múltiple, comentarios y chat privado.
- Asistencia con eventos `INGRESO`/`SALIDA` al activar/cerrar ruta; timer de almuerzo (máx. 1 h) y cierre automático a las 23:00.
- Pantallas de admin: mapa Google Maps en vivo, asistencia, estadísticas y exportación de reportes.
- **Actualización in-app (OTA):** consulta `check_update.php`, descarga el APK y lo instala con `FileProvider`.

**Dashboard Web** (14 pantallas en `dashboard/src/pages/`)
- `Overview` (KPIs), `Map` (Leaflet), `Solicitudes`, `SatelitesStatus`, `Asistencia`, `Users`, `Reports`, `SupervisionReports`, `SupervisorProductivity`, `TNPSMonitoring`, `AuxiliaresSolicitudes`, `AuxiliaresStatus`.
- Caché en `localStorage` con refresco silencioso cada 15 s (comparación profunda para evitar parpadeos).
- **Solo usuarios con rol exacto `admin` pueden entrar** (validado en `dashboard/src/services/api.js`).

**API PHP** (68 archivos en `api/`)
- Autenticación y usuarios: `login.php`, `register.php`, `change_password.php`, `admin_get_all_users.php`, `update_user_zone.php`, `bulk_import_tecnicos.php`, `importar_satelites.php`.
- Estado y GPS: `update_status.php`, `get_satellites_status.php`, `update_location.php`, `get_users_locations.php`, `reset_lunch_status.php`.
- Solicitudes: `get_solicitudes.php`, `create_solicitud.php`, `update_solicitud_status.php`, `upload_evidencia.php`, `delete_evidencia.php`.
- Supervisiones/TNPS: `start_supervision.php`, `submit_supervision.php`, `update_supervision_progress.php`, `cancel_supervision.php`, `get_all_supervisiones.php`, `validate_tnps_evidencia.php`, `upload_tnps_evidencia.php`.
- Chat: `get_contacts.php` y `mark_messages_read.php` están presentes. ⚠️ La app referencia `get_private_messages.php` y `send_private_message.php` (`AuthApi.kt`), pero **esos dos archivos no se encuentran en `api/`**; la documentación técnica los lista, así que revisar si faltan en el despliegue.
- Asistencia, zonas y reportes: `attendance.php`, `get_attendance.php`, `get_zones.php`, `export_asistencias_mensual.php`, `export_horarios_ruta.php`, `export_solicitudes.php`, `export_supervisiones.php`.
- Sistema y migraciones: `check_update.php`, `get_tops_performance.php`, `db_config.php`, `db_migration*.php`, `*_schema.php`, `debug_*.php`, `test_db*.php`.

---

## Cómo ejecutarlo

> Este repo incluye `node_modules/`, `dashboard/dist/` y los builds de Android, así que no hace falta instalar dependencias para ver/probar el dashboard ya compilado.

### 1. Backend (PHP + MySQL)
1. Copiar `api/` a `public_html/api/` del hosting.
2. Editar `api/db_config.php` con las credenciales reales (viene con placeholders `TU_USUARIO_DB`, etc.).
3. Ejecutar en el navegador `asistencias_schema.php`, `admin_update_schema.php` y los `db_migration*.php` que correspondan.
4. Verificar SSL y CORS (ya habilitado en `db_config.php`).

### 2. Dashboard Web
```bash
cd dashboard
npm install          # solo si no se usa el node_modules incluido
npm run dev          # desarrollo → http://localhost:5173
npm run build        # producción → dashboard/dist/
npm run preview      # serve del build
npm run lint
```
Antes de buildear, revisar `API_BASE_URL` en `dashboard/src/services/api.js`.

### 3. App Android
1. Abrir la carpeta raíz en Android Studio (SDK 36, minSdk 24, Java 11).
2. Verificar `BASE_URL` en `RetrofitClient.kt` y la API Key de Maps en `AndroidManifest.xml`.
3. `./gradlew :app:assembleDebug` → APK en `app/build/outputs/apk/debug/app-debug.apk`.
   - En Windows: `gradlew.bat :app:assembleDebug`.

### Prueba local realizada (solo entorno de desarrollo)
Se levantó una copia temporal de la API apuntando a la BD local `claro_ultima_milla` y se validó el login del dashboard con un usuario **admin de prueba** (`987654321` / `Admin123`, id 53). Ese usuario **no está en producción**; fue creado únicamente en la BD local para verificar el flujo. El usuario principal de la app es `Admin Líder` (`999999999` / `123456`, rol `supervisor_lider`).

---

## Configuración crítica

| Archivo | Qué controla |
|---|---|
| `api/db_config.php` | Credenciales MySQL, CORS y zona horaria |
| `api/check_update.php` | Versión del APK para actualizaciones OTA |
| `app/.../data/remote/RetrofitClient.kt` | URL base de la API en la app |
| `dashboard/src/services/api.js` | URL base de la API en el dashboard y validación de rol `admin` |
| `app/build.gradle.kts` | `versionCode` / `versionName` |
| `app/src/main/AndroidManifest.xml` | Permisos, servicios foreground y API Key de Google Maps |

---

## Estado y consideraciones de seguridad

- ✅ Compilación Android y del dashboard verificadas; 68 scripts PHP pasan `php -l` sin errores.
- ⚠️ **API Key de Google Maps hardcodeada** en `AndroidManifest.xml` línea 65. Al estar este repo en GitHub público, conviene **rotarla** y restringirla por SHA-1 + paquete.
- ⚠️ `local.properties`, `node_modules/`, `build/` y `dist/` están incluidos en el repo a petición expresa; contienen rutas locales de Windows.
- ⚠️ CORS abierto (`*`) y autenticación basada en ID de usuario (sin JWT). Ver el capítulo 8 de la documentación técnica para las recomendaciones de producción.
- ⚠️ Los endpoints de chat `get_private_messages.php` y `send_private_message.php` son llamados por la app pero no están en `api/`.
- ℹ️ `backup_diseno_primero/` contiene respaldos `.bak` de pantallas de un diseño anterior (no se usan en la compilación actual).

---

## Resumen de verificación

| Prueba | Resultado |
|---|---|
| `gradlew :app:assembleDebug` | ✅ BUILD SUCCESSFUL |
| Instalación y arranque en emulador `Medium_Phone_API_36.1` | ✅ sin crashes |
| Login en app con datos ficticios | ⚠️ timeout al no estar accesible `cicsamovil.com` desde ese entorno |
| `npm run build` (dashboard) | ✅ OK (warning de chunk > 500 kB) |
| `php -l` en 68 archivos | ✅ 0 errores |
| Login del dashboard contra API local + `admin_get_all_users.php` | ✅ rol `admin`, 29 usuarios |

Para el detalle completo de endpoints, esquema de BD, diagramas y despliegue, ver **[DOCUMENTACION_TECNICA.md](./DOCUMENTACION_TECNICA.md)**.
