<?php
// ARCHIVO: delete_evidencia.php
// Elimina UNA foto específica del campo foto_evidencia de una solicitud
header("Content-Type: application/json");
include 'db_config.php';

$data = json_decode(file_get_contents("php://input"), true);
$id = isset($data['id']) ? intval($data['id']) : 0;
$foto_url = isset($data['foto_url']) ? trim($data['foto_url']) : '';

if ($id <= 0 || empty($foto_url)) {
    echo json_encode(["status" => "error", "message" => "Parámetros inválidos"]);
    exit;
}

// Obtener las fotos actuales
$stmt = $conn->prepare("SELECT foto_evidencia FROM solicitudes WHERE id = ?");
$stmt->bind_param("i", $id);
$stmt->execute();
$result = $stmt->get_result();
$row = $result->fetch_assoc();
$stmt->close();

if (!$row) {
    echo json_encode(["status" => "error", "message" => "Solicitud no encontrada"]);
    exit;
}

$currentPhotos = $row['foto_evidencia'] ?? '';
$photos = array_filter(array_map('trim', explode(',', $currentPhotos)));

// Buscar y eliminar la foto indicada
$newPhotos = array_filter($photos, function($p) use ($foto_url) {
    return trim($p) !== trim($foto_url);
});

// Intentar eliminar el archivo físico del servidor
$cleanPath = ltrim($foto_url, '/');
$fullPath = __DIR__ . '/../' . $cleanPath;
if (file_exists($fullPath)) {
    @unlink($fullPath);
} else {
    // Intentar ruta directa
    $fullPath2 = __DIR__ . '/' . str_replace('api/', '', $cleanPath);
    if (file_exists($fullPath2)) {
        @unlink($fullPath2);
    }
}

// Actualizar BD
$newPhotoStr = implode(',', $newPhotos);
$stmt2 = $conn->prepare("UPDATE solicitudes SET foto_evidencia = ? WHERE id = ?");
$stmt2->bind_param("si", $newPhotoStr, $id);

if ($stmt2->execute()) {
    echo json_encode([
        "status" => "success",
        "message" => "Foto eliminada correctamente",
        "foto_evidencia" => $newPhotoStr
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Error al actualizar la base de datos"]);
}

$stmt2->close();
$conn->close();
?>
