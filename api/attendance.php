<?php
// ARCHIVO: api/attendance.php
// Registrar asistencia (Ingreso/Salida) de un satélite
header('Content-Type: application/json');
include 'db_config.php';

// Solo aceptar peticiones POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Método no permitido"]);
    exit;
}

// Obtener datos por POST (FormUrlEncoded como lo envía Retrofit)
$userId = isset($_POST['userId']) ? intval($_POST['userId']) : 0;
$evento = isset($_POST['evento']) ? strtoupper(trim($_POST['evento'])) : '';

if ($userId <= 0 || !in_array($evento, ['INGRESO', 'SALIDA'])) {
    echo json_encode(["status" => "error", "message" => "Datos incompletos o inválidos"]);
    exit;
}

// Inserción en la tabla de asistencias
$stmt = $conn->prepare("INSERT INTO asistencias (usuario_id, evento, fecha, hora) VALUES (?, ?, CURRENT_DATE, CURRENT_TIME)");
$stmt->bind_param("is", $userId, $evento);

if ($stmt->execute()) {
    echo json_encode([
        "status" => "success",
        "message" => "Registro de $evento realizado con éxito",
        "timestamp" => date("Y-m-d H:i:s")
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Fallo al registrar asistencia: " . $conn->error]);
}

$stmt->close();
if (isset($conn) && $conn) {
    $conn->close();
}
?>
