package com.example.appmovil.ui.solicitudes

object SupervisionConstants {
    val TIPO_ACTIVIDAD = listOf("Instalación", "Mantenimiento", "Postventa", "Migración")
    val TECNOLOGIA = listOf("FTTH", "HFC")
    val PERSONA_ATIENDE = listOf("Tercero", "Titular")
    val TIPO_TRABAJO = listOf("1play", "2play", "3play")
    val RESPUESTAS = listOf("no", "no aplica", "si")
    val ESTADO_SERVICIO = listOf("Completado", "Pendiente")
    val DERIVACIONES = listOf("derivado a sistemas", "derivado a pext", "derivado caida 360")
    
    // --- NUEVOS CONSTANTES ---
    val CUADRILLA = listOf("Simple", "Doble")
    val HORA_LLEGADA = listOf("Inicio de la instalación", "Media instalación", "Finalizando instalación")
    val CUMPLE_NO_CUMPLE = listOf("Cumple", "No cumple")
    val SI_NO = listOf("Si", "No")
    val MATERIAL_ESTADO = listOf("Nuevo", "Reutilizado")
    val PRODUCTIVIDAD = listOf("Productivo", "No es productivo")

    data class Question(val id: String, val text: String)

    val SECCIONES = mapOf(
        "CALIDAD DE SERVICIO" to listOf(
            Question("calidad_1", "El técnico es autosuficiente, tiene un trato adecuado, se encuentra correctamente uniformado y mejora la experiencia del cliente"),
            Question("calidad_2", "El técnico brinda información de los servicios de valor agregado y soluciones únicas de nuestro operador")
        ),
        "SEGURIDAD Y EPP" to listOf(
            Question("seguridad_1", "El técnico cuenta con sus EPP y EPE completos y los usa de forma adecuada"),
            Question("seguridad_2", "El técnico cuida su integridad física y respeta las distancias mínimas de seguridad eléctrica -DMS?"),
            Question("seguridad_vehiculo", "El técnico mantiene el vehículo limpio y ordenado")
        ),
        "EJECUCIÓN Y MATERIALES" to listOf(
            Question("ejecucion_1", "El técnico instala respetando las normativas técnicas la acometida (loop, chapa q, chapa p)?"),
            Question("ejecucion_2", "El técnico deja limpia su zona de trabajo?"),
            Question("ejecucion_4", "El técnico deja el/los equipos en un lugar adecuado y correctamente cableado para el uso del servicio?"),
            Question("acta_coincide", "El supervisor evidencia si el acta coincide con los materiales utilizados")
        ),
        "CONFIGURACION Y PRUEBAS" to listOf(
            Question("config_1", "Se configura de forma correcta los equipos instalados y se explica al cliente el uso del servicio?"),
            Question("config_2", "Se verifica parámetros adecuados en el ONT/EMTA dentro de rangos establecidos?"),
            Question("rf_check", "Cumple con el RF")
        )
    )

    val PENALIDADES = mapOf(
        "INCUMPLIMIENTOS CRÍTICOS" to listOf(
            "No utiliza EPP obligatorios",
            "Trato inadecuado al cliente",
            "Uso de material no autorizado/reutilizado sin reportar",
            "No registro de inicio/fin en sistemas",
            "Incumplimiento de normativa técnica",
            "No envío de acta de instalación"
        )
    )

    val QUESTIONS_MAP: Map<String, String> by lazy {
        SECCIONES.values.flatten().associate { it.id to it.text }
    }
}

