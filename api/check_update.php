<?php
header('Content-Type: application/json');

/**
 * SISTEMA DE ACTUALIZACION APPMOVIL
 * 
 * Para lanzar una nueva version:
 * 1. Sube el nuevo APK a la carpeta 'app/' o similar en tu servidor.
 * 2. Cambia el 'versionCode' al numero configurado en build.gradle (versionCode).
 * 3. Actualiza 'versionName' y 'apkUrl'.
 */

$updateInfo = [
    "versionCode" => 4, // Cambiado a 4 para ser mayor que el 3 de la App
    "versionName" => "1.0",
    "apkUrl" => "https://www.cicsamovil.com/api/app-debug.apk", // Asegurate que esta URL sea valida
    "notes" => "Versión inicial con sistema FIFO y feedback de satélite."
];

echo json_encode($updateInfo);
