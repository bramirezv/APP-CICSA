<?php
header('Content-Type: application/json');
include 'db_config.php';

/**
 * Script de Migración: Agregar columna almuerzo_usado
 * Ejecutar una sola vez para actualizar la base de datos.
 */

$column_check = $conn->query("SHOW COLUMNS FROM usuarios LIKE 'almuerzo_usado'");

if ($column_check->num_rows == 0) {
    // La columna no existe, la creamos
    $sql = "ALTER TABLE usuarios ADD COLUMN almuerzo_usado TINYINT DEFAULT 0 AFTER ruta_activa";
    if ($conn->query($sql) === TRUE) {
        echo json_encode(["status" => "success", "message" => "Base de Datos actualizada: Columna 'almuerzo_usado' creada con éxito."]);
    } else {
        echo json_encode(["status" => "error", "message" => "Error al actualizar DB: " . $conn->error]);
    }
} else {
    echo json_encode(["status" => "success", "message" => "La base de datos ya está actualizada."]);
}

$conn->close();
?>