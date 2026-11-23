<?php
header('Content-Type: application/json');
include 'auth_check.php';

// Asegurar zona horaria de Perú
$conn->query("SET time_zone = '-05:00'");

// Leer el JSON del cuerpo de la petición
$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data['userId']) || !isset($data['estado'])) {
    echo json_encode(array("status" => "error", "message" => "Parámetros insuficientes"));
    exit;
}

$userId = intval($data['userId']);
$estado = $conn->real_escape_string($data['estado']);
$sessionToken = $data['session_token'] ?? null;

// Validar Sesión Única
validateSession($conn, $userId, $sessionToken);

// Actualizar estado, última conexión y ruta activa
$almuerzoSql = ($estado === 'Almorzando') ? ", almuerzo_usado = 1, inicio_almuerzo = NOW()" : "";
$rutaSql = ($estado === 'No Disponible') ? ", ruta_activa = 0" : ", ruta_activa = 1";

$sql = "UPDATE usuarios SET estado = '$estado', ultima_conexion = NOW() $almuerzoSql $rutaSql WHERE id = $userId";

if ($conn->query($sql) === TRUE) {
    echo json_encode(array("status" => "success", "message" => "Estado actualizado correctamente"));
} else {
    echo json_encode(array("status" => "error", "message" => "Error al actualizar: " . $conn->error));
}

$conn->close();
?>