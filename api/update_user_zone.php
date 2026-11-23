<?php
header('Content-Type: application/json');
include 'db_config.php';

// Obtener datos del admin para actualizar la zona del satélite
// Ahora soportamos tanto JSON como POST normal por si acaso
$json = file_get_contents("php://input");
$data = json_decode($json, true);

// Si no es JSON, intentamos con $_POST
if (!$data) {
    $data = $_POST;
}

// Soportamos tanto 'userId' (App) como 'id' (Web)
$userId = 0;
if (isset($data['userId'])) {
    $userId = intval($data['userId']);
} elseif (isset($data['id'])) {
    $userId = intval($data['id']);
}

$zona = isset($data['zona']) ? $conn->real_escape_string($data['zona']) : '';

// --- VALIDACIÓN ESTRICTA ---
if ($userId <= 0 || empty($zona)) {
    echo json_encode(array(
        "status" => "error", 
        "message" => "Datos inválidos (ID: $userId, Zona: $zona)",
        "debug_received" => $data
    ));
    exit;
}

// Actualizar la zona del usuario
$sql = "UPDATE usuarios SET zona = '$zona' WHERE id = $userId";

if ($conn->query($sql) === TRUE) {
    if ($conn->affected_rows > 0) {
        echo json_encode(array("status" => "success", "message" => "Zona actualizada correctamente a $zona"));
    } else {
        // Si no afectó filas puede ser que la zona ya era la misma
        echo json_encode(array("status" => "success", "message" => "La zona ya estaba establecida en $zona o el ID no cambió nada"));
    }
} else {
    echo json_encode(array("status" => "error", "message" => "Error de base de datos: " . $conn->error));
}

$conn->close();
?>
