<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

// Habilitar reporte de errores para debugging interno (quitar en producción si se desea)
error_reporting(E_ALL);
ini_set('display_errors', 0); // No mostrar errores en el output JSON
ini_set('log_errors', 1);
ini_set('error_log', 'debug_supervision.log');

function sendResponse($status, $message, $extra = []) {
    echo json_encode(array_merge(["status" => $status, "message" => $message], $extra));
    exit;
}

// Auto-migración para la tabla de supervisiones si no existe
$sqlTable = "CREATE TABLE IF NOT EXISTS supervisiones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_supervisor INT NOT NULL,
    id_tecnico INT NOT NULL,
    sot VARCHAR(50),
    comentarios TEXT,
    latitud VARCHAR(50),
    longitud VARCHAR(50),
    puntuacion INT,
    checklist TEXT,
    evidencia TEXT, -- Guardaremos los nombres de las fotos separados por comas
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)";
if (!$conn->query($sqlTable)) {
    error_log("Error creando tabla: " . $conn->error);
}

// Reparación de esquema: Asegurar que existan las columnas que la App y get_all_supervisiones esperan
// (Esto soluciona conflictos si la tabla se creó con db_migration_supervision.php)
$conn->query("ALTER TABLE supervisiones ADD COLUMN IF NOT EXISTS sot VARCHAR(50) AFTER id_tecnico");
$conn->query("ALTER TABLE supervisiones ADD COLUMN IF NOT EXISTS evidencia TEXT");
$conn->query("ALTER TABLE supervisiones ADD COLUMN IF NOT EXISTS checklist TEXT");
$conn->query("ALTER TABLE supervisiones ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

// Si las columnas existen con otros nombres, las sincronizamos (opcional pero recomendado)
$conn->query("UPDATE supervisiones SET sot = sot_referencia WHERE sot IS NULL AND sot_referencia IS NOT NULL");
$conn->query("UPDATE supervisiones SET evidencia = foto_url WHERE evidencia IS NULL AND foto_url IS NOT NULL");
$conn->query("UPDATE supervisiones SET checklist = checklist_json WHERE checklist IS NULL AND checklist_json IS NOT NULL");

// Recibir datos (Priorizar los nombres que envía la App)
$id_supervisor = $_POST['id_supervisor'] ?? 0;
$session_token = $_POST['session_token'] ?? '';

// VALIDACIÓN DE SESIÓN ÚNICA
if ($id_supervisor > 0) {
    $stmt_session = $conn->prepare("SELECT session_token FROM usuarios WHERE id = ?");
    $stmt_session->bind_param("i", $id_supervisor);
    $stmt_session->execute();
    $res_session = $stmt_session->get_result();
    if ($row_session = $res_session->fetch_assoc()) {
        $db_token = $row_session['session_token'];
        if (!empty($db_token) && $db_token !== $session_token) {
            sendResponse("error", "Sesión inválida o iniciada en otro dispositivo. Por favor, reinicie la App.");
        }
    }
    $stmt_session->close();
}

$id_tecnico = $_POST['id_tecnico'] ?? 0;
$sot = $_POST['sot'] ?? '';
$comentarios = $_POST['comentarios'] ?? '';
$lat = $_POST['lat'] ?? $_POST['latitud'] ?? '0.0';
$lng = $_POST['lng'] ?? $_POST['longitud'] ?? '0.0';
$puntuacion = $_POST['puntuacion'] ?? 0;
$checklist = $_POST['checklist'] ?? '{}';

// Recuperar fotos previas (temporales) que se cargaron durante la sesión
$stmt_fotos = $conn->prepare("SELECT fotos_supervision_actual FROM usuarios WHERE id = ?");
$stmt_fotos->bind_param("i", $id_tecnico);
$stmt_fotos->execute();
$res_fotos = $stmt_fotos->get_result();
$fotos_temporales = [];
if ($row_fotos = $res_fotos->fetch_assoc()) {
    if (!empty($row_fotos['fotos_supervision_actual'])) {
        $fotos_temporales = explode(",", $row_fotos['fotos_supervision_actual']);
    }
}
$stmt_fotos->close();

$tempDir = 'uploads/supervisiones_temp/';
$uploadDir = 'uploads/supervisiones/';
if (!file_exists($uploadDir)) {
    if (!mkdir($uploadDir, 0777, true)) {
        error_log("No se pudo crear el directorio de subida: $uploadDir");
    }
}

$uploadedFiles = [];

// Mover fotos temporales a la carpeta definitiva
foreach ($fotos_temporales as $f) {
    if (!empty($f) && file_exists($tempDir . $f)) {
        if (rename($tempDir . $f, $uploadDir . $f)) {
            $uploadedFiles[] = $f;
        }
    }
}

// Manejar nuevas fotos enviadas en este request
foreach ($_FILES as $key => $file) {
    if ($file['error'] === UPLOAD_ERR_OK) {
        $tempFile = $file['tmp_name'];
        $extension = pathinfo($file['name'], PATHINFO_EXTENSION) ?: 'jpg';
        $fileName = "sup_" . $id_supervisor . "_" . time() . "_" . $key . "." . $extension;
        $targetFile = $uploadDir . $fileName;
        
        if (move_uploaded_file($tempFile, $targetFile)) {
            $uploadedFiles[] = $fileName;
        }
    }
}

$evidenciasStr = implode(',', $uploadedFiles);

// Usar Sentencias Preparadas para seguridad
$stmt = $conn->prepare("INSERT INTO supervisiones (id_supervisor, id_tecnico, sot, comentarios, latitud, longitud, puntuacion, checklist, evidencia) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

if ($stmt) {
    $stmt->bind_param("iisssssss", $id_supervisor, $id_tecnico, $sot, $comentarios, $lat, $lng, $puntuacion, $checklist, $evidenciasStr);
    
    if ($stmt->execute()) {
        // LIBERAR AL TÉCNICO Y LIMPIAR PROGRESO TEMPORAL
        $stmt_clear = $conn->prepare("UPDATE usuarios SET en_supervision = 0, id_supervisor_actual = NULL, inicio_supervision_actual = NULL, sot_supervision_actual = NULL, checklist_supervision_actual = NULL, comentarios_supervision_actual = NULL, fotos_supervision_actual = NULL WHERE id = ?");
        $stmt_clear->bind_param("i", $id_tecnico);
        $stmt_clear->execute();
        $stmt_clear->close();
        
        sendResponse("success", "Supervisión enviada correctamente", ["id" => $stmt->insert_id]);
    } else {
        error_log("Error ejecutando insert: " . $stmt->error);
        sendResponse("error", "Error al guardar en la base de datos");
    }
    $stmt->close();
} else {
    error_log("Error preparando sentencia: " . $conn->error);
    sendResponse("error", "Error interno del servidor al procesar la solicitud");
}

$conn->close();
?>