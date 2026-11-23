<?php
header('Content-Type: application/json');
include 'db_config.php';

// Obtener parámetros y normalizar
$userId = isset($_GET['mi_id']) ? intval($_GET['mi_id']) : 0;
$rol = isset($_GET['rol']) ? strtolower(trim($_GET['rol'])) : '';

if ($userId == 0) {
    echo json_encode(array());
    exit;
}

// Lógica de Visibilidad Maestra:
// 1. Satélites -> Ven Supervisores y Admins
// 2. Supervisores/Admins -> Ven Satélites, otros Supervisores y otros Admins
if ($rol == 'satelite' || $rol == 'auxiliar') {
    $sql = "SELECT id, nombre, rol, estado, ultima_conexion FROM usuarios WHERE LOWER(rol) IN ('supervisor', 'admin') AND id != $userId";
} else {
    $sql = "SELECT id, nombre, rol, estado, ultima_conexion FROM usuarios WHERE id != $userId";
}

$result = $conn->query($sql);
$contacts = array();

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $otroId = intval($row['id']);

        // Contar mensajes no leídos dirigidos a MÍ desde este contacto
        $sqlUnread = "SELECT COUNT(*) as unread FROM mensajes WHERE emisor_id = $otroId AND receptor_id = $userId AND leido = 0";
        $resUnread = $conn->query($sqlUnread);
        $unreadCount = 0;
        if ($resUnread) {
            $rowUnread = $resUnread->fetch_assoc();
            $unreadCount = intval($rowUnread['unread']);
        }

        $contacts[] = array(
            "id" => $otroId,
            "nombre" => $row['nombre'],
            "rol" => $row['rol'],
            "estado" => $row['estado'] ?: "Desconectado",
            "ultima_conexion" => $row['ultima_conexion'],
            "unreadCount" => $unreadCount
        );
    }
}

echo json_encode($contacts);
$conn->close();
?>