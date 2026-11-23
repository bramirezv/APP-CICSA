<?php
/**
 * ARCHIVO: export_asistencias_mensual.php
 * Genera el reporte de ASISTENCIA MENSUAL (Matriz) con ALTA FIDELIDAD al Excel Satelites.xlsx
 */

include 'db_config.php';

// Asegurar zona horaria de Perú/Colombia
$conn->query("SET time_zone = '-05:00';");

// Capturar Mes y Año (por defecto el actual)
$month = isset($_GET['month']) ? intval($_GET['month']) : intval(date('m'));
$year = isset($_GET['year']) ? intval($_GET['year']) : intval(date('Y'));

$daysInMonth = cal_days_in_month(CAL_GREGORIAN, $month, $year);
$meses = ["", "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"];
$monthLabel = $meses[$month];

$filename = "REPORTE_ASISTENCIA_" . $monthLabel . "_" . $year . ".xls";

// Configurar cabeceras para Excel
header("Content-Type: application/vnd.ms-excel; charset=utf-8");
header("Content-Disposition: attachment; filename=\"$filename\"");
header("Pragma: no-cache");
header("Expires: 0");

// 1. Obtener SOLO los usuarios con rol 'satelite'
$sqlUsers = "SELECT * FROM usuarios WHERE rol = 'satelite' ORDER BY nombre ASC";
$resUsers = $conn->query($sqlUsers);
$users = [];
while ($u = $resUsers->fetch_assoc()) {
    $users[] = $u;
}

// Para que el reporte tenga el mismo tamaño que el original, agregaremos filas vacías hasta 15 si hay menos
$totalWorkers = count($users);
$displayRows = max($totalWorkers, 15);

// 2. Obtener TODAS las asistencias del mes
$sqlAttendance = "SELECT usuario_id, DAY(fecha) as dia FROM asistencias 
                  WHERE MONTH(fecha) = $month AND YEAR(fecha) = $year";
$resAttendance = $conn->query($sqlAttendance);
$attendanceMap = [];
while ($a = $resAttendance->fetch_assoc()) {
    $attendanceMap[$a['usuario_id']][$a['dia']] = 'OK';
}

// Mapeo de letras de columna para la fórmula de Excel
function getExcelCol($n) {
    if ($n < 26) return chr(65 + $n);
    return chr(65 + floor($n / 26) - 1) . chr(65 + ($n % 26));
}

// Las referencias de columna se calculan en la sección de datos

?>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <!--[if gte mso 9]>
    <xml>
    <x:ExcelWorkbook>
    <x:ExcelWorksheets>
    <x:ExcelWorksheet>
    <x:Name>Satelites</x:Name>
    <x:WorksheetOptions>
    <x:DisplayGridlines/>
    </x:WorksheetOptions>
    </x:ExcelWorksheet>
    </x:ExcelWorksheets>
    </x:ExcelWorkbook>
    </xml>
    <![endif]-->
    <style>
        body { font-family: Arial, sans-serif; font-size: 8pt; }
        .legend-title { font-weight: bold; background-color: #FFFFFF; border: 1px solid #000000; text-align: center; }
        .asistio-ok { background-color: #92D050; border: 1px solid #000000; text-align: center; font-weight: bold; }
        .falto-f { background-color: #FF0000; color: #FFFFFF; border: 1px solid #000000; text-align: center; font-weight: bold; }
        .inasistencia-fi { background-color: #FFC000; border: 1px solid #000000; text-align: center; }
        .inasistencia-fj { background-color: #C00000; color: #FFFFFF; border: 1px solid #000000; text-align: center; }
        .header-cell { background-color: #FFFFFF; font-weight: bold; text-align: center; border: 1px solid #000000; }
        .date-header { mso-rotate: 90; background-color: #FFFFFF; border: 1px solid #000000; text-align: center; font-size: 7pt; height: 68pt; }
        .sunday-header { mso-rotate: 90; background-color: #FF0000; color: #FFFFFF; border: 1px solid #000000; text-align: center; font-size: 7pt; height: 68pt; }
        .data-cell { border: 1px solid #000000; height: 15.5pt; vertical-align: middle; }
        .sunday-merge { background-color: #FF0000; border: 1px solid #000000; }
        .num-format { mso-number-format:"\@"; }
    </style>
</head>
<body>

<table cellspacing="0" cellpadding="0">
    <!-- COLWIDTHS EXACTOS (en pt approx de Satelites.xlsx) -->
    <col style="width: 20pt;" /> <!-- A -->
    <col style="width: 25pt;" /> <!-- B: # -->
    <col style="width: 280pt;" /> <!-- C: Nombre -->
    <col style="width: 85pt;" /> <!-- D: DNI -->
    <col style="width: 60pt;" /> <!-- E: Dias -->
    <col style="width: 100pt;" /> <!-- F: Obs -->
    <col style="width: 135pt;" /> <!-- G: Puesto -->
    <?php for ($d = 1; $d <= $daysInMonth; $d++) echo '<col style="width: 24pt;" />'; ?>
    <col style="width: 50pt;" /> <!-- AG: Distrito -->
    <col style="width: 55pt;" /> <!-- AH: Licencia -->
    <col style="width: 75pt;" /> <!-- AI: Cell -->

    <!-- LEYENDA (Filas 1-4) -->
    <tr style="height: 15pt;"> <td colspan="35"></td> </tr>
    <tr style="height: 30pt;">
        <td colspan="2"></td>
        <td class="legend-title" style="font-size: 11pt;">Leyenda:</td>
        <td class="asistio-ok" style="font-size: 11pt;">Asistio</td>
        <td class="falto-f" style="font-size: 11pt;">Falto</td>
        <td class="inasistencia-fi" style="font-size: 11pt;">Inasistencia Injustificada</td>
        <td class="inasistencia-fj" style="font-size: 11pt;">Inasistencia justificada</td>
    </tr>
    <tr style="height: 15.5pt;">
        <td colspan="2"></td>
        <td class="legend-title">Leyenda:</td>
        <td class="asistio-ok">OK</td>
        <td class="falto-f">F</td>
        <td class="inasistencia-fi">FI</td>
        <td class="inasistencia-fj">FJ</td>
    </tr>
    <tr style="height: 15.5pt;">
        <td colspan="2"></td>
        <td class="legend-title">Almacen:</td>
        <td style="border: 1px solid #000000; text-align: center;">B</td>
    </tr>
    <tr style="height: 13.5pt;"> <td colspan="35"></td> </tr>

    <!-- CABECERA (Fila 6) -->
    <tr style="height: 68pt;">
        <th class="header-cell" style="width: 20pt;"></th>
        <th class="header-cell" style="width: 25pt;">n°</th>
        <th class="header-cell" style="width: 280pt;">APELLIDOS Y NOMBRES</th>
        <th class="header-cell" style="width: 85pt;">DNI/CE</th>
        <th class="header-cell" style="width: 60pt;">Dias<br/>Asistidos<br/>Teoricos</th>
        <th class="header-cell" style="width: 100pt;">OBSERVACIONES</th>
        <th class="header-cell" style="width: 135pt;">PUESTO </th>
        <?php 
        for ($d = 1; $d <= $daysInMonth; $d++) {
            $dateObj = mktime(0, 0, 0, $month, $d, $year);
            $dayNum = date('N', $dateObj);
            $class = ($dayNum == 7) ? "sunday-header" : "date-header";
            $dateFormatted = str_pad($d, 2, '0', STR_PAD_LEFT) . '/' . str_pad($month, 2, '0', STR_PAD_LEFT) . '/' . $year;
            echo "<th class='$class'>$dateFormatted</th>";
        }
        ?>
        <th class="header-cell" style="width: 50pt;">Distrito </th>
        <th class="header-cell" style="width: 55pt;">Licencia </th>
        <th class="header-cell" style="width: 75pt;">CELULAR<br/>PERSONAL </th>
    </tr>

    <!-- CUERPO (Fila 7+) con solo SATELITES -->
    <?php
    // Con la columna A vacía, las fechas empiezan en columna I (índice 8)
    $dateStartCol = getExcelCol(8); // I
    $dateEndCol = getExcelCol(8 + $daysInMonth - 1);
    
    $rowStart = 7;
    for ($i = 0; $i < $displayRows; $i++) {
        $user = isset($users[$i]) ? $users[$i] : null;
        $rowNum = $rowStart + $i;
        echo "<tr style='height: 15.6pt;'>";
        
        // Columna A (vacía - margen)
        echo "<td class='data-cell'></td>";
        
        // n° correlativo (fórmula como en el Excel original)
        echo "<td class='data-cell' style='text-align: center; font-weight: bold;'>=ROW(C$rowNum)-6</td>";
        
        // Apellidos y Nombres
        echo "<td class='data-cell' style='text-align: left; text-transform: uppercase;'>&nbsp;" . ($user ? htmlspecialchars($user['nombre']) : '') . "</td>";
        
        // DNI/CE con formato de texto para evitar notación científica
        echo "<td class='data-cell num-format' style='text-align: center;'>" . ($user ? htmlspecialchars($user['dni'] ?: '') : '') . "</td>";
        
        // Días Asistidos Teóricos (Fórmula Excel exacta como el original)
        $range = $dateStartCol . $rowNum . ":" . $dateEndCol . $rowNum;
        echo "<td class='data-cell' style='text-align: center; font-weight: bold; mso-number-format:\"0\";'>=COUNTIF($range, \"OK\")</td>";
        
        // Observaciones (Vacío para llenar manual)
        echo "<td class='data-cell'></td>";
        
        // Puesto
        echo "<td class='data-cell' style='text-align: center;'>" . ($user ? "SATELITE" : "") . "</td>";

        // Matriz de Asistencia Diaria
        for ($d = 1; $d <= $daysInMonth; $d++) {
            $dateObj = mktime(0, 0, 0, $month, $d, $year);
            $isSunday = (date('N', $dateObj) == 7);
            
            if ($isSunday) {
                // Solo insertamos el rowspan en la primera fila (Efecto Columna Unificada)
                if ($i == 0) {
                    echo "<td rowspan='$displayRows' class='sunday-merge'></td>";
                }
            } else {
                $status = ($user && isset($attendanceMap[$user['id']][$d])) ? "OK" : "";
                $class = ($status == "OK") ? "asistio-ok data-cell" : "data-cell";
                echo "<td class='$class' style='text-align: center;'>" . $status . "</td>";
            }
        }

        // Datos Adicionales Finales (Distrito, Licencia, Celular)
        echo "<td class='data-cell' style='text-align: center;'>" . ($user ? htmlspecialchars($user['distrito_nombre'] ?: $user['zona']) : '') . "</td>";
        echo "<td class='data-cell' style='text-align: center;'>" . ($user ? htmlspecialchars($user['licencia'] ?: '') : '') . "</td>";
        echo "<td class='data-cell num-format' style='text-align: center;'>" . ($user ? htmlspecialchars($user['celular_personal'] ?: $user['telefono']) : '') . "</td>";
        echo "</tr>";
    }
    ?>
</table>

</body>
</html>
<?php $conn->close(); ?>
