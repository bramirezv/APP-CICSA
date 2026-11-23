<?php
include 'db_config.php';
header('Content-Type: text/plain');

echo "--- CREACIÓN DE TABLA TNPS_EVIDENCIA ---\n";

$sql = "CREATE TABLE IF NOT EXISTS tnps_evidencia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_tecnico INT NOT NULL,
    nombre_tecnico VARCHAR(255) NOT NULL,
    sot VARCHAR(50) NULL,
    foto_url TEXT NOT NULL,
    comentario TEXT NULL,
    id_supervisor INT NULL,
    nombre_supervisor VARCHAR(255) NULL,
    estado_validacion VARCHAR(20) DEFAULT 'pendiente',
    comentario_supervisor TEXT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    latitud DECIMAL(10, 8) NULL,
    longitud DECIMAL(11, 8) NULL
)";

if ($conn->query($sql) === TRUE) {
    echo "[OK] Tabla tnps_evidencia creada o ya existe.\n";
} else {
    echo "[ERROR] Error al crear tabla: " . $conn->error . "\n";
}

$alters = [
    "ALTER TABLE tnps_evidencia ADD COLUMN IF NOT EXISTS id_supervisor INT NULL;",
    "ALTER TABLE tnps_evidencia ADD COLUMN IF NOT EXISTS nombre_supervisor VARCHAR(255) NULL;",
    "ALTER TABLE tnps_evidencia ADD COLUMN IF NOT EXISTS estado_validacion VARCHAR(20) DEFAULT 'pendiente';",
    "ALTER TABLE tnps_evidencia ADD COLUMN IF NOT EXISTS comentario_supervisor TEXT NULL;"
];

foreach ($alters as $alter) {
    if ($conn->query($alter) === TRUE) {
        echo "[OK] Columna verificada/agregada.\n";
    } else {
        echo "[ERROR] Error al alterar tabla: " . $conn->error . "\n";
    }
}

$conn->close();
?>
