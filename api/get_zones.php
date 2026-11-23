<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

$zones = array();

// Intentar detectar la columna de nombre automáticamente
$sql = "SELECT * FROM zonas LIMIT 1";
$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $row = $result->fetch_assoc();
    $columnName = "";
    
    // Buscar una columna que sea tipo texto
    foreach ($row as $key => $value) {
        if (is_string($value) && !is_numeric($value)) {
            $columnName = $key;
            break;
        }
    }

    if ($columnName) {
        $resAll = $conn->query("SELECT $columnName FROM zonas ORDER BY $columnName ASC");
        while($r = $resAll->fetch_assoc()) {
            $zones[] = $r[$columnName];
        }
    }
}

// Si falló la detección o está vacía, enviamos una lista base
if (empty($zones)) {
    $zones = ["SURCO", "SJM", "VES", "CHORRILLOS", "LURIN"];
}

echo json_encode($zones);
$conn->close();
?>
