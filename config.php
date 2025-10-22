<?php
// config.php
session_start();

const DB_HOST = 'localhost';
const DB_NAME = 'project_management_system';
const DB_USER = 'root';
const DB_PASS = '';

function db(): PDO {
  static $pdo = null;
  if ($pdo === null) {
    $dsn = 'mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4';
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
      PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
      PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
  }
  return $pdo;
}

// ——— Security utils ———

// Считает хеш пароля как SHA-512(HEX(salt)||password) — совместимо с нашей БД
function password_hash_with_salt(string $password, string $saltBinary): string {
  $hexSalt = bin2hex($saltBinary);
  return hex2bin(hash('sha512', $hexSalt.$password));
}

function random_salt(int $len = 16): string {
  return random_bytes($len);
}

// Сообщения в сессию (flash)
function flash_set(string $type, string $msg): void {
  $_SESSION['flash'][$type] = $msg;
}
function flash_get(string $type): ?string {
  if (!empty($_SESSION['flash'][$type])) {
    $msg = $_SESSION['flash'][$type];
    unset($_SESSION['flash'][$type]);
    return $msg;
  }
  return null;
}

function db_flag_is_true($value): bool {
  if (is_bool($value)) {
    return $value;
  }
  if (is_int($value)) {
    return $value === 1;
  }
  if (is_string($value)) {
    $normalized = strtolower(trim($value));
    return !in_array($normalized, ['0', 'false', 'no', 'n'], true);
  }
  return (bool)$value;
}


// Требовать авторизацию
function require_login(): void {
  if (empty($_SESSION['user'])) {
    header('Location: login.php');
    exit;
  }
}

// Требовать роль администратора
function require_admin(): void {
  require_login();
  if (($_SESSION['user']['auth_role'] ?? '') !== 'Администратор') {
    http_response_code(403);
    echo 'Доступ запрещён';
    exit;
  }
}
