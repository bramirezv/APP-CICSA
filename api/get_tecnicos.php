<?php
header('Content-Type: application/json; charset=utf-8');
ob_start();
include 'db_config.php';

// Logging de depuración para ver qué llega al servidor
$logFile = 'debug_search.log';
$timestamp = date('Y-m-d H:i:s');

$query = isset($_GET['query']) ? trim($_GET['query']) : '';
file_put_contents($logFile, "[$timestamp] Query: '$query'\n", FILE_APPEND);

try {
    $data = [];
    
    if (empty($query)) {
        // Sugerir técnicos recientes si no hay búsqueda
        $sql = "SELECT id, nombre, dni, rol, zona FROM usuarios 
                WHERE (LOWER(rol) LIKE '%tecnico%' OR LOWER(rol) LIKE '%técnico%')
                ORDER BY id DESC LIMIT 10";
        $result = $conn->query($sql);
    } else {
        // Búsqueda robusta por nombre o DNI
        $searchTerm = "%$query%";
        $stmt = $conn->prepare("SELECT id, nombre, dni, rol, zona FROM usuarios 
                                WHERE (LOWER(rol) LIKE '%tecnico%' OR LOWER(rol) LIKE '%técnico%')
                                AND (nombre LIKE ? OR dni LIKE ? OR id = ?)
                                LIMIT 20");
        $idQuery = is_numeric($query) ? (int)$query : -1;
        $stmt->bind_param("ssi", $searchTerm, $searchTerm, $idQuery);
        $stmt->execute();
        $result = $stmt->get_result();
    }

    if ($result) {
        while ($row = $result->fetch_assoc()) {
            // Limpieza básica de datos
            $row['id'] = (int)$row['id'];
            $row['nombre'] = mb_convert_encoding($row['nombre'] ?? '', 'UTF-8', 'UTF-8');
            $row['zona'] = ($row['zona'] && $row['zona'] != '') ? $row['zona'] : 'SIN ZONA';
            $data[] = $row;
        }
    }

    ob_clean();
    echo json_encode([
        "status" => "success", 
        "count" => count($data),
        "data" => $data
    ]);

} catch (Exception $e) {
    ob_clean();
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => $e->getMessage()
    ]);
}
?>
