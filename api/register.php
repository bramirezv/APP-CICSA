<?php
error_reporting(0);
ini_set("display_errors", 0);
header("Content-Type: application/json");
ob_start();

try {
    include "db_config.php";

    $data = json_decode(file_get_contents("php://input"), true);
    if (!$data) { $data = $_POST; }

    if (empty($data["name"]) || empty($data["password"])) {
        throw new Exception("Datos incompletos");
    }

    $name = $conn->real_escape_string($data["name"]);
    $dni = isset($data["dni"]) ? $conn->real_escape_string($data["dni"]) : "";
    $email = isset($data["email"]) ? $conn->real_escape_string($data["email"]) : "";
    $telefono = isset($data["telefono"]) ? $conn->real_escape_string($data["telefono"]) : "";
    $password = password_hash($data["password"], PASSWORD_DEFAULT);
    $rol = isset($data["rol"]) ? $conn->real_escape_string($data["rol"]) : "satelite";
    $zona = isset($data["zona"]) ? $conn->real_escape_string($data["zona"]) : "SIN ZONA";

    // Verificar existencia
    $sqlCheck = "SELECT id FROM usuarios WHERE 
                 (dni != \"\" AND dni = \"$dni\") OR 
                 (email != \"\" AND email = \"$email\") OR 
                 (telefono != \"\" AND telefono = \"$telefono\") OR 
                 nombre = \"$name\"";
    $resultCheck = $conn->query($sqlCheck);
    if ($resultCheck && $resultCheck->num_rows > 0) {
        throw new Exception("El usuario ya existe (DNI, Email, Tel o Nombre duplicado)");
    }

    // Insertar
    $sql = "INSERT INTO usuarios (nombre, dni, email, telefono, celular_personal, password, rol, zona) 
            VALUES (\"$name\", \"$dni\", \"$email\", \"$telefono\", \"$telefono\", \"$password\", \"$rol\", \"$zona\")";

    if ($conn->query($sql) === TRUE) {
        $newId = $conn->insert_id;
        echo json_encode(array("status" => "success", "message" => "REGISTRADO CON ID: $newId EN TABLA USUARIOS"));
    } else {
        $err1 = $conn->error;
        $sqlFallback = "INSERT INTO usuarios (nombre, dni, email, telefono, password, rol, zona) 
                        VALUES (\"$name\", \"$dni\", \"$email\", \"$telefono\", \"$password\", \"$rol\", \"$zona\")";
        if ($conn->query($sqlFallback) === TRUE) {
            $newId = $conn->insert_id;
            echo json_encode(array("status" => "success", "message" => "REGISTRADO (LEGACY) CON ID: $newId"));
        } else {
            throw new Exception("Error DB: " . $conn->error . " | Prev: " . $err1);
        }
    }

} catch (Exception $e) {
    echo json_encode(array("status" => "error", "message" => $e->getMessage()));
}

$output = ob_get_clean();
echo $output;
?>
