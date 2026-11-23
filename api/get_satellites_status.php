<?php
header('Content-Type: application/json');
include 'db_config.php';

// Asegurar zona horaria de Perú
$conn->query("SET time_zone = '-05:00'");

// Resetear automáticamente el almuerzo para todos los usuarios si es un nuevo día
$conn->query("UPDATE usuarios SET almuerzo_usado = 0 WHERE almuerzo_usado = 1 AND inicio_almuerzo IS NOT NULL AND DATE(inicio_almuerzo) < CURDATE()");

// Obtener todos los usuarios que pueden ser relevantes (satélites, auxiliares, supervisores, admins)
$sql = "SELECT id, nombre, rol, estado, ultima_conexion, zona, almuerzo_usado FROM usuarios WHERE rol IN ('satelite', 'auxiliar', 'supervisor', 'admin', 'usuario')";
$result = $conn->query($sql);

$satellites = array();

if ($result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $id = $row['id'];

        // --- LÓGICA INTELIGENTE DE ESTADO ---
        // Verificamos si tiene alguna solicitud en curso
        $sqlCheck = "SELECT COUNT(*) as busy FROM solicitudes WHERE id_satelite = $id AND estado IN ('aceptada', 'camino', 'llegado', 'corregir')";
        $resCheck = $conn->query($sqlCheck);
        $busyCount = $resCheck->fetch_assoc()['busy'];

        // Si tiene solicitudes en curso, sobreescribimos su estado a "Ocupado"
        // (Esto es más fiable que depender solo del estado manual enviado por la app)
        $estadoReal = ($busyCount > 0) ? "Ocupado" : ($row['estado'] ?: "Disponible");

        $satellites[] = array(
            "id" => (int) $id,
            "nombre" => $row['nombre'],
            "rol" => $row['rol'],
            "estado" => $estadoReal,
            "ultima_conexion" => $row['ultima_conexion'],
            "zona" => $row['zona'] ?: "SIN ZONA",
            "almuerzo_usado" => (int) $row['almuerzo_usado']
        );
    }
}

echo json_encode($satellites);
$conn->close();
?>