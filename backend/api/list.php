<?php
declare(strict_types=1);
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
require_once __DIR__ . '/../config.php';
if (!check_key()) { json_out(401, ['ok'=>false,'error'=>'unauthorized']); exit; }
$phone = $_GET['phone_number'] ?? null;
$limit = isset($_GET['limit']) ? max(1, min(1000, (int)$_GET['limit'])) : 100;
$since = isset($_GET['since']) ? (int)$_GET['since'] : null;
$pdo = db();
$pdo->exec('CREATE TABLE IF NOT EXISTS sms (id INTEGER PRIMARY KEY AUTOINCREMENT, phone_number TEXT, address TEXT, body TEXT, timestamp INTEGER, received_at INTEGER, ip TEXT, ua TEXT)');
$sql = 'SELECT id, phone_number, address, body, timestamp, received_at FROM sms';
$cond = [];
$params = [];
if ($phone !== null && $phone !== '') { $cond[] = 'phone_number = ?'; $params[] = $phone; }
if ($since !== null) { $cond[] = 'timestamp >= ?'; $params[] = $since; }
if ($cond) { $sql .= ' WHERE ' . implode(' AND ', $cond); }
$sql .= ' ORDER BY id DESC LIMIT ?';
$params[] = $limit;
$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
echo json_encode($rows, JSON_UNESCAPED_UNICODE);
