<?php
// ARCHIVO: upload_evidencia.php
// Sube una foto de evidencia y la AGREGA al campo foto_evidencia (soporta múltiples fotos separadas por coma)
header("Content-Type: application/json");
include 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["status" => "error", "message" => "Método no permitido"]);
    exit;
}

$id = isset($_POST['id']) ? intval($_POST['id']) : 0;

if ($id <= 0) {
    echo json_encode(["status" => "error", "message" => "ID de solicitud inválido"]);
    exit;
}

if (!isset($_FILES['foto']) || $_FILES['foto']['error'] !== UPLOAD_ERR_OK) {
    echo json_encode(["status" => "error", "message" => "No se recibió la foto correctamente"]);
    exit;
}

// Crear directorio de uploads si no existe
$uploadDir = __DIR__ . '/uploads/evidencias/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

// Generar nombre único para la foto
$extension = pathinfo($_FILES['foto']['name'], PATHINFO_EXTENSION) ?: 'jpg';
$fileName = "ev_{$id}_" . time() . "_" . rand(1000, 9999) . ".{$extension}";
$targetPath = $uploadDir . $fileName;

if (!move_uploaded_file($_FILES['foto']['tmp_name'], $targetPath)) {
    echo json_encode(["status" => "error", "message" => "Error al guardar la foto en el servidor"]);
    exit;
}

// Ruta relativa para guardar en la BD
$relativePath = "api/uploads/evidencias/" . $fileName;

// Obtener las fotos actuales para CONCATENAR (multi-foto)
$stmt = $conn->prepare("SELECT foto_evidencia FROM solicitudes WHERE id = ?");
$stmt->bind_param("i", $id);
$stmt->execute();
$result = $stmt->get_result();
$row = $result->fetch_assoc();
$stmt->close();

$currentPhotos = $row['foto_evidencia'] ?? '';

if (!empty($currentPhotos) && $currentPhotos !== 'null') {
    // Agregar la nueva foto separada por coma
    $newPhotos = $currentPhotos . ',' . $relativePath;
} else {
    $newPhotos = $relativePath;
}

// Actualizar la BD con todas las fotos
$stmt2 = $conn->prepare("UPDATE solicitudes SET foto_evidencia = ? WHERE id = ?");
$stmt2->bind_param("si", $newPhotos, $id);

if ($stmt2->execute()) {
    echo json_encode([
        "status" => "success",
        "message" => "Evidencia subida correctamente",
        "foto_url" => $relativePath
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Error al actualizar la base de datos"]);
}

$stmt2->close();
$conn->close();
?>
