<?php
// ARCHIVO: migrar_comentarios.php
// Añadir el campo de comentario de corrección a la tabla solicitudes
include 'db_config.php';
header('Content-Type: text/plain');

echo "--- INICIANDO MIGRACION DE COMENTARIOS ---\n\n";

// Añadir la columna comentario_satelite si no existe
$sql = "ALTER TABLE solicitudes ADD COLUMN comentario_satelite TEXT DEFAULT NULL AFTER nombre_satelite";

if ($conn->query($sql) === TRUE) {
    echo "[OK] Columna 'comentario_satelite' creada exitosamente.\n";
    echo "Ya puedes usar el sistema de feedback entre Satélite y Supervisor.";
} else {
    echo "[ERROR/INFO] " . $conn->error . "\n";
    echo "Si el error dice 'Duplicate column', ignóralo, ya estaba creada.";
}

$conn->close();
?>
