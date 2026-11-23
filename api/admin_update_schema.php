<?php
// ARCHIVO: admin_update_schema.php
// Ejecutar UNA SOLA VEZ desde el navegador para añadir columnas de tiempo
include 'db_config.php';
header('Content-Type: text/plain');

echo "--- ACTUALIZACIÓN DE SCHEMA: USUARIOS Y SOLICITUDES ---\n";

$queries = [
    "ALTER TABLE solicitudes ADD COLUMN IF NOT EXISTS fecha_aceptacion DATETIME NULL AFTER fecha_creacion",
    "ALTER TABLE solicitudes ADD COLUMN IF NOT EXISTS fecha_finalizacion DATETIME NULL AFTER fecha_aceptacion",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS latitud DECIMAL(10, 8) NULL",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS longitud DECIMAL(11, 8) NULL",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultima_ubicacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
];

foreach ($queries as $sql) {
    if ($conn) {
        $qResult = $conn->query($sql);
        if ($qResult === TRUE) {
            echo "[OK] Consulta ejecutada: $sql\n";
        } else {
            echo "[ERROR] Fallo en consulta: " . ($conn->error ?: "Error desconocido") . "\n";
        }
    } else {
        echo "[ERROR] Fallo: No hay conexión a la base de datos.\n";
    }
}

if (isset($conn) && $conn) {
    $conn->close();
}
echo "\nActualización completada.";
?>
