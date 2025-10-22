<?php
// login.php
require_once __DIR__.'/config.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $login = trim($_POST['login'] ?? '');
  $password = $_POST['password'] ?? '';

  if ($login === '' || $password === '') {
    flash_set('error', 'Логин и пароль обязательны для заполнения');
    header('Location: login.php');
    exit;
  }

  $pdo = db();
  // Берём пользователя
  $stmt = $pdo->prepare('SELECT * FROM `user` WHERE login = ? LIMIT 1');
  $stmt->execute([$login]);
  $u = $stmt->fetch();

  // Не раскрываем, что логина нет — общая ошибка
  if (!$u) {
    flash_set('error', 'Вы ввели неверный логин или пароль. Пожалуйста проверьте ещё раз введенные данные');
    header('Location: login.php');
    exit;
  }

  // Проверка блокировки по неактивности (>=1 месяц)
  $lastLoginRaw = $u['last_login_date'] ?? null;
  $createdRaw   = $u['created_date'] ?? null;
  $lastTs    = $lastLoginRaw ? strtotime($lastLoginRaw) : false;
  $createdTs = $createdRaw ? strtotime($createdRaw) : false;
  $monthAgoTs = strtotime('-1 month');

  if (($lastTs !== false && $lastTs < $monthAgoTs) ||
      ($lastTs === false && $createdTs !== false && $createdTs < $monthAgoTs)) {
    $pdo->prepare('UPDATE `user` SET is_active=0, locked_at=NOW(), locked_reason="INACTIVE" WHERE user_id=?')
        ->execute([$u['user_id']]);
    flash_set('error', 'Вы заблокированы. Обратитесь к администратору');
    header('Location: login.php');
    exit;
  }

  if (!db_flag_is_true($u['is_active'] ?? null)) {
    flash_set('error', 'Вы заблокированы. Обратитесь к администратору');
    header('Location: login.php');
    exit;
  }

  // $u — строка из БД с полями password_salt, password_hash, password

function norm_bin($v) {
  if ($v === null) return null;
  // Если пришла hex-строка — превратим в бинарь
  if (preg_match('/^[0-9a-fA-F]+$/', $v) && (strlen($v) % 2 === 0)) {
    return hex2bin($v);
  }
  return $v; // уже бинарь
}

function h512_hexsalt($salt_bin, $password) {
  $hexSalt = bin2hex($salt_bin);                 // HEX(salt)
  return hex2bin(hash('sha512', $hexSalt.$password)); // бинарный 64 байта
}
function h512_binsalt($salt_bin, $password) {
  // SHA512( (binary)salt || password ), бинарный вывод:
  $ctx = hash_init('sha512');
  hash_update($ctx, $salt_bin);
  hash_update($ctx, $password);
  return hex2bin(hash_final($ctx));
}

$db_salt = norm_bin($u['password_salt'] ?? null);
$db_hash = norm_bin($u['password_hash'] ?? null);
$db_plain= $u['password'] ?? null;

$ok = false;

// Попробуем 2 стратегии хеширования, если в БД есть соль и хеш
if ($db_salt !== null && $db_hash !== null) {
  // Вариант A: SHA512(HEX(salt) || password)
  $calcA = h512_hexsalt($db_salt, $password);
  if (hash_equals($db_hash, $calcA)) {
    $ok = true;
  } else {
    // Вариант B: SHA512((binary)salt || password)
    $calcB = h512_binsalt($db_salt, $password);
    if (hash_equals($db_hash, $calcB)) {
      $ok = true;
    }
  }
}

// Переходный fallback: если в БД ещё есть plaintext
if (!$ok && $db_plain !== null) {
  if (hash_equals($db_plain, $password)) {
    $ok = true;
    // Можно сразу перевести на соль+хеш по единому варианту (A), чтобы унифицировать:
    // Генерируем соль из UUID (MariaDB-совместимо)
    $salt = bin2hex(random_bytes(16)); // hex на 32 символа
    $salt_bin = hex2bin($salt);
    $hash_bin = h512_hexsalt($salt_bin, $password);
    $q = $pdo->prepare('UPDATE `user` SET password_salt=?, password_hash=?, password_changed=1 WHERE user_id=?');
    $q->execute([$salt_bin, $hash_bin, $u['user_id']]);
    $u['password_salt'] = $salt_bin;
    $u['password_hash'] = $hash_bin;
  }
}

// --- дальше твоя логика: if (!$ok) { ошибка } else { успех } ---


  if (!$ok) {
    // инкремент попыток и возможная блокировка
    $attempts = (int)$u['login_attempts'] + 1;
    if ($attempts >= 3) {
      $pdo->prepare('UPDATE `user` SET is_active=0, locked_at=NOW(), locked_reason="FAILED_LOGINS" WHERE user_id=?')
          ->execute([$u['user_id']]);
      flash_set('error', 'Вы заблокированы. Обратитесь к администратору');
    } else {
      $pdo->prepare('UPDATE `user` SET login_attempts=? WHERE user_id=?')->execute([$attempts, $u['user_id']]);
      flash_set('error', 'Вы ввели неверный логин или пароль. Пожалуйста проверьте ещё раз введенные данные');
    }
    header('Location: login.php');
    exit;
  }

  // успех: сброс попыток, отметка времени
  $pdo->prepare('UPDATE `user` SET login_attempts=0, last_login_date=NOW() WHERE user_id=?')
      ->execute([$u['user_id']]);

  // Сессия
  $_SESSION['user'] = [
    'user_id'    => $u['user_id'],
    'login'      => $u['login'],
    'first_name' => $u['first_name'],
    'last_name'  => $u['last_name'],
    'auth_role'  => $u['auth_role'] ?: 'Пользователь',
    'password_changed' => (int)$u['password_changed'],
  ];

  if ((int)$u['password_changed'] === 0) {
    // Требуется смена пароля
    flash_set('info', 'Вы успешно авторизовались. Требуется сменить пароль.');
    header('Location: change_password.php');
    exit;
  }

  flash_set('success', 'Вы успешно авторизовались');
  header('Location: home.php');
  exit;
}

// GET: форма
$err = flash_get('error');
$ok  = flash_get('success');
$info= flash_get('info');
?>
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <title>Авторизация</title>
  <style>
    body{font-family:system-ui,Arial;margin:40px}
    form{max-width:360px;margin:auto;display:flex;flex-direction:column;gap:12px}
    input{padding:10px;border:1px solid #ccc;border-radius:8px}
    button{padding:10px;border:0;border-radius:8px;cursor:pointer}
    .btn{background:#111;color:#fff}
    .msg{max-width:360px;margin:10px auto;padding:10px;border-radius:8px}
    .error{background:#ffe0e0}
    .ok{background:#e0ffe8}
    .info{background:#e8f0ff}
  </style>
</head>
<body>
  <?php if($err):?><div class="msg error"><?=htmlspecialchars($err)?></div><?php endif;?>
  <?php if($ok):?><div class="msg ok"><?=htmlspecialchars($ok)?></div><?php endif;?>
  <?php if($info):?><div class="msg info"><?=htmlspecialchars($info)?></div><?php endif;?>

  <form method="post" action="login.php">
    <h2>Вход</h2>
    <label>Логин
      <input type="text" name="login" required>
    </label>
    <label>Пароль
      <input type="password" name="password" required>
    </label>
    <button class="btn" type="submit">Войти</button>
  </form>
</body>
</html>
