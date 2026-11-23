<?php
/**
 * ARCHIVO: api/importar_satelites.php
 * Importa los satélites desde los datos extraídos de Satelites.xlsx
 */
header('Content-Type: text/plain; charset=utf-8');
include 'db_config.php';

$satelites = [
    ["dni" => "73024424", "nombre" => "RAMIREZ VICENTE BRYAN JAVIER"],
    ["dni" => "45101874", "nombre" => "HURTADO CASANCA NEIL JESUS"],
    ["dni" => "72707708", "nombre" => "RIMAC PARIONA MAYCOL MHAY"],
    ["dni" => "73229114", "nombre" => "HUAMAN ÑAHUI ANTHONY SEBASTIAN"],
    ["dni" => "44292020", "nombre" => "ROA CONDE FABIO GUIDO"],
    ["dni" => "71735182", "nombre" => "RAMOS HUALLPA JUAN CARLOS"],
    ["dni" => "47144706", "nombre" => "TELLO IZAGUIRRE JULIO CESAR"],
    ["dni" => "73059859", "nombre" => "AGUILAR PAZ RODOLFO CARLOS"],
    ["dni" => "3158240", "nombre" => "CARABALLO RODRIGUEZ MARIO JOSE"],
    ["dni" => "73358098", "nombre" => "MEDINA GUTIERREZ JOSE MIGUEL"],
    ["dni" => "73813685", "nombre" => "HUAMANI SAVICHE GIANCARLO"],
    ["dni" => "76607944", "nombre" => "LEON TASA OMAR ARTURO"],
    ["dni" => "73528666", "nombre" => "HUANACO ACHATA YOEL"],
    ["dni" => "60754914", "nombre" => "GONZALES CASTRO BRYAN ALFREDO"]
];

echo "--- INICIO DE IMPORTACIÓN DE SATÉLITES ---\n\n";

$passHash = password_hash("cicsa123", PASSWORD_DEFAULT);
$rol = "satelite";
$zona = "B"; // Almacen B

foreach ($satelites as $sat) {
    $nombre = $conn->real_escape_string($sat['nombre']);
    $dni = $conn->real_escape_string($sat['dni']);
    $email = strtolower(str_replace(' ', '.', $sat['nombre'])) . "@cicsa.com"; // Email generado para login

    // Verificar si ya existe
    $check = $conn->query("SELECT id FROM usuarios WHERE dni = '$dni' OR nombre = '$nombre'");
    if ($check->num_rows > 0) {
        echo "[!] Saltando: {$sat['nombre']} (Ya existe)\n";
        continue;
    }

    $sql = "INSERT INTO usuarios (nombre, email, password, rol, zona, dni, puesto, celular_personal, distrito_nombre) 
            VALUES ('$nombre', '$email', '$passHash', '$rol', '$zona', '$dni', 'SATELITE', '', 'DISTRITO B')";

    if ($conn->query($sql) === TRUE) {
        echo "[OK] Insertado: {$sat['nombre']} | Login: $email\n";
    } else {
        echo "[ERROR] Error en {$sat['nombre']}: " . $conn->error . "\n";
    }
}

echo "\n--- PROCESO FINALIZADO ---";
$conn->close();
?>
