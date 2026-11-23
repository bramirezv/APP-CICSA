<?php
// ARCHIVO: check_accepted_tickets.php
// Retorna solicitudes del supervisor que acaban de ser ACEPTADAS por un satélite.
// Se usa para notificar al supervisor en tiempo real.
header('Content-Type: application/json');
include 'db_config.php';
$conn->query("SET time_zone = '-05:00';");

$supervisor = isset($_GET['supervisor']) ? $conn->real_escape_string($_GET['supervisor']) : '';
$since_seconds = isset($_GET['since_seconds']) ? intval($_GET['since_seconds']) : 15;

// Seguridad: máximo 60 segundos hacia atrás
if ($since_seconds < 5) $since_seconds = 5;
if ($since_seconds > 60) $since_seconds = 60;

if (empty($supervisor)) {
    echo json_encode(["status" => "error", "message" => "Supervisor requerido"]);
    exit;
}

// Buscar solicitudes NORMALES del supervisor que acaban de ser aceptadas
$sql = "SELECT id, sot, tecnico, motivo, zona, estado, nombre_satelite, fecha_aceptacion
        FROM solicitudes
        WHERE nombre_supervisor = '$supervisor'
          AND tipo = 'normal'
          AND estado = 'aceptada'
          AND fecha_aceptacion IS NOT NULL
          AND fecha_aceptacion >= (NOW() - INTERVAL $since_seconds SECOND)
        ORDER BY fecha_aceptacion DESC
        LIMIT 10";

$result = $conn->query($sql);
$accepted = [];
if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $accepted[] = [
            "id"              => (int)$row['id'],
            "sot"             => $row['sot'],
            "tecnico"         => $row['tecnico'],
            "motivo"          => $row['motivo'],
            "zona"            => $row['zona'],
            "nombre_satelite" => $row['nombre_satelite'],
            "fecha_aceptacion"=> $row['fecha_aceptacion']
        ];
    }
}

echo json_encode([
    "status"   => "success",
    "count"    => count($accepted),
    "accepted" => $accepted
]);
$conn->close();
?>

