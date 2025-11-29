<?php
declare(strict_types=1);
$DB_PATH = __DIR__ . '/data/sms.db';
$API_KEY = '';
function db() {
    global $DB_PATH;
    $pdo = new PDO('sqlite:' . $DB_PATH);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    return $pdo;
}
function check_key() {
    global $API_KEY;
    if ($API_KEY === '') return true;
    $h = $_SERVER['HTTP_X_API_KEY'] ?? '';
    return hash_equals($API_KEY, $h);
}
function json_out($code, $data) {
    http_response_code($code);
    header('Content-Type: application/json');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
}
