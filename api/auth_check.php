<?php
// ARCHIVO: api/auth_check.php
include 'db_config.php';
// Función para validar la sesión única

function validateSession($conn, $userId, $sessionToken) {
    if (!$userId) return true; // Si no hay ID, no validamos (ej. registro)
    
    // Si el token viene vacío de la App, pero en DB hay uno, es sospechoso
    // Pero permitimos para no romper compatibilidad inmediata si es necesario
    if (empty($sessionToken)) return true; 

    $stmt = $conn->prepare("SELECT session_token FROM usuarios WHERE id = ?");
    $stmt->bind_param("i", $userId);
    $stmt->execute();
    $res = $stmt->get_result();
    
    if ($row = $res->fetch_assoc()) {
        $dbToken = $row['session_token'];
        // Si hay un token en DB y no coincide con el enviado
        if (!empty($dbToken) && $dbToken !== $sessionToken) {
            echo json_encode([
                "status" => "error",
                "session_error" => true,
                "message" => "Sesión iniciada en otro dispositivo. Se cerrará esta sesión."
            ]);
            exit;
        }
    }
    $stmt->close();
    return true;
}
?>
