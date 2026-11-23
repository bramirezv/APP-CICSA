<?php
/**
 * ARCHIVO: get_tops_performance.php
 * Retorna el ranking de los satélites con mayor número de solicitudes completadas.
 */
header("Content-Type: application/json");
include 'db_config.php';
$conn->query("SET time_zone = '-05:00';");

// Seleccionamos el conteo de servicios completados por satélite
$sql = "SELECT 
            id_satelite as id, 
            nombre_satelite as nombre, 
            COUNT(*) as total_completadas
        FROM solicitudes 
        WHERE estado = 'completada' 
          AND id_satelite IS NOT NULL 
          AND id_satelite != 0
        GROUP BY id_satelite 
        ORDER BY total_completadas DESC 
        LIMIT 10";

$result = $conn->query($sql);
$tops = array();

if ($result && $result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $tops[] = array(
            "id" => (int)$row['id'],
            "nombre" => $row['nombre'],
            "total" => (int)$row['total_completadas']
        );
    }
}

echo json_encode($tops);
$conn->close();
?>
