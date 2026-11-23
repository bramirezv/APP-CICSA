<?php
// ARCHIVO: api/asistencias_schema.php
// Ejecutar desde el navegador para crear la tabla de asistencias
include 'db_config.php';
header('Content-Type: text/plain');

echo "--- CREACIÓN DE TABLA DE ASISTENCIAS ---\n";

$query = "CREATE TABLE IF NOT EXISTS asistencias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    evento VARCHAR(20) NOT NULL, -- 'INGRESO' o 'SALIDA'
    fecha DATE DEFAULT CURRENT_DATE,
    hora TIME DEFAULT CURRENT_TIME,
    latitud DECIMAL(10, 8) NULL,
    longitud DECIMAL(11, 8) NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

if ($conn->query($query) === TRUE) {
    echo "[OK] Tabla 'asistencias' creada o ya existía.\n";
} else {
    
    echo "[ERROR] Error al crear la tabla: " . $conn->error . "\n";
}

// También verificamos si la tabla usuarios tiene las columnas de ubicación y de reporte
$queries_usuarios = [
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS estado VARCHAR(20) DEFAULT 'No Disponible'",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS zona VARCHAR(50) NULL",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS dni VARCHAR(20) NULL",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS puesto VARCHAR(50) NULL",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS licencia VARCHAR(50) NULL",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS celular_personal VARCHAR(20) NULL",
    "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS distrito_nombre VARCHAR(100) NULL"
];

foreach ($queries_usuarios as $sql) {
    if ($conn->query($sql) === TRUE) {
        echo "[OK] Actualizado usuarios: $sql\n";
    } else {
        echo "[ERROR] Fallo en usuarios: " . $conn->error . "\n";
    }
}

if (isset($conn) && $conn) {
    $conn->close();
}
echo "\nProceso finalizado.";
?>
