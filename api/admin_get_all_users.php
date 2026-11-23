<?php
header('Content-Type: application/json');
include 'db_config.php';

/**
 * Endpoint de Administración Maestro
 * Devuelve TODOS los usuarios en el sistema con sus datos completos para gestión.
 */

$sql = "SELECT id, nombre, email, telefono, rol, zona, estado, ultima_conexion, latitud, longitud, almuerzo_usado FROM usuarios ORDER BY id DESC";
$result = $conn->query($sql);

$users = array();

if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $users[] = array(
            "id" => (int) $row['id'],
            "nombre" => $row['nombre'] ?: "Sin Nombre",
            "email" => $row['email'] ?: "",
            "telefono" => $row['telefono'] ?: "",
            "rol" => $row['rol'] ?: "satelite",
            "zona" => $row['zona'] ?: "SIN ZONA",
            "estado" => $row['estado'] ?: "Fuera de línea",
            "ultima_conexion" => $row['ultima_conexion'] ?: "Nunca",
            "lat" => $row['latitud'] ? (float) $row['latitud'] : null,
            "lng" => $row['longitud'] ? (float) $row['longitud'] : null,
            "almuerzo_usado" => (int) $row['almuerzo_usado']                    
        );
    }
}

echo json_encode($users);
$conn->close();
?>