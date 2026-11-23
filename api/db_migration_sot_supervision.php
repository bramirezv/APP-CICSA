<?php
include 'db_config.php';

// Añadir columna para guardar la SOT de la supervisión en curso
$sql = "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS sot_supervision_actual VARCHAR(50) DEFAULT NULL";
if ($conn->query($sql) === TRUE) {
    echo "Columna sot_supervision_actual añadida correctamente\n";
} else {
    echo "Error al añadir columna: " . $conn->error . "\n";
}

$conn->close();
?>
