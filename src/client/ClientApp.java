package client;

import client.controllers.LoginController;
import javafx.application.Application;
import javafx.stage.Stage;


public class ClientApp extends Application {

    private final ServerConnection connection = new ServerConnection();

    @Override
    public void start(Stage primaryStage) {
        if (!connection.connect()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur de connexion");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de se connecter au serveur.\nVérifiez que le serveur est démarré (RG10).");
            alert.showAndWait();
            return;
        }

        LoginController login = new LoginController(primaryStage, connection);
        login.show();
    }

    @Override
    public void stop() {
        connection.disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}