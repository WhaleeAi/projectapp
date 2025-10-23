package app;

import app.view.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        LoginView root = new LoginView(stage);
        Scene scene = new Scene(root, 520, 420);

        // Подключаем глобальный стиль
        scene.getStylesheets().add(
                getClass().getResource("/styles/app.css").toExternalForm()
        );

        stage.setTitle("Project Management — Desktop");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
