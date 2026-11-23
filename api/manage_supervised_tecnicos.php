<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

// Auto-migración
$conn->query("CREATE TABLE IF NOT EXISTS supervisor_tecnicos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_supervisor INT NOT NULL,
    id_tecnico INT NOT NULL,
    fecha_asignacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (id_supervisor, id_tecnico)
)");

$data = json_decode(file_get_contents("php://input"), true);

$id_supervisor = isset($data['id_supervisor']) ? (int)$data['id_supervisor'] : 0;
$id_tecnico = isset($data['id_tecnico']) ? (int)$data['id_tecnico'] : 0;
$action = isset($data['action']) ? $data['action'] : ''; // 'add' o 'remove'

if ($id_supervisor <= 0 || $id_tecnico <= 0) {
    echo json_encode(["status" => "error", "message" => "IDs inválidos"]);
    exit;
}

if ($action === 'add') {
    $stmt = $conn->prepare("INSERT IGNORE INTO supervisor_tecnicos (id_supervisor, id_tecnico) VALUES (?, ?)");
    $stmt->bind_param("ii", $id_supervisor, $id_tecnico);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Técnico agregado a tu equipo"]);
    } else {
        echo json_encode(["status" => "error", "message" => $stmt->error]);
    }
} else if ($action === 'remove') {
    $stmt = $conn->prepare("DELETE FROM supervisor_tecnicos WHERE id_supervisor = ? AND id_tecnico = ?");
    $stmt->bind_param("ii", $id_supervisor, $id_tecnico);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Técnico removido de tu equipo"]);
    } else {
        echo json_encode(["status" => "error", "message" => $stmt->error]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Acción no válida"]);
}

$conn->close();
?>
