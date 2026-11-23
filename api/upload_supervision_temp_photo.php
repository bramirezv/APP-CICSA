<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$id_supervisor = $_POST['id_supervisor'] ?? 0;
$id_tecnico = $_POST['id_tecnico'] ?? 0;

if ($id_supervisor <= 0 || $id_tecnico <= 0 || empty($_FILES['foto'])) {
    echo json_encode(["status" => "error", "message" => "Datos incompletos"]);
    exit;
}

$uploadDir = 'uploads/supervisiones_temp/';
if (!file_exists($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

$file = $_FILES['foto'];
$extension = pathinfo($file['name'], PATHINFO_EXTENSION) ?: 'jpg';
$fileName = "temp_" . $id_supervisor . "_" . time() . "_" . uniqid() . "." . $extension;
$targetFile = $uploadDir . $fileName;

if (move_uploaded_file($file['tmp_name'], $targetFile)) {
    // Agregar el nombre del archivo a la lista de fotos actuales del técnico
    $stmt_get = $conn->prepare("SELECT fotos_supervision_actual FROM usuarios WHERE id = ?");
    $stmt_get->bind_param("i", $id_tecnico);
    $stmt_get->execute();
    $res = $stmt_get->get_result();
    $fotos = "";
    if ($row = $res->fetch_assoc()) {
        $fotos = $row['fotos_supervision_actual'] ?: "";
    }
    $stmt_get->close();

    if ($fotos == "") {
        $fotos = $fileName;
    } else {
        $fotos .= "," . $fileName;
    }

    $stmt_upd = $conn->prepare("UPDATE usuarios SET fotos_supervision_actual = ? WHERE id = ?");
    $stmt_upd->bind_param("si", $fotos, $id_tecnico);
    $stmt_upd->execute();
    $stmt_upd->close();

    echo json_encode([
        "status" => "success", 
        "message" => "Foto subida", 
        "filename" => $fileName,
        "url" => $uploadDir . $fileName
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Error al mover archivo"]);
}

$conn->close();
?>
