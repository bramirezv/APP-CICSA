<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$json = file_get_contents('php://input');
$data = json_decode($json, true);

$userId = isset($data['id']) ? $data['id'] : null;
$nombre = isset($data['nombre']) ? $data['nombre'] : null;
$telefono = isset($data['telefono']) ? $data['telefono'] : null;
$email = isset($data['email']) ? $data['email'] : null;
$direccion = isset($data['direccion']) ? $data['direccion'] : null;
$fecha_nacimiento = isset($data['fecha_nacimiento']) ? $data['fecha_nacimiento'] : null;
$telefono_emergencia = isset($data['telefono_emergencia']) ? $data['telefono_emergencia'] : null;

if (!$userId) {
    echo json_encode(["status" => "error", "message" => "ID de usuario no proporcionado"]);
    exit;
}

// ACTUALIZACIÓN DE PERFIL
$sql = "UPDATE usuarios SET nombre=?, telefono=?, email=?, direccion=?, fecha_nacimiento=?, telefono_emergencia=? WHERE id=?";
$stmt = $conn->prepare($sql);

if ($stmt) {
    $stmt->bind_param("ssssssi", $nombre, $telefono, $email, $direccion, $fecha_nacimiento, $telefono_emergencia, $userId);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Perfil actualizado correctamente"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Error al actualizar: " . $conn->error]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Error en la consulta: " . $conn->error]);
}

$conn->close();
?>
