<?php
include 'db_config.php';
$result = $conn->query("DESCRIBE solicitudes");
while($row = $result->fetch_assoc()) {
    echo $row['Field'] . " - " . $row['Type'] . "\n";
}
$conn->close();
?>
