<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header('Content-Type: text/plain');

include 'db_config.php';

echo "--- DEBUG TNPS ---\n";

// 1. Verificar Conexión
if ($conn->connect_error) {
    die("Conexión fallida: " . $conn->connect_error);
}
echo "[OK] Conexión a DB establecida.\n";

// 2. Verificar Tabla
$result = $conn->query("SHOW TABLES LIKE 'tnps_evidencia'");
if ($result->num_rows > 0) {
    echo "[OK] La tabla 'tnps_evidencia' existe.\n";
} else {
    echo "[!] La tabla 'tnps_evidencia' NO EXISTE. Ejecutando migración...\n";
    $sql = "CREATE TABLE tnps_evidencia (
        id INT AUTO_INCREMENT PRIMARY KEY,
        id_tecnico INT NOT NULL,
        nombre_tecnico VARCHAR(255) NOT NULL,
        sot VARCHAR(50) NULL,
        foto_url TEXT NOT NULL,
        comentario TEXT NULL,
        fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
        latitud DECIMAL(10, 8) NULL,
        longitud DECIMAL(11, 8) NULL
    )";
    if ($conn->query($sql) === TRUE) {
        echo "[OK] Tabla creada exitosamente.\n";
    } else {
        echo "[ERROR] Error al crear tabla: " . $conn->error . "\n";
    }
}

// 3. Verificar Directorio de Cargas
$target_dir = "uploads/tnps/";
if (!file_exists($target_dir)) {
    if (mkdir($target_dir, 0777, true)) {
        echo "[OK] Directorio '$target_dir' creado.\n";
    } else {
        echo "[ERROR] No se pudo crear el directorio '$target_dir'.\n";
    }
} else {
    echo "[OK] El directorio '$target_dir' ya existe.\n";
    if (is_writable($target_dir)) {
        echo "[OK] El directorio es escribible.\n";
    } else {
        echo "[ERROR] El directorio NO es escribible.\n";
    }
}

// 4. Verificar configuración de PHP para subidas
echo "upload_max_filesize: " . ini_get('upload_max_filesize') . "\n";
echo "post_max_size: " . ini_get('post_max_size') . "\n";

$conn->close();
?>
