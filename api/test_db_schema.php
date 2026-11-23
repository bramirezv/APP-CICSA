<?php
include 'db_config.php';
header('Content-Type: text/plain');

echo "--- COLUMNAS DE LA TABLA SOLICITUDES ---\n";
$res = $conn->query("SHOW COLUMNS FROM solicitudes");
if ($res) {
    while($row = $res->fetch_assoc()) {
        echo $row['Field'] . " (" . $row['Type'] . ")\n";
    }
} else {
    echo "Error al obtener columnas: " . $conn->error;
}

echo "\n--- COLUMNAS DE LA TABLA USUARIOS ---\n";
$res = $conn->query("SHOW COLUMNS FROM usuarios");
if ($res) {
    while($row = $res->fetch_assoc()) {
        echo $row['Field'] . " (" . $row['Type'] . ")\n";
    }
} else {
    echo "Error al obtener columnas: " . $conn->error;
}
$conn->close();
?>
