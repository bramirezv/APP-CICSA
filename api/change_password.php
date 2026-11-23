<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$json = file_get_contents('php://input');
$data = json_decode($json, true);

$userId = isset($data['id']) ? $data['id'] : null;
$currentPassword = isset($data['current_password']) ? $data['current_password'] : null;
$newPassword = isset($data['new_password']) ? $data['new_password'] : null;

if (!$userId || !$currentPassword || !$newPassword) {
    echo json_encode(["status" => "error", "message" => "Faltan datos obligatorios"]);
    exit;
}

// 1. Obtener la contraseña actual de la DB
$sql = "SELECT password FROM usuarios WHERE id = ?";
$stmt = $conn->prepare($sql);
if ($stmt) {
    $stmt->bind_param("i", $userId);
    $stmt->execute();
    $result = $stmt->get_result();
    $user = $result->fetch_assoc();
    
    if (!$user) {
        echo json_encode(["status" => "error", "message" => "Usuario no encontrado"]);
        exit;
    }

    // 2. Verificar que la contraseña actual sea correcta
    if (!password_verify($currentPassword, $user['password'])) {
        echo json_encode(["status" => "error", "message" => "La contraseña actual es incorrecta"]);
        exit;
    }

    // 3. Hashear la nueva contraseña y actualizar
    $newHashedPassword = password_hash($newPassword, PASSWORD_DEFAULT);
    $updateSql = "UPDATE usuarios SET password = ? WHERE id = ?";
    $updateStmt = $conn->prepare($updateSql);
    
    if ($updateStmt) {
        $updateStmt->bind_param("si", $newHashedPassword, $userId);
        if ($updateStmt->execute()) {
            echo json_encode(["status" => "success", "message" => "Contraseña actualizada correctamente"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Error al actualizar clave"]);
        }
    }
}

$conn->close();
?>
