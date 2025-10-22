package app.view;

import app.model.User;
import app.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PasswordChangeView extends VBox {
    private final AuthService auth = new AuthService();

    public PasswordChangeView(Stage stage, User user) {
        setPadding(new Insets(24));
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Смена пароля (обязательно)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        PasswordField current = new PasswordField();
        current.setPromptText("Текущий пароль");

        PasswordField np = new PasswordField();
        np.setPromptText("Новый пароль");

        PasswordField nc = new PasswordField();
        nc.setPromptText("Подтверждение пароля");

        Button save = new Button("Изменить пароль");
        Label status = new Label();
        status.setWrapText(true);

        save.setOnAction(e -> {
            String cur = current.getText() == null ? "" : current.getText().trim();
            String n1  = np.getText()       == null ? "" : np.getText().trim();
            String n2  = nc.getText()       == null ? "" : nc.getText().trim();

            if (cur.isEmpty() || n1.isEmpty() || n2.isEmpty()) {
                status.setText("Все поля обязательны для заполнения.");
                return;
            }
            if (!n1.equals(n2)) {
                status.setText("Новый пароль и подтверждение не совпадают.");
                return;
            }

            String msg = auth.changePassword(user.getUserId(), cur, n1, n2);
            status.setText(msg);

            if (msg != null && msg.toUpperCase().startsWith("SUCCESS")) {
                new Alert(Alert.AlertType.INFORMATION, "Пароль успешно изменён").showAndWait();
                stage.setScene(new javafx.scene.Scene(new MainView(stage, user), 920, 600));
            } else if (msg != null && msg.toUpperCase().contains("INVALID_CURRENT")) {
                new Alert(Alert.AlertType.ERROR, "Текущий пароль введён неверно.").showAndWait();
            }
        });

        getChildren().addAll(
                title,
                new Label("Пользователь: " + user.getDisplayName() + " (" + user.getLogin() + ")"),
                current, np, nc, save, status
        );
    }
}
