package app;

import app.view.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static void applyGlobalStyles(Scene scene) {
        scene.getStylesheets().add(
                MainApp.class.getResource("/styles/app.css").toExternalForm()
        );
    }

    @Override
    public void start(Stage stage) {
        LoginView root = new LoginView(stage);
        Scene scene = new Scene(root, 520, 420);
        applyGlobalStyles(scene);

        stage.setTitle("Компания заказчика");

        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/icons/logo.png"))
        );

        stage.setScene(scene);

        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
