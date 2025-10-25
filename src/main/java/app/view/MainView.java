package app.view;

import app.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.layout.Priority;

public class MainView extends BorderPane {
    private final User user;
    private final Stage stage;

    public MainView(Stage stage, User user) {
        this.user = user;
        this.stage = stage;
        setPadding(new Insets(16));

        HBox top = new HBox(16);
        top.getStyleClass().add("main-top");

        Label hello = new Label(
                "Здравствуйте, " +
                        (user.getDisplayName().isBlank() ? user.getLogin() : user.getDisplayName()) +
                        " | Роль: " + user.getAuthRole()
        );
        hello.getStyleClass().add("h2");
        HBox.setHgrow(hello, Priority.ALWAYS);

        Button logout = new Button("Выход");
        logout.getStyleClass().add("button-secondary");
        logout.setOnAction(e -> {
            javafx.scene.Scene s = new javafx.scene.Scene(new LoginView(stage), 520, 420);
            app.MainApp.applyGlobalStyles(s);             // ← добавили
            stage.setScene(s);
        });

        Button validation = new Button("Валидация данных");
        validation.getStyleClass().add("button-secondary");
        validation.setOnAction(e -> openValidationWindow());

        top.getChildren().addAll(hello, validation, logout);
        setTop(top);

        if (isAdmin(user.getAuthRole())) {
            AdminUsersView admin = new AdminUsersView();
            setCenter(admin);
        } else if (user.getRole().equalsIgnoreCase("Руководитель проекта")) {
            setCenter(new ProjectManagerView());
        } else {
            VBox center = new VBox(12);
            center.getStyleClass().addAll("container", "card");
            center.getChildren().add(new Label("Добро пожаловать! Здесь появится рабочий стол пользователя."));
            setCenter(center);
        }

        // Подключаем admin.css
        getStylesheets().add(
                getClass().getResource("/styles/admin.css").toExternalForm()
        );

        Button themeToggle = new Button("🌙");
        themeToggle.setOnAction(e -> {
            Scene scene = stage.getScene();
            stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icons/logo.png"))
            );
            scene.getStylesheets().clear();
            String current = themeToggle.getText();
            if ("🌙".equals(current)) {
                scene.getStylesheets().add(getClass().getResource("/styles/dark.css").toExternalForm());
                themeToggle.setText("🌞");
            } else {
                scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
                themeToggle.setText("🌙");
            }
        });
        top.getChildren().add(themeToggle);
    }

    private boolean isAdmin(String role) {
        if (role == null) return false;
        String r = role.trim().toLowerCase();
        return r.contains("admin") || r.contains("администратор");
    }

    private void openValidationWindow() {
        Stage validationStage = new Stage();
        validationStage.initOwner(stage);
        validationStage.initModality(Modality.NONE);
        validationStage.setTitle("Валидация данных клиента");
        validationStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icons/logo.png"))
        );

        EmailValidationView view = new EmailValidationView();
        Scene scene = new Scene(view, 900, 560);
        app.MainApp.applyGlobalStyles(scene);
        validationStage.setScene(scene);
        validationStage.show();
    }
}