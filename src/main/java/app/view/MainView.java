package app.view;

import app.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainView extends BorderPane {
    private final User user;

    public MainView(Stage stage, User user) {
        this.user = user;
        setPadding(new Insets(16));

        Label hello = new Label(
                "Здравствуйте, " +
                        (user.getDisplayName().isBlank() ? user.getLogin() : user.getDisplayName()) +
                        " | Роль: " + user.getAuthRole()
        );
        hello.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Button logout = new Button("Выход");
        logout.setOnAction(e -> stage.setScene(new javafx.scene.Scene(new LoginView(stage), 480, 360)));

        HBox top = new HBox(16, hello, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        setTop(top);

        if (isAdmin(user.getAuthRole())) {
            // Панель администратора: управление пользователями
            setCenter(new AdminUsersView());
        } else {
            // Обычный пользователь — заглушка
            VBox center = new VBox(12);
            center.setPadding(new Insets(12));
            center.getChildren().add(new Label("Добро пожаловать! Здесь появится рабочий стол пользователя."));
            setCenter(center);
        }
    }

    private boolean isAdmin(String role) {
        if (role == null) return false;
        String r = role.trim().toLowerCase();
        return r.contains("admin") || r.contains("администратор");
    }
}
