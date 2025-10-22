<?php
// admin.php
require_once __DIR__.'/config.php';
require_admin();

$pdo = db();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $action = $_POST['action'] ?? '';

  if ($action === 'create') {
    $login = trim($_POST['login'] ?? '');
    $pass  = $_POST['password'] ?? '';
    $first = trim($_POST['first_name'] ?? '');
    $last  = trim($_POST['last_name'] ?? '');
    $role  = $_POST['auth_role'] === 'Администратор' ? 'Администратор' : 'Пользователь';

    if ($login === '' || $pass === '' || $first === '' || $last === '') {
      flash_set('error', 'Заполните все поля для создания пользователя');
      header('Location: admin.php'); exit;
    }

    // проверка дубликата
    $q = $pdo->prepare('SELECT COUNT(*) FROM `user` WHERE login=?');
    $q->execute([$login]);
    if ($q->fetchColumn() > 0) {
      flash_set('error', 'Пользователь с таким логином уже существует');
      header('Location: admin.php'); exit;
    }

    $salt = random_salt();
    $hash = password_hash_with_salt($pass, $salt);

    $stmt = $pdo->prepare('INSERT INTO `user`(login, password_salt, password_hash, first_name, last_name, auth_role, is_active, login_attempts, created_date, updated_date, password_changed)
                           VALUES(?,?,?,?,?,?,1,0,NOW(),NOW(),0)');
    $stmt->execute([$login, $salt, $hash, $first, $last, $role]);

    flash_set('success', 'Пользователь создан');
    header('Location: admin.php'); exit;
  }

  if ($action === 'unlock') {
    $uid = (int)($_POST['user_id'] ?? 0);
    if ($uid > 0) {
      $pdo->prepare('UPDATE `user` SET is_active=1, login_attempts=0, locked_at=NULL, locked_reason=NULL WHERE user_id=?')
          ->execute([$uid]);
      flash_set('success', 'Блокировка снята');
    }
    header('Location: admin.php'); exit;
  }

  if ($action === 'role') {
    $uid = (int)($_POST['user_id'] ?? 0);
    $role = $_POST['auth_role'] === 'Администратор' ? 'Администратор' : 'Пользователь';
    if ($uid > 0) {
      $pdo->prepare('UPDATE `user` SET auth_role=? WHERE user_id=?')->execute([$role, $uid]);
      flash_set('success', 'Роль обновлена');
    }
    header('Location: admin.php'); exit;
  }
}

// список пользователей
$users = $pdo->query('SELECT user_id, login, first_name, last_name, auth_role, is_active, login_attempts, locked_reason, last_login_date FROM `user` ORDER BY user_id')->fetchAll();

$err = flash_get('error');
$ok  = flash_get('success');
?>
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <title>Админ-панель</title>
  <style>
    body{font-family:system-ui,Arial;margin:40px}
    .row{display:flex;gap:24px}
    form{display:flex;flex-direction:column;gap:8px;padding:16px;border:1px solid #ddd;border-radius:10px;max-width:380px}
    input,select{padding:8px;border:1px solid #ccc;border-radius:8px}
    button{padding:10px;border:0;border-radius:8px;cursor:pointer;background:#111;color:#fff}
    table{border-collapse:collapse;margin-top:24px;width:100%}
    th,td{border:1px solid #ddd;padding:8px}
    .msg{padding:10px;border-radius:8px;margin-bottom:16px}
    .error{background:#ffe0e0}
    .ok{background:#e0ffe8}
  </style>
</head>
<body>
  <h2>Админ-панель</h2>
  <p><a href="home.php">← На рабочий стол</a></p>

  <?php if($err):?><div class="msg error"><?=htmlspecialchars($err)?></div><?php endif;?>
  <?php if($ok):?><div class="msg ok"><?=htmlspecialchars($ok)?></div><?php endif;?>

  <div class="row">
    <form method="post">
      <h3>Добавить пользователя</h3>
      <input type="hidden" name="action" value="create">
      <label>Логин <input type="text" name="login" required></label>
      <label>Пароль <input type="password" name="password" required></label>
      <label>Имя <input type="text" name="first_name" required></label>
      <label>Фамилия <input type="text" name="last_name" required></label>
      <label>Роль
        <select name="auth_role">
          <option>Пользователь</option>
          <option>Администратор</option>
        </select>
      </label>
      <button type="submit">Создать</button>
    </form>

    <form method="post">
      <h3>Снять блокировку</h3>
      <input type="hidden" name="action" value="unlock">
      <label>ID пользователя <input type="number" name="user_id" min="1" required></label>
      <button type="submit">Разблокировать</button>
    </form>

    <form method="post">
      <h3>Сменить роль</h3>
      <input type="hidden" name="action" value="role">
      <label>ID пользователя <input type="number" name="user_id" min="1" required></label>
      <label>Новая роль
        <select name="auth_role">
          <option>Пользователь</option>
          <option>Администратор</option>
        </select>
      </label>
      <button type="submit">Обновить роль</button>
    </form>
  </div>

  <h3>Пользователи</h3>
  <table>
    <tr>
      <th>ID</th><th>Логин</th><th>ФИО</th><th>Роль</th><th>Активен</th><th>Попытки</th><th>Причина блокировки</th><th>Последний вход</th>
    </tr>
    <?php foreach($users as $u): ?>
      <tr>
        <td><?=$u['user_id']?></td>
        <td><?=htmlspecialchars($u['login'])?></td>
        <td><?=htmlspecialchars(($u['last_name']??'').' '.($u['first_name']??''))?></td>
        <td><?=htmlspecialchars($u['auth_role']??'')?></td>
        <td><?=$u['is_active'] ? 'Да' : 'Нет'?></td>
        <td><?=$u['login_attempts']?></td>
        <td><?=htmlspecialchars($u['locked_reason']??'')?></td>
        <td><?=htmlspecialchars($u['last_login_date']??'')?></td>
      </tr>
    <?php endforeach;?>
  </table>
</body>
</html>
