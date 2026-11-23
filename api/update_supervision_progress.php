<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$id_supervisor = $_POST['id_supervisor'] ?? 0;
$id_tecnico = $_POST['id_tecnico'] ?? 0;
$checklist = $_POST['checklist'] ?? null;
$comentarios = $_POST['comentarios'] ?? null;
$fotos = $_POST['fotos'] ?? null; // Nombres de archivos separados por comas
$session_token = $_POST['session_token'] ?? '';

if ($id_supervisor <= 0 || $id_tecnico <= 0) {
    echo json_encode(["status" => "error", "message" => "Datos incompletos"]);
    exit;
}

// Validación de sesión única opcional aquí, pero recomendada
$stmt_session = $conn->prepare("SELECT session_token FROM usuarios WHERE id = ?");
$stmt_session->bind_param("i", $id_supervisor);
$stmt_session->execute();
$res_session = $stmt_session->get_result();
if ($row_session = $res_session->fetch_assoc()) {
    $db_token = $row_session['session_token'];
    if (!empty($db_token) && $db_token !== $session_token) {
        echo json_encode(["status" => "error", "message" => "Sesión inválida", "session_error" => true]);
        exit;
    }
}
$stmt_session->close();

// Actualizar el progreso en el registro del TÉCNICO (que es quien tiene los campos de supervisión activa)
$stmt = $conn->prepare("UPDATE usuarios 
                        SET checklist_supervision_actual = ?, 
                            comentarios_supervision_actual = ?, 
                            fotos_supervision_actual = ? 
                        WHERE id = ? AND id_supervisor_actual = ? AND en_supervision = 1");

if ($stmt) {
    $stmt->bind_param("sssii", $checklist, $comentarios, $fotos, $id_tecnico, $id_supervisor);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Progreso guardado"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Error al guardar progreso: " . $stmt->error]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Error en la base de datos"]);
}

$conn->close();
?>
