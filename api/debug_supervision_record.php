<?php
include 'db_config.php';
header('Content-Type: application/json');

$conn->query("SET time_zone = '-05:00'");

$res = $conn->query("SELECT id, id_supervisor, id_tecnico, sot, fecha_creacion FROM supervisiones ORDER BY id DESC LIMIT 10");
$data = [];
while($row = $res->fetch_assoc()) {
    $data[] = $row;
}

$time = $conn->query("SELECT NOW() as ahora, CURDATE() as fecha")->fetch_assoc();

echo json_encode([
    "supervisiones_recientes" => $data,
    "db_time" => $time,
    "php_time" => date('Y-m-d H:i:s')
]);
?>
