<?php
// ARCHIVO: get_users_locations.php
include 'db_config.php';
header('Content-Type: application/json');

// Obtenemos usuarios activos en las últimas 24 horas (Satelites, Supervisores y Administradores para pruebas)
$sql = "SELECT id, nombre, rol, zona, latitud, longitud, ultima_ubicacion, estado, ruta_activa, almuerzo_usado, telefono 
        FROM usuarios 
        WHERE rol IN ('satelite', 'supervisor', 'admin') 
        AND latitud IS NOT NULL 
        AND longitud IS NOT NULL
        AND ultima_ubicacion >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
        ORDER BY ultima_ubicacion DESC";

$result = $conn->query($sql);

if (!$result) {
    echo json_encode(["status" => "error", "message" => "SQL Error: " . $conn->error]);
    $conn->close();
    exit;
}

$users = [];

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $users[] = [
            "id" => (int)$row['id'],
            "nombre" => $row['nombre'],
            "rol" => $row['rol'],
            "zona" => $row['zona'],
            "lat" => (float)$row['latitud'],
            "lng" => (float)$row['longitud'],
            "ultima_conexion" => $row['ultima_ubicacion'],
            "estado" => $row['estado'] ?: "Disponible",
            "ruta_activa" => (int)($row['ruta_activa'] ?? 0),
            "almuerzo_usado" => (int)($row['almuerzo_usado'] ?? 0),
            "telefono" => $row['telefono'] ?: ""
        ];
    }
}

echo json_encode($users);
$conn->close();
?>
