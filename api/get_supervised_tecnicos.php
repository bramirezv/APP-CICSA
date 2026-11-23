<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$id_supervisor = isset($_GET['id_supervisor']) ? (int)$_GET['id_supervisor'] : 0;

if ($id_supervisor <= 0) {
    echo json_encode(["status" => "error", "message" => "ID de supervisor inválido"]);
    exit;
}

$sql = "SELECT u.id, u.nombre, u.dni, u.zona, u.celular_personal as telefono 
        FROM supervisor_tecnicos st
        JOIN usuarios u ON st.id_tecnico = u.id
        WHERE st.id_supervisor = $id_supervisor";

$result = $conn->query($sql);
$equipo = [];

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $equipo[] = $row;
    }
    echo json_encode(["status" => "success", "data" => $equipo]);
} else {
    echo json_encode(["status" => "error", "message" => $conn->error]);
}

$conn->close();
?>
