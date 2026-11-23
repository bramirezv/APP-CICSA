<?php
header('Content-Type: application/json; charset=utf-8');
include 'db_config.php';

// Asegurar que existan las columnas en la tabla usuarios
$conn->query("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS en_supervision INT DEFAULT 0");
$conn->query("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS id_supervisor_actual INT DEFAULT NULL");
$conn->query("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS inicio_supervision_actual DATETIME DEFAULT NULL");
$conn->query("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS sot_supervision_actual VARCHAR(50) DEFAULT NULL");
$conn->query("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS checklist_supervision_actual TEXT DEFAULT NULL");
$conn->query("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS comentarios_supervision_actual TEXT DEFAULT NULL");
$conn->query("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fotos_supervision_actual TEXT DEFAULT NULL");

$id_supervisor = $_POST['id_supervisor'] ?? 0;
$id_tecnico = $_POST['id_tecnico'] ?? 0;
$sot = $_POST['sot'] ?? '';
$session_token = $_POST['session_token'] ?? '';

if ($id_tecnico <= 0 || $id_supervisor <= 0) {
    echo json_encode(["status" => "error", "message" => "Datos incompletos"]);
    exit;
}

// 0. VALIDACIÓN DE SESIÓN ÚNICA
$stmt_session = $conn->prepare("SELECT session_token FROM usuarios WHERE id = ?");
$stmt_session->bind_param("i", $id_supervisor);
$stmt_session->execute();
$res_session = $stmt_session->get_result();
if ($row_session = $res_session->fetch_assoc()) {
    $db_token = $row_session['session_token'];
    if (!empty($db_token) && $db_token !== $session_token) {
        echo json_encode(["status" => "error", "message" => "Sesión iniciada en otro dispositivo. Por favor reinicie la App."]);
        exit;
    }
}
$stmt_session->close();

// 1. Verificar si el técnico ya está siendo supervisado
$stmt = $conn->prepare("SELECT u.en_supervision, u.id_supervisor_actual, u.inicio_supervision_actual, s.nombre as supervisor_nombre 
                        FROM usuarios u 
                        LEFT JOIN usuarios s ON u.id_supervisor_actual = s.id
                        WHERE u.id = ?");
$stmt->bind_param("i", $id_tecnico);
$stmt->execute();
$res = $stmt->get_result();

if ($row = $res->fetch_assoc()) {
    if ($row['en_supervision'] == 1) {
        // Permitir si es el MISMO supervisor quien intenta re-iniciar/recuperar
        if ($row['id_supervisor_actual'] == $id_supervisor) {
            // Ya es suyo, permitimos continuar
        } else {
            // Verificar si la supervisión ha expirado (más de 2 horas)
            $inicio = strtotime($row['inicio_supervision_actual']);
            $ahora = time();
            if (($ahora - $inicio) < 7200) { // Menos de 2 horas
                $nombre_sup = $row['supervisor_nombre'] ?: "otro supervisor";
                echo json_encode([
                    "status" => "error", 
                    "message" => "Este técnico ya está siendo supervisado por: $nombre_sup. Por favor elija otro técnico."
                ]);
                exit;
            }
        }
    }
}

// 2. Marcar como "En Supervisión"
$stmt_upd = $conn->prepare("UPDATE usuarios SET en_supervision = 1, id_supervisor_actual = ?, inicio_supervision_actual = NOW(), sot_supervision_actual = ?, checklist_supervision_actual = NULL, comentarios_supervision_actual = NULL, fotos_supervision_actual = NULL WHERE id = ?");
if (!$stmt_upd) {
    echo json_encode(["status" => "error", "message" => "Error preparando UPDATE: " . $conn->error]);
    exit;
}
$stmt_upd->bind_param("isi", $id_supervisor, $sot, $id_tecnico);

if ($stmt_upd->execute()) {
    echo json_encode(["status" => "success", "message" => "Supervisión iniciada correctamente"]);
} else {
    echo json_encode(["status" => "error", "message" => "Error al ejecutar UPDATE: " . $stmt_upd->error]);
}

$conn->close();
?>
