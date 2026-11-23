<?php
include 'api/db_config.php';
$res = $conn->query("DESCRIBE usuarios");
while($row = $res->fetch_assoc()) {
    echo $row['Field'] . " (" . $row['Type'] . ")\n";
}
?>
