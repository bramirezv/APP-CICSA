<?php
header('Content-Type: application/json');
include 'db_config.php';

// Script de diagnóstico avanzado: Mock de respuesta de login
$id_prueba = 1; // Un ID que exista
$sql = "SELECT id, nombre, rol, zona FROM usuarios LIMIT 1";
$result = $conn->query($sql);

$mock_response = array();
if ($result && $row = $result->fetch_assoc()) {
    $mock_response = array(
        "status" => "success",
        "message" => "Diagnóstico OK",
        "userId" => (int)$row['id'],
        "userName" => $row['nombre'],
        "rol" => $row['rol'],
        "zona" => $row['zona'] ?: "SIN ZONA"
    );
}

// También mostramos las columnas para estar 100% seguros
$sqlCols = "DESCRIBE usuarios";
$resCols = $conn->query($sqlCols);
$cols = array();
while($r = $resCols->fetch_assoc()) { $cols[] = $r; }

echo json_encode(array(
    "MOCK_LOGIN_RESPONSE_JSON" => $mock_response,
    "COLUMN_NAMES" => $cols
));
$conn->close();
?>
