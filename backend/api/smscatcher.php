<?php
declare(strict_types=1);
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Api-Key');
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(204); exit; }
require_once __DIR__ . '/../config.php';
if (!check_key()) { json_out(401, ['ok'=>false,'error'=>'unauthorized']); exit; }
$ct = $_SERVER['CONTENT_TYPE'] ?? '';
$raw = file_get_contents('php://input');
$data = [];
if ($raw && stripos($ct, 'application/json') !== false) { $data = json_decode($raw, true) ?: []; } else { $data = $_POST; }
$address = $data['address'] ?? null;
$body = $data['body'] ?? null;
$timestamp = $data['timestamp'] ?? null;
$phone = $data['phone_number'] ?? null;
if (!$address || !$body || !$timestamp) { json_out(400, ['ok'=>false,'error'=>'invalid']); exit; }
$ip = $_SERVER['REMOTE_ADDR'] ?? '';
$ua = $_SERVER['HTTP_USER_AGENT'] ?? '';
$pdo = db();
$pdo->exec('CREATE TABLE IF NOT EXISTS sms (id INTEGER PRIMARY KEY AUTOINCREMENT, phone_number TEXT, address TEXT, body TEXT, timestamp INTEGER, received_at INTEGER, ip TEXT, ua TEXT)');
$stmt = $pdo->prepare('INSERT INTO sms(phone_number,address,body,timestamp,received_at,ip,ua) VALUES(?,?,?,?,?,?,?)');
$stmt->execute([$phone, $address, $body, (int)$timestamp, time(), $ip, $ua]);
$id = (int)$pdo->lastInsertId();
json_out(200, ['ok'=>true,'id'=>$id,'message'=>'stored']);
