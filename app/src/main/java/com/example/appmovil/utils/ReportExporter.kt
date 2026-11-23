package com.example.appmovil.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.appmovil.data.remote.SolicitudItem
import java.io.File
import java.io.FileOutputStream

object ReportExporter {

    /**
     * Exporta un resumen de satélites a CSV
     */
    fun exportSatelliteStats(context: Context, stats: List<SatelliteStat>) {
        val fileName = "Resumen_Satélites_${System.currentTimeMillis()}.csv"
        val csvHeader = "Satélite,Pendientes,En Proceso,Completadas,Total\n"
        val csvBody = StringBuilder()

        stats.forEach {
            csvBody.append("${it.name},${it.pending},${it.inProgress},${it.completed},${it.total}\n")
        }

        val content = csvHeader + csvBody.toString()
        shareFile(context, fileName, content)
    }

    /**
     * Exporta todas las solicitudes a un reporte detallado
     */
    fun exportDetailedReport(context: Context, solicitudes: List<SolicitudItem>) {
        val fileName = "Reporte_Detallado_${System.currentTimeMillis()}.csv"
        val csvHeader = "ID,SOT,Cliente,Estado,Satélite,Fecha Creación,Motivo\n"
        val csvBody = StringBuilder()

        solicitudes.forEach {
            val satName = it.nombreSatelite ?: "No Asignado"
            val motivo = it.motivo?.replace(",", ";") ?: ""
            val cli = it.tecnico?.replace(",", ";") ?: ""
            csvBody.append("${it.id},${it.sot},${cli},${it.estado},${satName},${it.fechaCreacion},${motivo}\n")
        }

        val content = csvHeader + csvBody.toString()
        shareFile(context, fileName, content)
    }

    /**
     * Exporta el registro de asistencia a CSV
     */
    fun exportAttendance(context: Context, attendance: List<com.example.appmovil.data.remote.AttendanceItem>) {
        val fileName = "Asistencia_${System.currentTimeMillis()}.csv"
        val csvHeader = "ID,Satélite,Evento,Fecha,Hora\n"
        val csvBody = StringBuilder()

        attendance.forEach {
            csvBody.append("${it.userId},${it.nombre},${it.evento},${it.fecha},${it.hora}\n")
        }

        val content = csvHeader + csvBody.toString()
        shareFile(context, fileName, content)
    }

    /**
     * Descarga el reporte profesional (Excel) directamente del servidor
     */
    fun downloadServerReport(context: Context, startDate: String, endDate: String) {
        val url = "http://www.cicsamovil.com/api/export_solicitudes.php?start_date=$startDate&end_date=$endDate"
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Descarga el reporte de asistencia mensual (Excel) directamente del servidor
     */
    fun downloadAttendanceMonthlyReport(context: Context, month: Int, year: Int) {
        val url = "http://www.cicsamovil.com/api/export_asistencias_mensual.php?month=$month&year=$year"
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, fileName: String, content: String) {
        try {
            val cacheDir = File(context.cacheDir, "reports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { 
                // Añadimos BOM para que Excel reconozca UTF-8 correctamente
                it.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                it.write(content.toByteArray()) 
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Reporte CICSA Móvil")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Descargar reporte"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class SatelliteStat(
    val name: String,
    val pending: Int,
    val inProgress: Int,
    val completed: Int,
    val total: Int
)
