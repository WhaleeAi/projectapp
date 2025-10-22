<?php
// change_password.php
require_once __DIR__.'/config.php';
require_login();

$pdo = db();
$user = $_SESSION['user'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $current = $_POST['current_password'] ?? '';
  $new     = $_POST['new_password'] ?? '';
  $confirm = $_POST['new_password_confirm'] ?? '';

  if ($current === '' || $new === '' || $confirm === '') {
    flash_set('error', 'Все поля обязательны для заполнения');
    header('Location: change_password.php');
    exit;
  }
  if ($new !== $confirm) {
    flash_set('error', 'Новый пароль и подтверждение не совпадают');
    header('Location: change_password.php');
    exit;
  }

  // Тянем актуальные password_salt/hash/старый plaintext на случай миграции
  $stmt = $pdo->prepare('SELECT password_salt, password_hash, password, is_active FROM `user` WHERE user_id=?');
  $stmt->execute([$user['user_id']]);
  $u = $stmt->fetch();

  if (!$u || (int)$u['is_active'] === 0) {
    flash_set('error', 'Вы заблокированы. Обратитесь к администратору');
    header('Location: login.php');
    exit;
  }

  // Проверка текущего пароля
  $ok = false;
  if (!empty($u['password_salt']) && !empty($u['password_hash'])) {
    $hash = password_hash_with_salt($current, $u['password_salt']);
    $ok = hash_equals($u['password_hash'], $hash);
  } else {
    if (isset($u['password']) && $u['password'] !== null) {
      $ok = hash_equals($u['password'], $current);
    }
  }
  if (!$ok) {
    flash_set('error', 'Текущий пароль введён неверно');
    header('Location: change_password.php');
    exit;
  }

  // Устанавливаем новый соль + хеш
  $salt = random_salt();
  $hash = password_hash_with_salt($new, $salt);
  $pdo->prepare('UPDATE `user` SET password_salt=?, password_hash=?, password_changed=1, login_attempts=0, is_active=1, locked_at=NULL, locked_reason=NULL WHERE user_id=?')
      ->execute([$salt, $hash, $user['user_id']]);

  // Обновим флаг в сессии
  $_SESSION['user']['password_changed'] = 1;

  flash_set('success', 'Пароль успешно изменён');
  header('Location: home.php');
  exit;
}

// GET: форма
$err = flash_get('error');
$ok  = flash_get('success');
?>
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <title>Смена пароля</title>
  <style>
    body{font-family:system-ui,Arial;margin:40px}
    form{max-width:420px;margin:auto;display:flex;flex-direction:column;gap:12px}
    input{padding:10px;border:1px solid #ccc;border-radius:8px}
    button{padding:10px;border:0;border-radius:8px;cursor:pointer;background:#111;color:#fff}
    .msg{max-width:420px;margin:10px auto;padding:10px;border-radius:8px}
    .error{background:#ffe0e0}
    .ok{background:#e0ffe8}
  </style>
</head>
<body>
  <?php if($err):?><div class="msg error"><?=htmlspecialchars($err)?></div><?php endif;?>
  <?php if($ok):?><div class="msg ok"><?=htmlspecialchars($ok)?></div><?php endif;?>

  <form method="post" action="change_password.php">
    <h2>Смена пароля</h2>
    <label>Текущий пароль
      <input type="password" name="current_password" required>
    </label>
    <label>Новый пароль
      <input type="password" name="new_password" required>
    </label>
    <label>Подтверждение нового пароля
      <input type="password" name="new_password_confirm" required>
    </label>
    <button type="submit">Изменить пароль</button>
  </form>
</body>
</html>
