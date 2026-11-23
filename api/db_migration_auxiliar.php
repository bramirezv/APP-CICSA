<?php
/**
 * Migración: Agregar columna 'tipo' a la tabla solicitudes
 * para diferenciar solicitudes normales de plantillas de auxiliar.
 * 
 * Ejecutar UNA sola vez en producción.
 */
header('Content-Type: application/json');
include 'db_config.php';

$results = array();

// 1. Agregar columna 'tipo' a solicitudes
$sql = "ALTER TABLE solicitudes ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) DEFAULT 'normal'";
if ($conn->query($sql) === TRUE) {
    $results[] = "✅ Columna 'tipo' agregada a solicitudes (o ya existía)";
} else {
    // Intentar sin IF NOT EXISTS para MySQL < 10.0
    $checkCol = $conn->query("SHOW COLUMNS FROM solicitudes LIKE 'tipo'");
    if ($checkCol && $checkCol->num_rows == 0) {
        $sql2 = "ALTER TABLE solicitudes ADD COLUMN tipo VARCHAR(20) DEFAULT 'normal'";
        if ($conn->query($sql2) === TRUE) {
            $results[] = "✅ Columna 'tipo' agregada a solicitudes";
        } else {
            $results[] = "❌ Error al agregar columna 'tipo': " . $conn->error;
        }
    } else {
        $results[] = "✅ Columna 'tipo' ya existe en solicitudes";
    }
}

echo json_encode(array(
    "status" => "success",
    "results" => $results
));

$conn->close();
?>
