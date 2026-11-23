<?php
include 'db_config.php';

echo "--- INICIANDO MIGRACIÓN DE SUPERVISIÓN ---\n";

// 1. Tabla de relación Supervisor - Técnicos
$sql1 = "CREATE TABLE IF NOT EXISTS supervisor_tecnicos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_supervisor INT NOT NULL,
    id_tecnico INT NOT NULL,
    fecha_asignacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (id_supervisor, id_tecnico)
)";

if ($conn->query($sql1) === TRUE) {
    echo "[OK] Tabla supervisor_tecnicos lista.\n";
} else {
    echo "[ERROR] " . $conn->error . "\n";
}

// 2. Tabla de Reportes de Supervisión
$sql2 = "CREATE TABLE IF NOT EXISTS supervisiones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_supervisor INT NOT NULL,
    id_tecnico INT NOT NULL,
    sot_referencia VARCHAR(50) NULL,
    comentarios TEXT NULL,
    foto_url TEXT NULL,
    latitud DECIMAL(10, 8) NULL,
    longitud DECIMAL(11, 8) NULL,
    puntuacion INT DEFAULT 0,
    checklist_json TEXT NULL,
    fecha_supervision DATETIME DEFAULT CURRENT_TIMESTAMP
)";

if ($conn->query($sql2) === TRUE) {
    echo "[OK] Tabla supervisiones lista.\n";
} else {
    echo "[ERROR] " . $conn->error . "\n";
}

$conn->close();
echo "--- MIGRACIÓN COMPLETADA ---\n";
?>