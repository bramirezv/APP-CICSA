<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';
$conn->query("SET time_zone = '-05:00';");

// Auto-migración y reparación por si acaso
$sqlTable = "CREATE TABLE IF NOT EXISTS supervisiones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_supervisor INT NOT NULL,
    id_tecnico INT NOT NULL,
    sot VARCHAR(50),
    comentarios TEXT,
    latitud VARCHAR(50),
    longitud VARCHAR(50),
    puntuacion INT,
    checklist TEXT,
    evidencia TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)";
$conn->query($sqlTable);

// Asegurar columnas (Compatibilidad con db_migration_supervision.php)
$conn->query("ALTER TABLE supervisiones ADD COLUMN IF NOT EXISTS sot VARCHAR(50) AFTER id_tecnico");
$conn->query("ALTER TABLE supervisiones ADD COLUMN IF NOT EXISTS evidencia TEXT");
$conn->query("ALTER TABLE supervisiones ADD COLUMN IF NOT EXISTS checklist TEXT");
$conn->query("ALTER TABLE supervisiones ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

$start_date = isset($_GET['start_date']) ? $_GET['start_date'] : '';
$end_date = isset($_GET['end_date']) ? $_GET['end_date'] : '';

$where_sql = "";
if (!empty($start_date) && !empty($end_date)) {
    $sd = $conn->real_escape_string($start_date);
    $ed = $conn->real_escape_string($end_date);
    $where_sql = " WHERE s.fecha_creacion BETWEEN '$sd 00:00:00' AND '$ed 23:59:59' ";
}

$sql = "SELECT s.*, 
               u1.nombre as nombre_supervisor, 
               u2.nombre as nombre_tecnico
        FROM supervisiones s
        INNER JOIN usuarios u1 ON s.id_supervisor = u1.id
        INNER JOIN usuarios u2 ON s.id_tecnico = u2.id
        $where_sql
        ORDER BY s.fecha_creacion DESC";

$result = $conn->query($sql);
$data = array();

if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        // Formatear checklist si es JSON
        $row['checklist'] = json_decode($row['checklist'], true);

        // Convertir string de evidencias a array de URLs completas
        if (!empty($row['evidencia'])) {
            $fotos = explode(',', $row['evidencia']);
            $urls = array();
            foreach ($fotos as $f) {
                $urls[] = "https://" . $_SERVER['HTTP_HOST'] . "/api/uploads/supervisiones/" . trim($f);
            }
            $row['fotos_urls'] = $urls;
        } else {
            $row['fotos_urls'] = array();
        }

        $data[] = $row;
    }
}

echo json_encode(["status" => "success", "data" => $data]);
$conn->close();
?>