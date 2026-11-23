<?php
header('Content-Type: application/json');
include 'db_config.php';

// Leer el JSON del cuerpo de la petición
$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data['userId'])) {
    echo json_encode(array("status" => "error", "message" => "Falta el ID del usuario"));
    exit;
}

$userId = intval($data['userId']);

// Resetear el permiso de almuerzo
$sql = "UPDATE usuarios SET almuerzo_usado = 0 WHERE id = $userId";

if ($conn->query($sql) === TRUE) {
    echo json_encode(array("status" => "success", "message" => "Permiso de almuerzo reseteado correctamente"));
} else {
    echo json_encode(array("status" => "error", "message" => "Error al resetear: " . $conn->error));
}

$conn->close();
?>
