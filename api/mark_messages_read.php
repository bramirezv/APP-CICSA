<?php
header('Content-Type: application/json');
include 'db_config.php';

// Verificar que recibimos los parámetros
if (!isset($_GET['mi_id']) || !isset($_GET['otro_id'])) {
    echo json_encode(array("success" => false, "error" => "Parámetros faltantes"));
    exit;
}

$miId = intval($_GET['mi_id']);
$otroId = intval($_GET['otro_id']);

// Verificar conexión
if ($conn->connect_error) {
    echo json_encode(array("success" => false, "error" => "Error de conexión: " . $conn->connect_error));
    exit;
}

// Marcar como leídos todos los mensajes recibidos de 'otroId' por 'miId'
$sql = "UPDATE mensajes SET leido = 1 WHERE emisor_id = $otroId AND receptor_id = $miId AND leido = 0";

if ($conn->query($sql) === TRUE) {
    echo json_encode(array("success" => true, "affected_rows" => $conn->affected_rows));
} else {
    echo json_encode(array("success" => false, "error" => "Error al ejecutar: " . $conn->error));
}

$conn->close();
?>
