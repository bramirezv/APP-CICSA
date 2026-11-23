<?php
// ARCHIVO: api/get_ticket_tracking.php
// Retorna la ubicación en tiempo real del satélite asignado a un ticket.
// Usado por el supervisor para el mini-mapa estilo Uber.
header('Content-Type: application/json');
include 'db_config.php';
$conn->query("SET time_zone = '-05:00';");

$id_solicitud = isset($_GET['id_solicitud']) ? intval($_GET['id_solicitud']) : 0;

if ($id_solicitud <= 0) {
    echo json_encode(["status" => "error", "message" => "ID de solicitud requerido"]);
    exit;
}

// Obtener datos del ticket + ubicación del satélite asignado
$sql = "SELECT 
            s.id,
            s.sot,
            s.tecnico,
            s.motivo,
            s.zona,
            s.estado,
            s.coordenadas,
            s.nombre_satelite,
            s.id_satelite,
            s.fecha_aceptacion,
            u.nombre AS satelite_nombre_completo,
            u.latitud AS satelite_lat,
            u.longitud AS satelite_lng,
            u.ultima_ubicacion AS satelite_ultima_ubicacion,
            u.estado AS satelite_estado
        FROM solicitudes s
        LEFT JOIN usuarios u ON s.id_satelite = u.id
        WHERE s.id = ?";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    echo json_encode(["status" => "error", "message" => "Error DB: " . $conn->error]);
    exit;
}

$stmt->bind_param("i", $id_solicitud);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    // Parsear coordenadas del ticket (destino) - pueden ser "lat,lng" o un link de Maps
    $dest_lat = null;
    $dest_lng = null;
    $coordenadas = trim($row['coordenadas'] ?? '');
    
    // 1. Si es un enlace acortado de Google Maps, intentar resolver la redirección para obtener las coordenadas reales
    if (stripos($coordenadas, 'maps.app.goo.gl') !== false || stripos($coordenadas, 'goo.gl/maps') !== false) {
        if (function_exists('curl_init')) {
            $ch = curl_init();
            curl_setopt($ch, CURLOPT_URL, $coordenadas);
            curl_setopt($ch, CURLOPT_HEADER, true);
            curl_setopt($ch, CURLOPT_FOLLOWLOCATION, false);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_TIMEOUT, 3);
            curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)');
            $response = curl_exec($ch);
            $info = curl_getinfo($ch);
            curl_close($ch);
            
            if ($info['http_code'] == 301 || $info['http_code'] == 302) {
                if (preg_match('/Location:\s*([^\s\r\n]+)/i', $response, $locMatches)) {
                    $coordenadas = urldecode($locMatches[1]);
                }
            }
        }
    }
    
    // 2. Intentar parsear "lat,lng" directamente (permitiendo espacios antes/después del coma)
    if (preg_match('/^(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)$/', $coordenadas, $matches)) {
        $dest_lat = floatval($matches[1]);
        $dest_lng = floatval($matches[2]);
    }
    // 3. Intentar extraer de link de Google Maps (@lat,lng)
    elseif (preg_match('/@(-?\d+\.?\d*),(-?\d+\.?\d*)/', $coordenadas, $matches)) {
        $dest_lat = floatval($matches[1]);
        $dest_lng = floatval($matches[2]);
    }
    // 4. Intentar extraer de link de Google Maps (?q=lat,lng o &q=lat,lng)
    elseif (preg_match('/[?&]q=(-?\d+\.?\d*),(-?\d+\.?\d*)/', $coordenadas, $matches)) {
        $dest_lat = floatval($matches[1]);
        $dest_lng = floatval($matches[2]);
    }

    // Seguridad contra Null Island (coordenadas 0.0)
    if ($dest_lat == 0.0 || $dest_lng == 0.0) {
        $dest_lat = null;
        $dest_lng = null;
    }

    // Calcular ETA estimado (solo si tenemos ambas ubicaciones)
    $eta_minutos = null;
    if ($dest_lat && $dest_lng && $row['satelite_lat'] && $row['satelite_lng']) {
        // Fórmula de Haversine para distancia aproximada
        $R = 6371; // Radio de la Tierra en km
        $lat1 = deg2rad(floatval($row['satelite_lat']));
        $lat2 = deg2rad($dest_lat);
        $dlat = deg2rad($dest_lat - floatval($row['satelite_lat']));
        $dlng = deg2rad($dest_lng - floatval($row['satelite_lng']));
        $a = sin($dlat/2)*sin($dlat/2) + cos($lat1)*cos($lat2)*sin($dlng/2)*sin($dlng/2);
        $c = 2*atan2(sqrt($a), sqrt(1-$a));
        $distancia_km = $R * $c;
        // Velocidad promedio en ciudad: ~30 km/h
        $eta_minutos = round(($distancia_km / 30) * 60);
        if ($eta_minutos < 1) $eta_minutos = 1;
        if ($eta_minutos > 120) $eta_minutos = null; // Si es muy lejos, no mostrar ETA
    }

    echo json_encode([
        "status"          => "success",
        "ticket" => [
            "id"           => intval($row['id']),
            "sot"          => $row['sot'],
            "tecnico"      => $row['tecnico'],
            "motivo"       => $row['motivo'],
            "zona"         => $row['zona'],
            "estado"       => $row['estado'],
            "coordenadas"  => $row['coordenadas'],
            "dest_lat"     => $dest_lat,
            "dest_lng"     => $dest_lng,
        ],
        "satelite" => [
            "id"               => intval($row['id_satelite'] ?? 0),
            "nombre"           => $row['satelite_nombre_completo'] ?? $row['nombre_satelite'],
            "lat"              => $row['satelite_lat'] ? floatval($row['satelite_lat']) : null,
            "lng"              => $row['satelite_lng'] ? floatval($row['satelite_lng']) : null,
            "ultima_ubicacion" => $row['satelite_ultima_ubicacion'],
            "estado"           => $row['satelite_estado'],
        ],
        "eta_minutos" => $eta_minutos,
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Ticket no encontrado"]);
}

$stmt->close();
$conn->close();
?>
