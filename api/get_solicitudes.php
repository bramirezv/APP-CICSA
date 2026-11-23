<?php
// ARCHIVO: get_solicitudes.php
// Lógica de Filtrado Focus + Feedback de Corrección
header("Content-Type: application/json");
include 'db_config.php';
$conn->query("SET time_zone = '-05:00';");

$zona_param = isset($_GET['zona']) ? $_GET['zona'] : '';
$userId = isset($_GET['userId']) ? intval($_GET['userId']) : 0;
$supervisor_param = isset($_GET['supervisor']) ? $_GET['supervisor'] : '';
$start_date = isset($_GET['start_date']) ? $_GET['start_date'] : '';
$end_date = isset($_GET['end_date']) ? $_GET['end_date'] : '';
$tipo_param = isset($_GET['tipo']) ? $_GET['tipo'] : 'normal';
$zona_param = $conn->real_escape_string($zona_param);
$supervisor_param = $conn->real_escape_string($supervisor_param);
$tipo_param = $conn->real_escape_string($tipo_param);
$rol_param = isset($_GET['rol']) ? strtolower(trim($conn->real_escape_string($_GET['rol']))) : '';

$where_clauses = array();

// Filtrar por tipo (normal o auxiliar)
$where_clauses[] = "tipo = '$tipo_param'";

// LÓGICA POR ROL
$is_admin = (strpos($rol_param, 'admin') !== false);
$is_supervisor = (strpos($rol_param, 'supervisor') !== false || $rol_param === 'usuario');
$is_satelite = ($rol_param === 'satelite');
$is_tecnico = ($rol_param === 'tecnico');

if ($rol_param === 'auxiliar') {
    // Auxiliar: Solo ve las plantillas que él mismo creó
    if (!empty($supervisor_param)) {
        $where_clauses[] = "nombre_satelite = '$supervisor_param'";
    }
} else if ($is_tecnico) {
    // Tecnico: Solo ve las solicitudes donde él es el técnico
    if (!empty($supervisor_param)) {
        $where_clauses[] = "tecnico = '$supervisor_param'";
    }
} else if ($is_satelite && $userId > 0) {
    // Satélite: Ver sus tareas asignadas o pendientes para tomar
    $where_clauses[] = "(id_satelite = $userId OR ((id_satelite IS NULL OR id_satelite = 0) AND estado IN ('pendiente', 'corregir')))";
} else {
    // Administradores y Supervisores
    if ($tipo_param === 'auxiliar') {
        // Un usuario con rol "admin" verá todo lo de la zona.
        // Un supervisor o usuario normal solo verá lo suyo.
        if ($is_admin) {
            if (!empty($zona_param) && $zona_param !== "SIN ZONA" && $zona_param !== "ADMIN") {
                $where_clauses[] = "zona = '$zona_param' AND estado IN ('pendiente', 'aceptada', 'por_validar')";
            }
        } else if ($userId > 0) {
            // SUPERVISOR / USUARIO: Filtro estricto personal
            $where_clauses[] = "(id_satelite = $userId OR nombre_supervisor = '$supervisor_param')";
        }
    } else {
        // FLUJO NORMAL
        if (!empty($zona_param) && $zona_param !== "SIN ZONA" && $zona_param !== "ADMIN") {
            $where_clauses[] = "zona = '$zona_param' AND estado = 'pendiente'";
        } elseif (!empty($supervisor_param)) {
            $where_clauses[] = "nombre_supervisor = '$supervisor_param'";
        }
    }
}

// Filtro por FOCO de Fecha (Start/End)
if (!empty($start_date) && !empty($end_date)) {
    $sd = $conn->real_escape_string($start_date);
    $ed = $conn->real_escape_string($end_date);
    $where_clauses[] = "fecha_creacion BETWEEN '$sd 00:00:00' AND '$ed 23:59:59'";
} else {
    // Sin filtro de fecha explícito: 
    // Para auxiliares, mostrar todo (o un rango amplio) según solicitud del usuario
    if ($tipo_param === 'auxiliar') {
        // Podríamos no poner restricción o poner una muy amplia
        // Por ahora, quitamos la restricción para que salga "todo" como pidió el usuario
    } else {
        // Para flujo normal, mantenemos la restricción del mes actual por rendimiento
        $where_clauses[] = "YEAR(fecha_creacion) = YEAR(CURDATE()) AND MONTH(fecha_creacion) = MONTH(CURDATE())";
    }
}

$where_sql = count($where_clauses) > 0 ? " WHERE " . implode(" AND ", $where_clauses) : "";

if ($tipo_param === 'auxiliar') {
    // Ordenar para que las rechazadas aparezcan al final de la lista
    $sql = "SELECT * FROM solicitudes $where_sql ORDER BY CASE WHEN LOWER(estado) = 'rechazada' THEN 1 ELSE 0 END, fecha_creacion DESC";
} else {
    $sql = "SELECT * FROM solicitudes $where_sql ORDER BY fecha_creacion DESC";
}

$result = $conn->query($sql);
if ($result && $result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $solicitudes[] = formatSolicitud($row);
    }
}

function formatSolicitud($row) {
    return array(
        "id" => (int)$row['id'],
        "sot" => $row['sot'],
        "tecnico" => $row['tecnico'],
        "motivo" => $row['motivo'],
        "material" => $row['material'],
        "coordenadas" => $row['coordenadas'],
        "nombre_supervisor" => $row['nombre_supervisor'],
        "zona" => $row['zona'],
        "estado" => $row['estado'],
        "foto_evidencia" => $row['foto_evidencia'] ?: "",
        "id_satelite" => isset($row['id_satelite']) ? (int)$row['id_satelite'] : 0,
        "nombre_satelite" => $row['nombre_satelite'] ?: "",
        "comentario_satelite" => $row['comentario_satelite'] ?: "",
        "fecha_creacion" => $row['fecha_creacion'],
        "fecha_aceptacion" => $row['fecha_aceptacion'] ?: null,
        "fecha_finalizacion" => $row['fecha_finalizacion'] ?: null,
        "tipo" => $row['tipo'] ?? 'normal'
    );
}

echo json_encode($solicitudes);
$conn->close();
?>
