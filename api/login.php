<?php
// ARCHIVO: api/login.php - LOGIN UNIVERSAL (App y Web)
header('Content-Type: application/json');
include 'db_config.php';
$conn->query("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS session_token VARCHAR(255) NULL");

$identifier = null;
$password = null;

// 1. Intentar leer datos de JSON (Dashboard Web)
$data = json_decode(file_get_contents("php://input"), true);
if ($data) {
    if (isset($data['phone'])) $identifier = $data['phone'];
    else if (isset($data['celular'])) $identifier = $data['celular'];
    else if (isset($data['email'])) $identifier = $data['email'];
    
    if (isset($data['password'])) $password = $data['password'];
}

// 2. Intentar leer datos de $_POST (App Android - Form URL Encoded)
if (!$identifier) {
    if (isset($_POST['phone'])) $identifier = $_POST['phone'];
    else if (isset($_POST['celular'])) $identifier = $_POST['celular'];
    else if (isset($_POST['email'])) $identifier = $_POST['email'];
}
if (!$password && isset($_POST['password'])) {
    $password = $_POST['password'];
}

// Validación de campos obligatorios
if (!$identifier || !$password) {
    echo json_encode(array(
        "status" => "error", 
        "message" => "Faltan datos obligatorios (Usuario/Celular o Contraseña)"
    ));
    exit;
}

$id_escaped = $conn->real_escape_string($identifier);

// Búsqueda flexible en la base de datos
$sql = "SELECT id, nombre, rol, password, zona FROM usuarios 
        WHERE email = '$id_escaped' 
        OR nombre = '$id_escaped' 
        OR celular_personal = '$id_escaped' 
        OR telefono = '$id_escaped' 
        OR dni = '$id_escaped'";

$result = $conn->query($sql);

if ($result->num_rows > 0) {
    $user = $result->fetch_assoc();
    if (password_verify($password, $user['password'])) {
        $newToken = bin2hex(random_bytes(16)); // Generar token único
        $userId = (int)$user['id'];
        $conn->query("UPDATE usuarios SET session_token = '$newToken' WHERE id = $userId");

        echo json_encode(array(
            "status" => "success",
            "message" => "Bienvenido",
            "userId" => $userId,
            "userName" => $user['nombre'],
            "rol" => $user['rol'],
            "zona" => $user['zona'] ?: "SIN ZONA",
            "sessionToken" => $newToken
        ));
    } else {
        echo json_encode(array("status" => "error", "message" => "Contraseña incorrecta"));
    }
} else {
    echo json_encode(array("status" => "error", "message" => "Usuario no encontrado"));
}

$conn->close();
?>
