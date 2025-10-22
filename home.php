<?php
require_once __DIR__.'/config.php';
require_login();
$user = $_SESSION['user'];
?>
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <title>Рабочий стол</title>
  <style>body{font-family:system-ui,Arial;margin:40px}</style>
</head>
<body>
  <h2>Добро пожаловать, <?=htmlspecialchars($user['first_name'].' '.$user['last_name'])?></h2>
  <p>Ваша роль: <b><?=htmlspecialchars($user['auth_role'])?></b></p>
  <p><a href="admin.php">Админ-панель</a> (видна только администраторам)</p>
  <p><a href="change_password.php">Сменить пароль</a></p>
  <p><a href="logout.php">Выйти</a></p>
</body>
</html>
