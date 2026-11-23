<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

// Asegurar zona horaria de Perú para CURDATE()
$conn->query("SET time_zone = '-05:00'");

$start_date = isset($_GET['start_date']) ? $_GET['start_date'] : '';
$end_date = isset($_GET['end_date']) ? $_GET['end_date'] : '';

$date_condition = "DATE(s.fecha_creacion) = CURDATE()";
if (!empty($start_date) && !empty($end_date)) {
    $sd = $conn->real_escape_string($start_date);
    $ed = $conn->real_escape_string($end_date);
    $date_condition = "s.fecha_creacion BETWEEN '$sd 00:00:00' AND '$ed 23:59:59'";
}

// Obtener estadísticas de supervisores para el periodo
$sql = "SELECT u.id as supervisor_id, u.nombre as supervisor_nombre, 
               COUNT(s.id) as total_supervisiones,
               GROUP_CONCAT(DISTINCT u2.nombre SEPARATOR ', ') as tecnicos_supervisados
        FROM usuarios u
        LEFT JOIN supervisiones s ON u.id = s.id_supervisor AND $date_condition
        LEFT JOIN usuarios u2 ON s.id_tecnico = u2.id
        WHERE u.rol = 'supervisor'
        GROUP BY u.id, u.nombre
        ORDER BY total_supervisiones DESC";

$result = $conn->query($sql);
$data = array();

if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $row['total_supervisiones'] = (int) $row['total_supervisiones'];
        $data[] = $row;
    }
}

echo json_encode(["status" => "success", "data" => $data]);
$conn->close();
?>
