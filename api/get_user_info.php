<?php
header('Content-Type: application/json; charset=utf-8');
include 'auth_check.php';

// Asegurar zona horaria de Perú para CURDATE()
$conn->query("SET time_zone = '-05:00'");

$json = file_get_contents('php://input');
$data = json_decode($json, true);
$userId = isset($_GET['userId']) ? $_GET['userId'] : (isset($_GET['id']) ? $_GET['id'] : (isset($data['id']) ? $data['id'] : (isset($data['userId']) ? $data['userId'] : null)));
$sessionToken = isset($_GET['session_token']) ? $_GET['session_token'] : (isset($data['session_token']) ? $data['session_token'] : null);

if (!$userId) {
    echo json_encode(["status" => "error", "message" => "Falta el ID del usuario"]);
    exit;
}

// Validar Sesión Única
validateSession($conn, $userId, $sessionToken);

// CONSULTA ACTUALIZADA con las nuevas columnas
$sql = "SELECT id, nombre, telefono, rol, zona, estado, 
        ultima_conexion, ultima_ubicacion, disponible, en_almuerzo, 
        inicio_almuerzo, latitud AS lat, longitud AS lng, distrito_id, foto_perfil, 
        ruta_activa, ultima_activacion, 
        email, direccion, fecha_nacimiento, telefono_emergencia,
        almuerzo_usado 
        FROM usuarios WHERE id = ?";

$stmt = $conn->prepare($sql);

if ($stmt) {
    $stmt->bind_param("i", $userId);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($user = $result->fetch_assoc()) {
        $user['created_at'] = $user['ultima_conexion'];
        $user['ultimo_acceso'] = $user['ultima_conexion'];

        date_default_timezone_set('America/Lima');
        // Resetear automáticamente el estado de almuerzo si es un nuevo día
        if ($user['almuerzo_usado'] == 1 && !empty($user['inicio_almuerzo'])) {
            $inicio_almuerzo_date = date('Y-m-d', strtotime($user['inicio_almuerzo']));
            $current_date = date('Y-m-d');
            if ($inicio_almuerzo_date < $current_date) {
                $conn->query("UPDATE usuarios SET almuerzo_usado = 0 WHERE id = " . intval($user['id']));
                $user['almuerzo_usado'] = 0;
            }
        }

        // Si es supervisor, contar supervisiones de hoy
        // Contar supervisiones de hoy para este usuario (supervisor)
        $hoy = date('Y-m-d');
        $fechaLike = $hoy . '%';
        $sqlSup = "SELECT COUNT(*) as total FROM supervisiones WHERE id_supervisor = ? AND fecha_creacion LIKE ?";
        $stmtSup = $conn->prepare($sqlSup);
        if ($stmtSup) {
            $stmtSup->bind_param("is", $userId, $fechaLike);
            $stmtSup->execute();
            $resSup = $stmtSup->get_result();
            $rowSup = $resSup->fetch_assoc();
            $user['supervisiones_hoy'] = intval($rowSup['total']);
            $stmtSup->close();
        } else {
            $user['supervisiones_hoy'] = 0;
        }

        // Si es supervisor, buscar si tiene una supervisión activa en curso
        $sqlAct = "SELECT id, nombre, sot_supervision_actual, inicio_supervision_actual, 
                          checklist_supervision_actual, comentarios_supervision_actual, fotos_supervision_actual 
                   FROM usuarios 
                   WHERE id_supervisor_actual = ? AND en_supervision = 1 
                   LIMIT 1";
        $stmtAct = $conn->prepare($sqlAct);
        if ($stmtAct) {
            $stmtAct->bind_param("i", $userId);
            $stmtAct->execute();
            $resAct = $stmtAct->get_result();
            if ($rowAct = $resAct->fetch_assoc()) {
                $user['supervision_activa'] = [
                    "tecnico_id" => intval($rowAct['id']),
                    "tecnico_nombre" => $rowAct['nombre'],
                    "sot" => $rowAct['sot_supervision_actual'],
                    "inicio" => $rowAct['inicio_supervision_actual'],
                    "checklist" => $rowAct['checklist_supervision_actual'],
                    "comentarios" => $rowAct['comentarios_supervision_actual'],
                    "fotos" => $rowAct['fotos_supervision_actual']
                ];
            } else {
                $user['supervision_activa'] = null;
            }
            $stmtAct->close();
        }

        echo json_encode([
            "status" => "success",
            "data" => $user
        ]);
    } else {
        echo json_encode(["status" => "error", "message" => "Usuario no encontrado"]);
    }
    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Error en DB: " . $conn->error]);
}

$conn->close();
?>