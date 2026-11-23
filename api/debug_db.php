<?php
include 'db_config.php';
header('Content-Type: text/plain');

echo "--- ESTRUCTURA DE TABLA USUARIOS ---\n";
$result = $conn->query("DESCRIBE usuarios");
if ($result) {
    while($row = $result->fetch_assoc()) {
        echo $row['Field'] . " - " . $row['Type'] . "\n";
    }
} else {
    echo "Error: " . $conn->error;
}

echo "\n--- MUESTRA DE USUARIOS (Primeros 5) ---\n";
$result = $conn->query("SELECT id, nombre, email, celular_personal, dni, rol FROM usuarios LIMIT 5");
if ($result) {
    while($row = $result->fetch_assoc()) {
        print_r($row);
    }
} else {
    echo "Error: " . $conn->error;
}
$conn->close();
?>
