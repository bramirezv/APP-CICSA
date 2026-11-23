<?php
include 'db_config.php';
$res = $conn->query("DESCRIBE supervisiones");
if ($res) {
    while($row = $res->fetch_assoc()) {
        echo $row['Field'] . " (" . $row['Type'] . ")\n";
    }
} else {
    echo "Error: " . $conn->error;
}
?>
