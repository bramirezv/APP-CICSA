<?php
// ARCHIVO: api/create_solicitud.php
header("Content-Type: application/json");
include 'db_config.php';
$conn->query("SET time_zone = '-05:00';");

$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data['sot']) || !isset($data['tecnico']) || !isset($data['motivo']) || !isset($data['zona'])) {
    echo json_encode(array("status" => "error", "message" => "Datos incompletos (SOT, Tecnico, Motivo y Zona son obligatorios)"));
    exit;
}

$sot = $conn->real_escape_string($data['sot']);
$tecnico = $conn->real_escape_string($data['tecnico']);
$motivo = $conn->real_escape_string($data['motivo']);
$material = $conn->real_escape_string($data['material'] ?: '');
$coordenadas = $conn->real_escape_string($data['coordenadas'] ?: '');
$nombre_supervisor = $conn->real_escape_string($data['nombre_supervisor'] ?: 'ADMIN');
$zona = $conn->real_escape_string($data['zona']);
$tipo = $conn->real_escape_string($data['tipo'] ?? 'normal');
$id_supervisor = isset($data['id_supervisor']) ? intval($data['id_supervisor']) : 0;
$nombre_supervisor_asignado = isset($data['nombre_supervisor_asignado']) ? $conn->real_escape_string($data['nombre_supervisor_asignado']) : null;

$estado = 'pendiente';
$parts_cols = ["sot", "tecnico", "motivo", "material", "coordenadas", "nombre_supervisor", "zona", "estado", "tipo", "fecha_creacion"];
$parts_vals = ["'$sot'", "'$tecnico'", "'$motivo'", "'$material'", "'$coordenadas'", "'$nombre_supervisor'", "'$zona'", "'$estado'", "'$tipo'", "NOW()"];

if ($tipo === 'auxiliar' && $id_supervisor > 0) {
    // El auxiliar selecciona supervisor al inicio
    $parts_cols[] = "id_satelite"; 
    $parts_vals[] = $id_supervisor;
    
    // Guardamos el nombre del auxiliar en nombre_satelite para no perderlo
    $parts_cols[] = "nombre_satelite";
    $parts_vals[] = "'$nombre_supervisor'";
    
    if ($nombre_supervisor_asignado) {
        $parts_vals[5] = "'$nombre_supervisor_asignado'"; // Sobreescribimos nombre_supervisor con el asignado
    }
}

$sql = "INSERT INTO solicitudes (" . implode(", ", $parts_cols) . ") VALUES (" . implode(", ", $parts_vals) . ")";

if ($conn->query($sql) === TRUE) {
    $newId = $conn->insert_id;
    
    // Para plantillas de auxiliar, no verificar satélites (van al supervisor)
    if ($tipo === 'auxiliar') {
        echo json_encode(array(
            "status" => "success",
            "message" => "Plantilla de asistencia enviada correctamente. El supervisor será notificado para verificar.",
            "id" => $newId,
            "satellite_status" => "auxiliar_sent",
            "total_satelites" => 0,
            "disponibles" => 0
        ));
    } else {
        // Verificar disponibilidad de satélites en la zona (flujo normal)
        $sqlSat = "SELECT u.id, u.nombre, u.estado,
                   (SELECT COUNT(*) FROM solicitudes s WHERE s.id_satelite = u.id AND s.estado IN ('aceptada','en_camino','llegado','corregir')) as tareas_activas
                   FROM usuarios u WHERE u.rol = 'satelite' AND u.zona = '$zona'";
        $resSat = $conn->query($sqlSat);
        
        $totalSatelites = 0;
        $disponibles = 0;
        $satelitesInfo = array();
        
        if ($resSat && $resSat->num_rows > 0) {
            while ($sat = $resSat->fetch_assoc()) {
                $totalSatelites++;
                $estaOcupado = ($sat['tareas_activas'] > 0) || ($sat['estado'] === 'Ocupado') || ($sat['estado'] === 'Almorzando') || ($sat['estado'] === 'No Disponible');
                if (!$estaOcupado) {
                    $disponibles++;
                }
                $satelitesInfo[] = array(
                    "nombre" => $sat['nombre'],
                    "ocupado" => $estaOcupado
                );
            }
        }
        
        if ($totalSatelites == 0) {
            $satellite_status = "no_satellites";
            $statusMsg = "Solicitud creada. No hay satélites asignados a la zona $zona. La solicitud quedará en espera.";
        } elseif ($disponibles == 0) {
            $satellite_status = "all_busy";
            $statusMsg = "Solicitud creada. Todos los satélites de la zona $zona están ocupados. Será notificado al primero que se libere.";
        } else {
            $satellite_status = "available";
            $statusMsg = "Solicitud enviada correctamente a la zona $zona ($disponibles satélite(s) disponible(s))";
        }
        
        echo json_encode(array(
            "status" => "success", 
            "message" => $statusMsg,
            "id" => $newId,
            "satellite_status" => $satellite_status,
            "total_satelites" => $totalSatelites,
            "disponibles" => $disponibles
        ));
    }
} else {
    echo json_encode(array("status" => "error", "message" => "Error al crear solicitud: " . $conn->error));
}

$conn->close();
?>
