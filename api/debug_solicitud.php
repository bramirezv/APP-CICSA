<?php
include 'db_config.php';
header('Content-Type: text/plain');

echo "--- ULTIMA SOLICITUD AUXILIAR ---\n";
$result = $conn->query("SELECT id, nombre_supervisor, id_satelite, nombre_satelite, tipo FROM solicitudes WHERE tipo='auxiliar' ORDER BY id DESC LIMIT 2");
if ($result) {
    while($row = $result->fetch_assoc()) {
        print_r($row);
    }
} else {
    echo "Error: " . $conn->error;
}
?>
