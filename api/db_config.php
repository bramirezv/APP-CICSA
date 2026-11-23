<?php
// ARCHIVO: db_config.php
// Configuración de conexión a la base de datos (Ejemplo para Hostinger)

// --- SOPORTE PARA CORS (Dashboard Web) ---
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit;
}

$host = 'localhost'; // O la IP de tu servidor MySQL
$user = 'TU_USUARIO_DB';
$pass = 'TU_PASSWORD_DB';
$dbname = 'TU_NOMBRE_DB';

$conn = new mysqli($host, $user, $pass, $dbname);

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Fallo de conexión: " . $conn->connect_error]));
}

// Asegurar que usemos UTF-8 y zona horaria correcta
$conn->set_charset("utf8mb4");
$conn->query("SET time_zone = '-05:00'");
?>
