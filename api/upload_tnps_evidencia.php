<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

// AUTO-MIGRACIÓN: Crear tabla si no existe para rapidez en despliegue
$conn->query("CREATE TABLE IF NOT EXISTS tnps_evidencia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_tecnico INT NOT NULL,
    nombre_tecnico VARCHAR(255) NOT NULL,
    sot VARCHAR(50) NULL,
    foto_url TEXT NOT NULL,
    comentario TEXT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    latitud DECIMAL(10, 8) NULL,
    longitud DECIMAL(11, 8) NULL
)");

// AUTO-MIGRACIÓN: Agregar nuevas columnas si no existen
$conn->query("ALTER TABLE tnps_evidencia ADD COLUMN IF NOT EXISTS id_supervisor INT NULL");
$conn->query("ALTER TABLE tnps_evidencia ADD COLUMN IF NOT EXISTS nombre_supervisor VARCHAR(255) NULL");
$conn->query("ALTER TABLE tnps_evidencia ADD COLUMN IF NOT EXISTS estado_validacion VARCHAR(20) DEFAULT 'pendiente'");
$conn->query("ALTER TABLE tnps_evidencia ADD COLUMN IF NOT EXISTS comentario_supervisor TEXT NULL");

// Capturar datos
$id_tecnico = isset($_POST['id_tecnico']) ? $_POST['id_tecnico'] : null;
$nombre_tecnico = isset($_POST['nombre_tecnico']) ? $_POST['nombre_tecnico'] : null;
$sot = isset($_POST['sot']) ? $_POST['sot'] : null;
$comentario = isset($_POST['comentario']) ? $_POST['comentario'] : null;
$lat = isset($_POST['lat']) ? $_POST['lat'] : null;
$lng = isset($_POST['lng']) ? $_POST['lng'] : null;
$id_supervisor = isset($_POST['id_supervisor']) ? $_POST['id_supervisor'] : null;
$nombre_supervisor = isset($_POST['nombre_supervisor']) ? $_POST['nombre_supervisor'] : null;

if (!$id_tecnico || !isset($_FILES['foto'])) {
    echo json_encode(["status" => "error", "message" => "Faltan datos obligatorios"]);
    exit;
}

// Procesar imagen
$uploadDir = __DIR__ . '/uploads/tnps/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

$file_ext = pathinfo($_FILES["foto"]["name"], PATHINFO_EXTENSION) ?: 'jpg';
$file_name = "tnps_" . $id_tecnico . "_" . time() . "." . $file_ext;
$target_file = $uploadDir . $file_name;

if (move_uploaded_file($_FILES["foto"]["tmp_name"], $target_file)) {
    $url_final = "https://www.cicsamovil.com/api/uploads/tnps/" . $file_name;
    
    try {
        $stmt = $conn->prepare("INSERT INTO tnps_evidencia (id_tecnico, nombre_tecnico, sot, foto_url, comentario, latitud, longitud, id_supervisor, nombre_supervisor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->bind_param("issssssis", $id_tecnico, $nombre_tecnico, $sot, $url_final, $comentario, $lat, $lng, $id_supervisor, $nombre_supervisor);
        
        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "message" => "Evidencia guardada", "url" => $url_final]);
        } else {
            echo json_encode(["status" => "error", "message" => "Error al guardar en DB: " . $stmt->error]);
        }
        $stmt->close();
    } catch (Exception $e) {
        echo json_encode(["status" => "error", "message" => "Excepción: " . $e->getMessage()]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Error al mover el archivo a: " . $target_file]);
}

$conn->close();
?>
