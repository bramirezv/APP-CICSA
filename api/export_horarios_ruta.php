<?php
/**
 * ARCHIVO: export_horarios_ruta.php
 * Genera un Excel con la hora de activación y cierre de ruta de cada satélite
 * agrupado por usuario para un rango de fechas.
 * Parámetros:
 *   fecha  (opcional, default hoy) — una sola fecha
 *   start  (opcional) — fecha inicio de rango
 *   end    (opcional) — fecha fin de rango
 */
include 'db_config.php';

// Asegurar zona horaria de Perú/Colombia
$conn->query("SET time_zone = '-05:00';");

// Determinar rango de fechas
if (isset($_GET['start']) && isset($_GET['end'])) {
    $startDate = $_GET['start'];
    $endDate = $_GET['end'];
} else {
    $fecha = isset($_GET['fecha']) ? $_GET['fecha'] : date('Y-m-d');
    $startDate = $fecha;
    $endDate = $fecha;
}

$filename = "HORARIOS_RUTA_" . str_replace('-', '', $startDate) . "_" . str_replace('-', '', $endDate) . ".xls";

// Cabeceras HTTP para Excel
header("Content-Type: application/vnd.ms-excel; charset=utf-8");
header("Content-Disposition: attachment; filename=\"$filename\"");
header("Pragma: no-cache");
header("Expires: 0");

// 1. Obtener usuarios satélite
$sqlUsers = "SELECT id, nombre, dni, zona, distrito_nombre, telefono, celular_personal 
             FROM usuarios WHERE rol = 'satelite' ORDER BY nombre ASC";
$resUsers = $conn->query($sqlUsers);
$users = [];
while ($u = $resUsers->fetch_assoc()) {
    $users[] = $u;
}

// 2. Obtener todas las asistencias del rango
$sqlAtt = "SELECT a.usuario_id, a.evento, a.fecha, a.hora, u.nombre
           FROM asistencias a
           JOIN usuarios u ON a.usuario_id = u.id
           WHERE a.fecha BETWEEN ? AND ?
           ORDER BY a.fecha ASC, a.hora ASC";
$stmt = $conn->prepare($sqlAtt);
$stmt->bind_param("ss", $startDate, $endDate);
$stmt->execute();
$resAtt = $stmt->get_result();

// Mapear: [ userId ][ fecha ] = { ingresos: [...], salidas: [...] }
$dataMap = [];
while ($row = $resAtt->fetch_assoc()) {
    $uid = $row['usuario_id'];
    $fecha = $row['fecha'];
    if (!isset($dataMap[$uid][$fecha])) {
        $dataMap[$uid][$fecha] = ['ingresos' => [], 'salidas' => []];
    }
    if ($row['evento'] === 'INGRESO') {
        $dataMap[$uid][$fecha]['ingresos'][] = $row['hora'];
    } else {
        $dataMap[$uid][$fecha]['salidas'][] = $row['hora'];
    }
}
$stmt->close();

// 3. Generar todas las fechas del rango
$dates = [];
$current = strtotime($startDate);
$end = strtotime($endDate);
while ($current <= $end) {
    $dates[] = date('Y-m-d', $current);
    $current = strtotime('+1 day', $current);
}

$diasSemana = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
?>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <!--[if gte mso 9]>
    <xml>
    <x:ExcelWorkbook>
    <x:ExcelWorksheets>
    <x:ExcelWorksheet>
    <x:Name>Horarios Ruta</x:Name>
    <x:WorksheetOptions>
    <x:DisplayGridlines/>
    </x:WorksheetOptions>
    </x:ExcelWorksheet>
    </x:ExcelWorksheets>
    </x:ExcelWorkbook>
    </xml>
    <![endif]-->
    <style>
        body { font-family: Arial, sans-serif; font-size: 9pt; }
        .title-cell { background-color: #1e40af; color: #FFFFFF; font-weight: bold; text-align: center; border: 1px solid #000; font-size: 12pt; }
        .subtitle-cell { background-color: #dbeafe; font-weight: bold; text-align: center; border: 1px solid #93c5fd; font-size: 9pt; color: #1e3a5f; }
        .header-cell { background-color: #f1f5f9; font-weight: bold; text-align: center; border: 1px solid #94a3b8; font-size: 8pt; color: #1e293b; }
        .data-cell { border: 1px solid #cbd5e1; padding: 3px 5px; vertical-align: middle; }
        .date-group { background-color: #eff6ff; font-weight: bold; border: 1px solid #93c5fd; color: #1e40af; font-size: 9pt; }
        .ingreso-cell { background-color: #dcfce7; color: #15803d; font-weight: bold; text-align: center; border: 1px solid #86efac; }
        .salida-cell { background-color: #fee2e2; color: #dc2626; font-weight: bold; text-align: center; border: 1px solid #fca5a5; }
        .no-data { color: #94a3b8; text-align: center; font-style: italic; border: 1px solid #cbd5e1; }
        .en-ruta { background-color: #fef9c3; color: #a16207; font-weight: bold; text-align: center; border: 1px solid #fde047; }
        .nombre-cell { font-weight: bold; text-transform: uppercase; border: 1px solid #cbd5e1; }
        .num-format { mso-number-format:"\@"; }
    </style>
</head>
<body>

<table cellspacing="0" cellpadding="3">
    <!-- Título -->
    <tr style="height: 30pt;">
        <td colspan="7" class="title-cell">CONTROL DE HORARIOS DE RUTA — APPMOVIL S.A.C</td>
    </tr>
    <tr style="height: 20pt;">
        <td colspan="7" class="subtitle-cell">
            Periodo: <?= date('d/m/Y', strtotime($startDate)) ?> al <?= date('d/m/Y', strtotime($endDate)) ?> — Generado: <?= date('d/m/Y H:i:s') ?>
        </td>
    </tr>
    <tr><td colspan="7"></td></tr>

<?php foreach ($dates as $dateStr):
    $dayOfWeek = $diasSemana[date('w', strtotime($dateStr))];
    $dateFormatted = date('d/m/Y', strtotime($dateStr));
    $isSunday = (date('N', strtotime($dateStr)) == 7);
?>
    <!-- Cabecera por fecha -->
    <tr style="height: 22pt;">
        <td colspan="7" class="date-group">📅 <?= $dayOfWeek ?> <?= $dateFormatted ?><?= $isSunday ? ' (DOMINGO)' : '' ?></td>
    </tr>
    <tr style="height: 18pt;">
        <th class="header-cell" style="width: 30pt;">N°</th>
        <th class="header-cell" style="width: 200pt;">SATÉLITE</th>
        <th class="header-cell" style="width: 80pt;">DNI</th>
        <th class="header-cell" style="width: 80pt;">ZONA</th>
        <th class="header-cell" style="width: 100pt;">HORA ACTIVÓ RUTA</th>
        <th class="header-cell" style="width: 100pt;">HORA CERRÓ RUTA</th>
        <th class="header-cell" style="width: 80pt;">ESTADO</th>
    </tr>
    <?php
    $num = 1;
    $hasData = false;
    foreach ($users as $user) {
        $uid = $user['id'];
        if (!isset($dataMap[$uid][$dateStr])) continue;
        
        $hasData = true;
        $events = $dataMap[$uid][$dateStr];
        
        // Primera hora de INGRESO (activó ruta)
        $primerIngreso = !empty($events['ingresos']) ? min($events['ingresos']) : null;
        // Última hora de SALIDA (cerró ruta)
        $ultimaSalida = !empty($events['salidas']) ? max($events['salidas']) : null;
        
        $estado = 'Sin Actividad';
        $estadoClass = 'no-data';
        if ($primerIngreso && $ultimaSalida) {
            $estado = 'Ruta Cerrada';
            $estadoClass = 'salida-cell';
        } elseif ($primerIngreso) {
            $estado = 'En Ruta';
            $estadoClass = 'en-ruta';
        }
    ?>
    <tr style="height: 16pt;">
        <td class="data-cell" style="text-align: center;"><?= $num++ ?></td>
        <td class="nombre-cell">&nbsp;<?= htmlspecialchars($user['nombre']) ?></td>
        <td class="data-cell num-format" style="text-align: center;"><?= htmlspecialchars($user['dni'] ?: '-') ?></td>
        <td class="data-cell" style="text-align: center;"><?= htmlspecialchars($user['zona'] ?: $user['distrito_nombre'] ?: '-') ?></td>
        <td class="<?= $primerIngreso ? 'ingreso-cell' : 'no-data' ?>"><?= $primerIngreso ?: '— — —' ?></td>
        <td class="<?= $ultimaSalida ? 'salida-cell' : 'no-data' ?>"><?= $ultimaSalida ?: '— — —' ?></td>
        <td class="<?= $estadoClass ?>"><?= $estado ?></td>
    </tr>
    <?php } // end foreach users
    
    if (!$hasData) { ?>
    <tr style="height: 16pt;">
        <td colspan="7" class="no-data">No hay registros para esta fecha</td>
    </tr>
    <?php } ?>
    <tr><td colspan="7"></td></tr>

<?php endforeach; // end foreach dates ?>

</table>

</body>
</html>
<?php $conn->close(); ?>
