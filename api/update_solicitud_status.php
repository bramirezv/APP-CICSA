<?php
// ARCHIVO: update_solicitud_status.php
// Versión Estable con Soporte para Comentarios de Corrección
header('Content-Type: application/json');
include 'db_config.php';
$conn->query("SET time_zone = '-05:00';");

$json = file_get_contents("php://input");
$data = json_decode($json, true);

if (!$data || !isset($data['id']) || !isset($data['nuevo_estado'])) {
    echo json_encode(array("status" => "error", "message" => "Datos incompletos"));
    exit;
}

$id = intval($data['id']);
$estado = $conn->real_escape_string($data['nuevo_estado']);
$idUsuario = isset($data['id_usuario']) ? intval($data['id_usuario']) : 0;
$comentario = isset($data['comentario']) ? $conn->real_escape_string($data['comentario']) : null;

if ($estado == "aceptada") {
    if ($idUsuario <= 0) {
        echo json_encode(array("status" => "error", "message" => "ID Usuario requerido"));
        exit;
    }
    
    $nombreSatelite = "Satelite";
    $uQ = "SELECT nombre FROM usuarios WHERE id = $idUsuario LIMIT 1";
    $uR = $conn->query($uQ);
    if ($uR && $uR->num_rows > 0) {
        $uRow = $uR->fetch_assoc();
        $nombreSatelite = $conn->real_escape_string($uRow['nombre']);
    }

    // Soportar reasignación manual (Si no está pendiente, se sobreescribe el usuario)
    $sql = "UPDATE solicitudes SET 
                estado = 'aceptada', 
                id_satelite = $idUsuario, 
                nombre_satelite = '$nombreSatelite',
                comentario_satelite = NULL,
                fecha_aceptacion = IFNULL(fecha_aceptacion, NOW()),
                fecha_finalizacion = NULL 
            WHERE id = $id";
    
    if ($conn) {
        $qResult = $conn->query($sql);
        if ($qResult === TRUE) {
            echo json_encode(array("status" => "success", "message" => "Asignada correctamente a $nombreSatelite"));
        } else {
            echo json_encode(array("status" => "error", "message" => "Fallo SQL: " . $conn->error));
        }
    }
} else {
    // Otros estados
    $parts = ["estado = '$estado'"];
    if ($idUsuario > 0) { 
        $parts[] = "id_satelite = $idUsuario"; 
        
        // Si es por_validar, también actualizamos el nombre_supervisor para que salga en la UI del auxiliar
        if ($estado == "por_validar") {
            $uQ = "SELECT nombre FROM usuarios WHERE id = $idUsuario LIMIT 1";
            $uR = $conn->query($uQ);
            if ($uR && $uR->num_rows > 0) {
                $uRow = $uR->fetch_assoc();
                $nombreSup = $conn->real_escape_string($uRow['nombre']);
                $parts[] = "nombre_supervisor = '$nombreSup'";
            }
        }
    }
    
    // Si es CORREGIR o RECHAZADA, guardamos el comentario
    if (($estado == "corregir" || $estado == "rechazada") && $comentario !== null) {
        $parts[] = "comentario_satelite = '$comentario'";
    } 
    
    // Si vuelve a PENDIENTE (cuando el supervisor corrige), limpiamos el comentario
    if ($estado == "pendiente") {
        $parts[] = "comentario_satelite = NULL";
        $parts[] = "id_satelite = NULL";
        $parts[] = "nombre_satelite = NULL";
        $parts[] = "fecha_aceptacion = NULL";
        $parts[] = "fecha_finalizacion = NULL";
    }

    // REGISTRO DE TIEMPOS
    if ($estado == "completada") {
        $parts[] = "fecha_finalizacion = NOW()";
    }
    if ($estado == "aceptada" || $estado == "en_camino") {
       // Nos aseguramos que tenga fecha de aceptación si no la tenía
       $parts[] = "fecha_aceptacion = IFNULL(fecha_aceptacion, NOW())";
    }

    $sql = "UPDATE solicitudes SET " . implode(", ", $parts) . " WHERE id = $id";
    if ($conn) {
        $qResult = $conn->query($sql);
        if ($qResult === TRUE) {
            echo json_encode(array("status" => "success", "message" => "Estado: $estado"));
        } else {
            echo json_encode(array("status" => "error", "message" => "Fallo BD: " . $conn->error));
        }
    } else {
        echo json_encode(array("status" => "error", "message" => "Fallo de conexión"));
    }
}

if (isset($conn) && $conn) {
    $conn->close();
}
?>
