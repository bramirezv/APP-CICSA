<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$id_tecnico = isset($_GET['id_tecnico']) ? $_GET['id_tecnico'] : null;
$id_supervisor = isset($_GET['id_supervisor']) ? $_GET['id_supervisor'] : null;
$fecha = isset($_GET['fecha']) ? $_GET['fecha'] : null;

$sql = "SELECT * FROM tnps_evidencia";
$conditions = [];
$params = [];
$types = "";

if ($id_tecnico) {
    $conditions[] = "id_tecnico = ?";
    $params[] = $id_tecnico;
    $types .= "i";
}

if ($id_supervisor) {
    $conditions[] = "id_supervisor = ?";
    $params[] = $id_supervisor;
    $types .= "i";
}

if ($fecha) {
    $conditions[] = "DATE(fecha_registro) = ?";
    $params[] = $fecha;
    $types .= "s";
}

if (!empty($conditions)) {
    $sql .= " WHERE " . implode(" AND ", $conditions);
}

$sql .= " ORDER BY fecha_registro DESC";

$stmt = $conn->prepare($sql);

if (!empty($params)) {
    $stmt->bind_param($types, ...$params);
}

$stmt->execute();
$result = $stmt->get_result();
$evidencias = [];

while ($row = $result->fetch_assoc()) {
    $evidencias[] = $row;
}

echo json_encode(["status" => "success", "data" => $evidencias]);

$stmt->close();
$conn->close();
?>
