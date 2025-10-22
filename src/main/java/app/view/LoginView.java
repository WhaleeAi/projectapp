package app.view;

import app.model.User;
import app.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView extends VBox {
    private final AuthService auth = new AuthService();
    private final Stage stage;

    private static final String MSG_INVALID = "Вы ввели неверный логин или пароль. Пожалуйста проверьте ещё раз введенные данные";
    private static final String MSG_BLOCKED = "Вы заблокированы. Обратитесь к администратору";
    private static final String MSG_SUCCESS = "Вы успешно авторизовались";

    public LoginView(Stage stage) {
        this.stage = stage;
        setPadding(new Insets(24));
        setSpacing(12);
        setAlignment(Pos.CENTER);

        Label title = new Label("Вход в систему");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextField login = new TextField();
        login.setPromptText("Логин (email)");

        PasswordField pass = new PasswordField();
        pass.setPromptText("Пароль");

        Button btn = new Button("Войти");
        Label status = new Label();
        status.setWrapText(true);

        btn.setDefaultButton(true);
        btn.setOnAction(e -> {
            try {
                String lg = login.getText() == null ? "" : login.getText().trim();
                String pw = pass.getText()  == null ? "" : pass.getText().trim();

                if (lg.isEmpty() || pw.isEmpty()) {
                    status.setText("Поля «Логин» и «Пароль» обязательны для заполнения.");
                    return;
                }

                AuthService.AuthResult res = auth.login(lg, pw);

                // Нормализуем отображаемые сообщения по ТЗ
                String normalizedMsg;
                if (res == null || res.status == null) {
                    normalizedMsg = "Ошибка: не удалось выполнить авторизацию (нет ответа).";
                } else if (isLocked(res.status)) {
                    normalizedMsg = MSG_BLOCKED;
                } else if (isInvalid(res.status)) {
                    normalizedMsg = MSG_INVALID;
                } else if (isSuccess(res.status)) {
                    normalizedMsg = MSG_SUCCESS;
                } else if ("PASSWORD_CHANGE_REQUIRED".equalsIgnoreCase(res.status)) {
                    normalizedMsg = "Требуется смена пароля";
                } else if (res.status.startsWith("DB_ERROR")) {
                    normalizedMsg = "Ошибка базы данных: " + res.message;
                } else {
                    normalizedMsg = (res.message == null || res.message.isBlank()) ? res.status : res.message;
                }

                status.setText(normalizedMsg);

                // Переходы по результатам
                if (res != null) {
                    if ("PASSWORD_CHANGE_REQUIRED".equalsIgnoreCase(res.status) && res.user != null) {
                        new Alert(Alert.AlertType.INFORMATION, "Требуется смена пароля").showAndWait();
                        stage.setScene(new javafx.scene.Scene(new PasswordChangeView(stage, res.user), 520, 400));
                        return;
                    }
                    if (isSuccess(res.status) && res.user != null && !res.user.isPasswordChangeRequired()) {
                        new Alert(Alert.AlertType.INFORMATION, MSG_SUCCESS).showAndWait();
                        stage.setScene(new javafx.scene.Scene(new MainView(stage, res.user), 920, 600));
                    }
                }

            } catch (Throwable t) {
                t.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Ошибка при входе: " + t.getMessage()).showAndWait();
            }
        });

        getChildren().addAll(title, login, pass, btn, status);
    }

    private boolean isInvalid(String status) {
        if (status == null) return true;
        String s = status.toUpperCase();
        return s.contains("INVALID") || s.contains("BAD_CREDENTIAL") || s.contains("WRONG_PASSWORD");
    }

    private boolean isLocked(String status) {
        if (status == null) return false;
        String s = status.toUpperCase();
        return s.contains("LOCK") || s.contains("BLOCK"); // LOCKED, BLOCKED, ACCOUNT_LOCKED, INACTIVE_30DAYS, etc.
    }

    private boolean isSuccess(String status) {
        return status != null && status.equalsIgnoreCase("SUCCESS");
    }
}
