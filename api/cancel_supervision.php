<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$id_supervisor = $_POST['id_supervisor'] ?? 0;
$id_tecnico = $_POST['id_tecnico'] ?? 0;

if ($id_tecnico <= 0 || $id_supervisor <= 0) {
    echo json_encode(["status" => "error", "message" => "Datos incompletos"]);
    exit;
}

// Verificar que el supervisor que cancela es el mismo que inició la supervisión
$stmt = $conn->prepare("UPDATE usuarios SET en_supervision = 0, id_supervisor_actual = NULL, inicio_supervision_actual = NULL, sot_supervision_actual = NULL 
                        WHERE id = ? AND id_supervisor_actual = ?");
$stmt->bind_param("ii", $id_tecnico, $id_supervisor);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Supervisión cancelada y técnico liberado"]);
} else {
    echo json_encode(["status" => "error", "message" => "Error al liberar técnico"]);
}

$conn->close();
?>
