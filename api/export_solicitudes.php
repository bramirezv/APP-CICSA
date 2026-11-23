<?php
/**
 * ARCHIVO: export_solicitudes.php
 * Genera un reporte CORPORATIVO PREMIUM en formato compatible con Excel (.xls)
 */

include 'db_config.php';

// Asegurar zona horaria de Perú/Colombia
$conn->query("SET time_zone = '-05:00';");

// Capturar filtros
$startDate = isset($_GET['start_date']) ? $_GET['start_date'] : null;
$endDate = isset($_GET['end_date']) ? $_GET['end_date'] : null;
$tipo = isset($_GET['tipo']) ? $_GET['tipo'] : 'normal';

// Construir cláusula WHERE
$whereClauses = array();
if ($tipo === 'auxiliar') {
    $whereClauses[] = "tipo = 'auxiliar'";
} else {
    $whereClauses[] = "tipo != 'auxiliar'";
}

if (!empty($startDate) && !empty($endDate)) {
    $startDateS = $conn->real_escape_string($startDate);
    $endDateS = $conn->real_escape_string($endDate);
    $whereClauses[] = "fecha_creacion BETWEEN '$startDateS 00:00:00' AND '$endDateS 23:59:59'";
}

$whereClause = count($whereClauses) > 0 ? " WHERE " . implode(" AND ", $whereClauses) : "";

// Nombre del archivo profesional
$suffix = ($startDate && $endDate) ? "_{$startDate}_al_{$endDate}" : "_" . date('Ymd_His');
$filename = "REPORTE_GESTION_SOLICITUDES" . $suffix . ".xls";

// Configurar cabeceras para Excel
header("Content-Type: application/vnd.ms-excel; charset=utf-8");
header("Content-Disposition: attachment; filename=\"$filename\"");
header("Pragma: no-cache");
header("Expires: 0");

// --- LÓGICA DE CONTEO PARA EL RESUMEN ---
function getCount($conn, $condition, $whereClause) {
    if (empty($whereClause)) {
        $finalWhere = empty($condition) ? "" : " WHERE $condition";
    } else {
        $finalWhere = $whereClause . (empty($condition) ? "" : " AND $condition");
    }
    $res = $conn->query("SELECT COUNT(*) as total FROM solicitudes $finalWhere");
    return $res ? $res->fetch_assoc()['total'] : 0;
}

$totalCount = getCount($conn, "", $whereClause);
$esperaCount = getCount($conn, "estado = 'pendiente'", $whereClause);
$cursoCount = getCount($conn, "estado IN ('aceptada', 'en_camino', 'llegado', 'corregir')", $whereClause);
$completadasCount = getCount($conn, "estado = 'completada'", $whereClause);
$rechazadasCount = getCount($conn, "(estado LIKE '%rechazada%' OR estado = 'rechazado')", $whereClause);
$canceladasCount = getCount($conn, "(estado LIKE '%cancelada%' OR estado = 'cancelado')", $whereClause);

$sql = "SELECT * FROM solicitudes " . $whereClause . " ORDER BY fecha_creacion DESC";
$result = $conn->query($sql);
?>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
</head>
<body style="font-family: 'Segoe UI', Calibri, Arial;">
<table border="0" cellpadding="0" cellspacing="0" style="width: 100%;">
    <tr>
        <td colspan="10" style="background-color: #A50000; color: #FFFFFF; font-size: 20pt; padding: 15px; border: 1px solid #000000; font-weight: bold;">
            ESTADO DE SOLICITUDES DE GESTIÓN <?php echo $tipo === 'auxiliar' ? '(AUXILIARES)' : '(SATÉLITES)'; ?>
        </td>
        <td colspan="3" style="background-color: #2C3E50; color: #FFFFFF; font-size: 9pt; text-align: right; vertical-align: middle; padding: 5px 15px;">
            <b>CICSA ÚLTIMA MILLA</b><br/>
            Periodo: <?php echo ($startDate && $endDate) ? "$startDate al $endDate" : "Histórico Total"; ?><br/>
            Generado: <?php echo date('d/m/Y H:i'); ?>
        </td>
    </tr>
</table>
<br/>
<table border="0" cellpadding="0" cellspacing="0" style="width: 100%; border-collapse: separate; border-spacing: 12px;">
    <tr>
        <td style="width: 15%; background-color: #EBF5FB; border-left: 5px solid #2E86C1; padding: 12px; border: 1px solid #D6EAF8;">
            <div style="font-size: 8pt; color: #2E86C1; font-weight: bold;">TOTAL SOLICITUDES</div>
            <div style="font-size: 16pt; color: #1B4F72; font-weight: bold;"><?php echo $totalCount; ?></div>
        </td>
        <td style="width: 15%; background-color: #FEF9E7; border-left: 5px solid #F39C12; padding: 12px; border: 1px solid #FCF3CF;">
            <div style="font-size: 8pt; color: #F39C12; font-weight: bold;">EN ESPERA</div>
            <div style="font-size: 16pt; color: #7D6608; font-weight: bold;"><?php echo $esperaCount; ?></div>
        </td>
        <td style="width: 15%; background-color: #EAF2F8; border-left: 5px solid #3498DB; padding: 12px; border: 1px solid #D6EAF8;">
            <div style="font-size: 8pt; color: #3498DB; font-weight: bold;">EN CURSO</div>
            <div style="font-size: 16pt; color: #1A5276; font-weight: bold;"><?php echo $cursoCount; ?></div>
        </td>
        <td style="width: 15%; background-color: #E9F7EF; border-left: 5px solid #27AE60; padding: 12px; border: 1px solid #D4EFDF;">
            <div style="font-size: 8pt; color: #27AE60; font-weight: bold;">COMPLETADAS</div>
            <div style="font-size: 16pt; color: #186A3B; font-weight: bold;"><?php echo $completadasCount; ?></div>
        </td>
        <td style="width: 15%; background-color: #FDEDEC; border-left: 5px solid #E74C3C; padding: 12px; border: 1px solid #FADBD8;">
            <div style="font-size: 8pt; color: #E74C3C; font-weight: bold;">RECHAZADAS</div>
            <div style="font-size: 16pt; color: #7B241C; font-weight: bold;"><?php echo $rechazadasCount; ?></div>
        </td>
        <td style="width: 15%; background-color: #F4F6F6; border-left: 5px solid #7F8C8D; padding: 12px; border: 1px solid #E5E8E8;">
            <div style="font-size: 8pt; color: #7F8C8D; font-weight: bold;">CANCELADAS</div>
            <div style="font-size: 16pt; color: #424949; font-weight: bold;"><?php echo $canceladasCount; ?></div>
        </td>
    </tr>
</table>
<br/>
<table border="1" cellpadding="0" cellspacing="0" style="width: 100%; border-collapse: collapse;">
    <thead>
        <tr>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 40px;">#</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 100px;">Fecha Inicio</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 90px;">Hora Inicio</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 120px;">Fecha Finalización</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 110px;">Hora Finalización</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 110px;">Estado</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 180px;">Técnico</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 250px;">Motivo</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 150px;">Materiales</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 120px;">SOT / Req.</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 180px;">Supervisor</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 180px;">Satélite Asignado</th>
            <th style="background-color: #A50000; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px; width: 250px;">Feedback / Observación Satélite</th>
        </tr>
    </thead>
    <tbody>
        <?php
        $i = 0;
        if ($result && $result->num_rows > 0) {
            while ($row = $result->fetch_assoc()) {
                $i++;
                $bgRow = ($i % 2 == 0) ? "#F8F9FA" : "#FFFFFF";
                $dtInit = new DateTime($row['fecha_creacion']);
                $fInit = $dtInit->format('d/m/Y'); $hInit = $dtInit->format('H:i');
                $fEnd = "-"; $hEnd = "-";
                if (!empty($row['fecha_finalizacion'])) {
                    $dtEnd = new DateTime($row['fecha_finalizacion']);
                    $fEnd = $dtEnd->format('d/m/Y'); $hEnd = $dtEnd->format('H:i');
                }
                $est = strtolower($row['estado']);
                $bgStatus = "#FAD7A0"; $txtStatus = "#7E5109"; $labelStatus = ucfirst($est);
                if ($est == 'completada') { $bgStatus = "#D4EFDF"; $txtStatus = "#196F3D"; $labelStatus = "Completada"; }
                elseif (stripos($est, 'rechazada') !== false || $est == 'rechazado') { $bgStatus = "#FADBD8"; $txtStatus = "#943126"; $labelStatus = "Rechazada"; }
                elseif ($est == 'pendiente') { $bgStatus = "#FCF3CF"; $txtStatus = "#9A7D0A"; $labelStatus = "En Espera"; }
                elseif ($est == 'cancelada' || $est == 'cancelado') { $bgStatus = "#E5E8E8"; $txtStatus = "#424949"; $labelStatus = "Cancelada"; }
                elseif (in_array($est, ['aceptada', 'en_camino', 'llegado'])) { $bgStatus = "#D6EAF8"; $txtStatus = "#1B4F72"; $labelStatus = "En Curso"; }

                echo "<tr style='background-color: $bgRow;'>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'>$i</td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'><b>$fInit</b></td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'>$hInit</td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'>$fEnd</td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'>$hEnd</td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'><div style='background-color: $bgStatus; color: $txtStatus; padding: 4px 8px; font-weight: bold; border-radius: 4px; border: 1px solid $txtStatus;'>$labelStatus</div></td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-transform: uppercase;'><b>" . htmlspecialchars($row['tecnico']) . "</b></td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt;'>" . htmlspecialchars($row['motivo']) . "</td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt;'>" . htmlspecialchars($row['material'] ?: '-') . "</td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center; color: #A50000;'><b>" . htmlspecialchars($row['sot'] ?: '-') . "</b></td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt;'>" . htmlspecialchars($row['nombre_supervisor']) . "</td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt;'>" . htmlspecialchars($row['nombre_satelite'] ?: '-') . "</td>";
                echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt;'>" . htmlspecialchars($row['comentario_satelite'] ?: '-') . "</td>";
                echo "</tr>";
            }
        } else {
            echo "<tr><td colspan='13' style='border: 1px solid #D5DBDB; padding: 30px; text-align: center;'>No se encontraron registros en el periodo seleccionado.</td></tr>";
        }
        ?>
    </tbody>
</table>
<br/>
<table border="0" cellpadding="0" cellspacing="0" style="width: 100%;">
    <tr>
        <td style="font-size: 8pt; color: #7F8C8D; font-style: italic; padding: 5px; border-top: 1px solid #D5DBDB;">* Reporte oficial generado por CICSA ÚLTIMA MILLA.</td>
    </tr>
</table>
</body>
</html>
<?php $conn->close(); ?>