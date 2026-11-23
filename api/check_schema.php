<?php
include 'db_config.php';
$result = $conn->query("SHOW COLUMNS FROM usuarios");
$columns = [];
while($row = $result->fetch_assoc()) {
    $columns[] = $row['Field'];
}
echo json_encode($columns);
?>
