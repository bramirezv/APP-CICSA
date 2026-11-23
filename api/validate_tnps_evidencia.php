<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$input = json_decode(file_get_contents('php://input'), true);

$id = isset($input['id']) ? intval($input['id']) : 0;
$estado_validacion = isset($input['estado_validacion']) ? $input['estado_validacion'] : null;
$comentario_supervisor = isset($input['comentario_supervisor']) ? $input['comentario_supervisor'] : null;

if ($id <= 0 || !$estado_validacion) {
    echo json_encode(["status" => "error", "message" => "Datos incompletos"]);
    exit;
}

// Validar que el estado sea correcto
$allowed_states = ['pendiente', 'validado', 'rechazado'];
if (!in_array($estado_validacion, $allowed_states)) {
    echo json_encode(["status" => "error", "message" => "Estado inválido"]);
    exit;
}

try {
    $stmt = $conn->prepare("UPDATE tnps_evidencia SET estado_validacion = ?, comentario_supervisor = ? WHERE id = ?");
    $stmt->bind_param("ssi", $estado_validacion, $comentario_supervisor, $id);
    
    if ($stmt->execute()) {
        if ($stmt->affected_rows > 0) {
            echo json_encode(["status" => "success", "message" => "Validación guardada correctamente"]);
        } else {
            echo json_encode(["status" => "error", "message" => "No se encontró el registro o no hubo cambios"]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "Error al actualizar DB: " . $stmt->error]);
    }
    
    $stmt->close();
} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => "Excepción: " . $e->getMessage()]);
}

$conn->close();
?>
