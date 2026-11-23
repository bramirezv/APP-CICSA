<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

// Ver los roles actuales en la base de datos para depuración
$sql = "SELECT DISTINCT rol FROM usuarios";
$result = $conn->query($sql);
$roles = [];
while ($row = $result->fetch_assoc()) {
    $roles[] = $row['rol'];
}

// Ver los últimos 5 usuarios registrados
$sqlUsers = "SELECT id, nombre, rol, dni FROM usuarios ORDER BY id DESC LIMIT 5";
$resultUsers = $conn->query($sqlUsers);
$users = [];
while ($row = $resultUsers->fetch_assoc()) {
    $users[] = $row;
}

echo json_encode([
    "debug_roles" => $roles,
    "last_users" => $users
]);
?>
