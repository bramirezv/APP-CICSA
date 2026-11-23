<?php
include 'db_config.php';

// 1. Crear tabla si no existe
$sql = "CREATE TABLE IF NOT EXISTS zonas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
)";

if ($conn->query($sql) === TRUE) {
    echo "Tabla 'zonas' lista.<br>";
} else {
    die("Error creando tabla: " . $conn->error);
}

// 2. Insertar algunas zonas de prueba si la tabla está vacía
$check = $conn->query("SELECT id FROM zonas LIMIT 1");
if ($check->num_rows == 0) {
    $zonas_iniciales = [
        "SURCO", 
        "SAN JUAN DE MIRAFLORES", 
        "VILLA EL SALVADOR", 
        "CHORRILLOS", 
        "LURIN", 
        "LA MOLINA", 
        "ATE", 
        "SAN BORJA"
    ];

    foreach ($zonas_iniciales as $z) {
        $conn->query("INSERT IGNORE INTO zonas (nombre) VALUES ('$z')");
    }
    echo "Zonas iniciales insertadas correctamente.";
} else {
    echo "La tabla ya tenía datos, no se insertaron duplicados.";
}

$conn->close();
?>
