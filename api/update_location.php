<?php
// ARCHIVO: update_location.php
include 'db_config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $userId = $_POST['userId'] ?? null;
    $lat = $_POST['lat'] ?? null;
    $lng = $_POST['lng'] ?? null;

    if ($userId && $lat !== null && $lng !== null) {
        $stmt = $conn->prepare("UPDATE usuarios SET latitud = ?, longitud = ?, ultima_ubicacion = NOW() WHERE id = ?");
        $stmt->bind_param("ddi", $lat, $lng, $userId);

        if ($stmt->execute()) {
            echo json_encode(["status" => "success", "message" => "Ubicación actualizada"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Fallo al actualizar: " . $stmt->error]);
        }
        $stmt->close();
    } else {
        echo json_encode(["status" => "error", "message" => "Datos incompletos"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Método no permitido"]);
}

$conn->close();
?>
