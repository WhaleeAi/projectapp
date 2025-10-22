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
  $last = $u['last_login_date'] ?? $u['created_date'];
  if ($last && strtotime($last) < strtotime('-1 month')) {
    // Блокируем
    $pdo->prepare('UPDATE `user` SET is_active=0, locked_at=NOW(), locked_reason="INACTIVE" WHERE user_id=?')
        ->execute([$u['user_id']]);
    flash_set('error', 'Вы заблокированы. Обратитесь к администратору');
    header('Location: login.php');
    exit;
  }

  if ((int)$u['is_active'] === 0) {
    flash_set('error', 'Вы заблокированы. Обратитесь к администратору');
    header('Location: login.php');
    exit;
  }

  // Проверка пароля: приоритет salt+hash; если их нет — сравниваем со старым plaintext (если ещё есть)
  $ok = false;
  if (!empty($u['password_salt']) && !empty($u['password_hash'])) {
    $hash = password_hash_with_salt($password, $u['password_salt']);
    $ok = hash_equals($u['password_hash'], $hash);
  } else {
    // Переходный период — сравнение со старым plaintext (если есть)
    if (isset($u['password']) && $u['password'] !== null) {
      $ok = hash_equals($u['password'], $password);
      if ($ok) {
        // Переводим на соль+хеш
        $salt = random_salt();
        $hash = password_hash_with_salt($password, $salt);
        $q = $pdo->prepare('UPDATE `user` SET password_salt=?, password_hash=? WHERE user_id=?');
        $q->execute([$salt, $hash, $u['user_id']]);
        // Обновим в $u, чтобы дальше использовать
        $u['password_salt'] = $salt;
        $u['password_hash'] = $hash;
      }
    }
  }

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
    header('Location: /change_password.php');
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
