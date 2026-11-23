<?php
/**
 * ARCHIVO: export_supervisiones.php
 * Genera un reporte de SUPERVISIONES EN CAMPO en formato compatible con Excel (.xls)
 */

include 'db_config.php';

// Asegurar zona horaria
$conn->query("SET time_zone = '-05:00';");

// Nombre del archivo profesional
$filename = "REPORTE_SUPERVISIONES_" . date('Ymd_His') . ".xls";

// Configurar cabeceras para Excel
header("Content-Type: application/vnd.ms-excel; charset=utf-8");
header("Content-Disposition: attachment; filename=\"$filename\"");
header("Pragma: no-cache");
header("Expires: 0");

$start_date = isset($_GET['start_date']) ? $_GET['start_date'] : '';
$end_date = isset($_GET['end_date']) ? $_GET['end_date'] : '';

$where_sql = "";
if (!empty($start_date) && !empty($end_date)) {
    $sd = $conn->real_escape_string($start_date);
    $ed = $conn->real_escape_string($end_date);
    $where_sql = " WHERE s.fecha_creacion BETWEEN '$sd 00:00:00' AND '$ed 23:59:59' ";
}

$sql = "SELECT s.id, s.id_supervisor, s.id_tecnico, s.sot, s.checklist, s.evidencia, s.comentarios, s.fecha_creacion, 
               u1.nombre as nombre_supervisor,
               u2.nombre as nombre_tecnico
        FROM supervisiones s
        INNER JOIN usuarios u1 ON s.id_supervisor = u1.id
        INNER JOIN usuarios u2 ON s.id_tecnico = u2.id
        $where_sql
        ORDER BY s.fecha_creacion DESC";
$result = $conn->query($sql);

?>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel"
    xmlns="http://www.w3.org/TR/REC-html40">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
</head>

<body style="font-family: 'Segoe UI', Calibri, Arial;">
    <table border="0" cellpadding="0" cellspacing="0" style="width: 100%;">
        <tr>
            <td colspan="6"
                style="background-color: #2E86C1; color: #FFFFFF; font-size: 20pt; padding: 15px; border: 1px solid #000000; font-weight: bold;">
                REPORTE DE SUPERVISIONES EN CAMPO
            </td>
            <td colspan="2"
                style="background-color: #1B4F72; color: #FFFFFF; font-size: 9pt; text-align: right; vertical-align: middle; padding: 5px 15px;">
                <b>CICSA ÚLTIMA MILLA</b><br />
                Generado: <?php echo date('d/m/Y H:i'); ?>
            </td>
        </tr>
    </table>
    <br />
    <table border="1" cellpadding="0" cellspacing="0" style="width: 100%; border-collapse: collapse;">
        <thead>
            <tr>
                <th
                    style="background-color: #2E86C1; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px;">
                    #</th>
                <th
                    style="background-color: #2E86C1; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px;">
                    Fecha</th>
                <th
                    style="background-color: #2E86C1; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px;">
                    SOT</th>
                <th
                    style="background-color: #2E86C1; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px;">
                    Supervisor</th>
                <th
                    style="background-color: #2E86C1; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px;">
                    Técnico</th>
                <th
                    style="background-color: #2E86C1; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px;">
                    Comentarios</th>
                <th
                    style="background-color: #2E86C1; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px;">
                    Respuestas de Auditoría</th>
                <th
                    style="background-color: #2E86C1; color: #FFFFFF; font-size: 10pt; font-weight: bold; border: 1px solid #000000; padding: 10px;">
                    Penalidades</th>
            </tr>
        </thead>
        <tbody>
            <?php
            $i = 0;
            if ($result && $result->num_rows > 0) {
                while ($row = $result->fetch_assoc()) {
                    $i++;
                    $bgRow = ($i % 2 == 0) ? "#F8F9FA" : "#FFFFFF";

                    $dt = new DateTime($row['fecha_creacion']);
                    $fCreacion = $dt->format('d/m/Y H:i');

                    // Procesar JSON de checklist
                    $checklistRaw = json_decode($row['checklist'], true);
                    $respuestasStr = "";
                    $penalidadesStr = "";

                    if ($checklistRaw) {
                        $respuestas = isset($checklistRaw['respuestas']) ? $checklistRaw['respuestas'] : [];
                        foreach ($respuestas as $key => $val) {
                            $respuestasStr .= strtoupper(str_replace('_', ' ', $key)) . ": " . strtoupper($val) . "\n";
                        }

                        $penalidades = isset($checklistRaw['penalidades']) ? $checklistRaw['penalidades'] : [];
                        if (!empty($penalidades)) {
                            $penalidadesStr = implode("\n- ", $penalidades);
                            $penalidadesStr = "- " . $penalidadesStr;
                        } else {
                            $penalidadesStr = "Ninguna";
                        }
                    }

                    echo "<tr style='background-color: $bgRow;'>";
                    echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'>$i</td>";
                    echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'>$fCreacion</td>";
                    echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; text-align: center;'><b>" . htmlspecialchars($row['sot']) . "</b></td>";
                    echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt;'>" . htmlspecialchars($row['nombre_supervisor'] ?: 'Desconocido') . "</td>";
                    echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt;'>" . htmlspecialchars($row['nombre_tecnico'] ?: 'Desconocido') . "</td>";
                    echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt;'>" . htmlspecialchars($row['comentarios']) . "</td>";
                    echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; white-space: pre-wrap;'>" . htmlspecialchars($respuestasStr) . "</td>";
                    echo "<td style='border: 1px solid #D5DBDB; padding: 10px; font-size: 9pt; white-space: pre-wrap; color: #A50000;'>" . htmlspecialchars($penalidadesStr) . "</td>";
                    echo "</tr>";
                }
            } else {
                echo "<tr><td colspan='8' style='border: 1px solid #D5DBDB; padding: 30px; text-align: center;'>No hay supervisiones registradas.</td></tr>";
            }
            ?>
        </tbody>
    </table>
</body>

</html>
<?php $conn->close(); ?>