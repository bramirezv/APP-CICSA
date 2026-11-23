<?php
// ARCHIVO: api/get_attendance.php
// Obtener el historial de asistencias (Solo Admin puede ver esto usualmente, pero se deja abierto para la sección de control)
header('Content-Type: application/json');
include 'db_config.php';

// Obtener fecha (por defecto hoy)
$fecha = isset($_GET['fecha']) ? $_GET['fecha'] : date("Y-m-d");

$query = "SELECT a.id, a.usuario_id as userId, u.nombre, a.evento, a.fecha, a.hora 
          FROM asistencias a
          JOIN usuarios u ON a.usuario_id = u.id
          WHERE a.fecha = ?
          ORDER BY a.hora DESC";

$stmt = $conn->prepare($query);
$stmt->bind_param("s", $fecha);
$stmt->execute();
$result = $stmt->get_result();

$asistencias = [];
while ($row = $result->fetch_assoc()) {
    $asistencias[] = $row;
}

echo json_encode($asistencias);

$stmt->close();
if (isset($conn) && $conn) {
    $conn->close();
}
?>
