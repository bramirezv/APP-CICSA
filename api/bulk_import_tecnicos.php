<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

/**
 * Script para importación masiva de técnicos
 * Formato esperado: Nombre | DNI | Celular
 * (El separador puede ser | , o tabulación)
 */

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["status" => "error", "message" => "Solo se permite método POST"]);
    exit;
}

$rawData = $_POST['data'] ?? '';
if (empty($rawData)) {
    // Intentar leer desde el body JSON
    $json = json_decode(file_get_contents('php://input'), true);
    $rawData = $json['data'] ?? '';
}

if (empty($rawData)) {
    echo json_encode(["status" => "error", "message" => "No se proporcionaron datos para importar"]);
    exit;
}

$lines = explode("\n", str_replace("\r", "", $rawData));
$results = [
    "success" => 0,
    "duplicates" => 0,
    "errors" => 0,
    "details" => []
];

foreach ($lines as $line) {
    $line = trim($line);
    if (empty($line)) continue;

    // Intentar detectar separador (| , \t)
    $parts = [];
    if (strpos($line, '|') !== false) {
        $parts = explode('|', $line);
    } elseif (strpos($line, ',') !== false) {
        $parts = explode(',', $line);
    } else {
        $parts = preg_split('/\s{2,}/', $line); // Separados por 2 o más espacios
    }

    if (count($parts) < 2) {
        $results["errors"]++;
        $results["details"][] = "Línea inválida (faltan datos): $line";
        continue;
    }

    $nombre = trim($parts[0]);
    $dni = trim($parts[1]);
    $telefono = isset($parts[2]) ? trim($parts[2]) : "";
    
    // Validar nombre mínimo
    if (strlen($nombre) < 3) {
        $results["errors"]++;
        $results["details"][] = "Nombre muy corto: $nombre";
        continue;
    }

    // El password por defecto será el DNI
    $passHash = password_hash($dni, PASSWORD_DEFAULT);
    $rol = "tecnico";
    $zona = "SIN ZONA";

    // Verificar si ya existe (DNI o Teléfono si no está vacío)
    $sqlCheck = "SELECT id FROM usuarios WHERE (dni != '' AND dni = ?) OR (telefono != '' AND telefono = ?)";
    $stmtCheck = $conn->prepare($sqlCheck);
    $stmtCheck->bind_param("ss", $dni, $telefono);
    $stmtCheck->execute();
    $resCheck = $stmtCheck->get_result();

    if ($resCheck->num_rows > 0) {
        $results["duplicates"]++;
        $results["details"][] = "Ya existe: $nombre ($dni)";
        $stmtCheck->close();
        continue;
    }
    $stmtCheck->close();

    // Insertar
    $sqlInsert = "INSERT INTO usuarios (nombre, dni, telefono, celular_personal, password, rol, zona) VALUES (?, ?, ?, ?, ?, ?, ?)";
    $stmtInsert = $conn->prepare($sqlInsert);
    $stmtInsert->bind_param("sssssss", $nombre, $dni, $telefono, $telefono, $passHash, $rol, $zona);

    if ($stmtInsert->execute()) {
        $results["success"]++;
    } else {
        $results["errors"]++;
        $results["details"][] = "Error al insertar $nombre: " . $conn->error;
    }
    $stmtInsert->close();
}

echo json_encode([
    "status" => "success",
    "summary" => [
        "total_procesados" => count($lines),
        "importados_ok" => $results["success"],
        "duplicados_omitidos" => $results["duplicates"],
        "errores" => $results["errors"]
    ],
    "details" => $results["details"]
]);

$conn->close();
?>
