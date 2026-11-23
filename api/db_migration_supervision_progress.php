<?php
include 'db_config.php';

// Añadir columnas para guardar el progreso de la supervisión
$sql = "ALTER TABLE usuarios 
        ADD COLUMN IF NOT EXISTS checklist_supervision_actual TEXT DEFAULT NULL,
        ADD COLUMN IF NOT EXISTS comentarios_supervision_actual TEXT DEFAULT NULL,
        ADD COLUMN IF NOT EXISTS fotos_supervision_actual TEXT DEFAULT NULL";

if ($conn->query($sql) === TRUE) {
    echo "Columnas de progreso de supervisión añadidas correctamente\n";
} else {
    echo "Error al añadir columnas: " . $conn->error . "\n";
}

$conn->close();
?>
