package app.view;

import app.model.User;
import app.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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

        // --- ВАЖНО: не растягивать детей по ширине
        setFillWidth(false);                       // ⟵ ключевая строка
        setPadding(new Insets(24));
        setSpacing(12);
        setAlignment(Pos.CENTER);

        // стили экрана логина
        getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());

        Label title = new Label("Вход в систему");
        title.getStyleClass().addAll("h1", "login-title");

        TextField login = new TextField();
        login.getStyleClass().add("text-field");
        login.setPromptText("Логин (email)");

        PasswordField pass = new PasswordField();
        pass.getStyleClass().add("password-field");
        pass.setPromptText("Пароль");

        Button btn = new Button("Войти");
        Label status = new Label();
        status.getStyleClass().addAll("login-status", "msg-info");
        status.setWrapText(true);

        // --- ограничиваем максимальную ширину элементов (не будут тянуться)
        int fieldMax = 320;                        // можно подправить под дизайн
        login.setMaxWidth(fieldMax);
        pass.setMaxWidth(fieldMax);
        btn.setMaxWidth(fieldMax);

        VBox card = new VBox(10, title, login, pass, btn, status);
        card.getStyleClass().addAll("card", "login-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);                     // ⟵ ширина карточки-формы
        // (необязательно, но можно задать и prefWidth)
        // card.setPrefWidth(420);

        getChildren().add(card);

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

                if (res != null) {
                    if ("PASSWORD_CHANGE_REQUIRED".equalsIgnoreCase(res.status) && res.user != null) {
                        new Alert(Alert.AlertType.INFORMATION, "Требуется смена пароля").showAndWait();
                        Scene s = new Scene(new PasswordChangeView(stage, res.user), 520, 400);
                        app.MainApp.applyGlobalStyles(s);
                        stage.setScene(s);
                        return;
                    }
                    if (isSuccess(res.status) && res.user != null && !res.user.isPasswordChangeRequired()) {
                        new Alert(Alert.AlertType.INFORMATION, MSG_SUCCESS).showAndWait();
                        Scene s = new Scene(new MainView(stage, res.user), 920, 600);
                        app.MainApp.applyGlobalStyles(s);
                        stage.setScene(s);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Ошибка при входе: " + t.getMessage()).showAndWait();
            }
        });
    }

    private boolean isInvalid(String status) {
        if (status == null) return true;
        String s = status.toUpperCase();
        return s.contains("INVALID") || s.contains("BAD_CREDENTIAL") || s.contains("WRONG_PASSWORD");
    }

    private boolean isLocked(String status) {
        if (status == null) return false;
        String s = status.toUpperCase();
        return s.contains("LOCK") || s.contains("BLOCK");
    }

    private boolean isSuccess(String status) {
        return status != null && status.equalsIgnoreCase("SUCCESS");
    }
}
