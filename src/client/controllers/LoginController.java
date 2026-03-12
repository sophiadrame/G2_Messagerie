package client.controllers;

import client.ServerConnection;
import common.NetworkPacket;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.User;

public class LoginController {

    private final Stage stage;
    private final ServerConnection connection;

    private TextField usernameField;
    private PasswordField passwordField;
    private ComboBox<User.Role> roleCombo;
    private Label statusLabel;

    public LoginController(Stage stage, ServerConnection connection) {
        this.stage = stage;
        this.connection = connection;
    }

    public void show() {

        // ── Titre ────────────────────────────────────────────────────────────
        Label title = new Label("💬 Messagerie G2");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#1a73e8"));

        Label subtitle = new Label("Connectez-vous à votre espace");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web("#888888"));

        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // ── Champs ───────────────────────────────────────────────────────────
        usernameField = styledTextField("👤  Nom d'utilisateur");
        passwordField = new PasswordField();
        passwordField.setPromptText("🔒  Mot de passe");
        styleInput(passwordField);

        roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(User.Role.values());
        roleCombo.setValue(User.Role.MEMBRE);
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setStyle(
                "-fx-background-color: #f8f9fa;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 8 12 8 12;"
        );

        // ── Boutons ──────────────────────────────────────────────────────────
        Button loginBtn = primaryButton("Se connecter");
        Button registerBtn = outlineButton("Créer un compte");

        loginBtn.setOnAction(e -> handleLogin());
        registerBtn.setOnAction(e -> handleRegister());
        passwordField.setOnAction(e -> handleLogin());

        Label orLabel = new Label("── ou ──");
        orLabel.setTextFill(Color.web("#aaaaaa"));
        orLabel.setFont(Font.font("Segoe UI", 11));

        statusLabel = new Label();
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(300);
        statusLabel.setAlignment(Pos.CENTER);

        // ── Card ─────────────────────────────────────────────────────────────
        VBox card = new VBox(14,
                titleBox,
                new Separator(),
                usernameField,
                passwordField,
                roleCombo,
                loginBtn,
                orLabel,
                registerBtn,
                statusLabel
        );
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(35, 40, 35, 40));
        card.setMaxWidth(360);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 4);"
        );

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: #f0f4ff;");
        root.setPadding(new Insets(40));

        connection.setOnPacketReceived(this::handleServerResponse);
        connection.setOnDisconnected(() ->
                Platform.runLater(() -> setStatus("❌ Connexion perdue (RG10)", true)));

        stage.setTitle("Messagerie G2 — Connexion");
        stage.setScene(new Scene(root, 460, 580));
        stage.setResizable(false);
        stage.show();
    }

    // ── Helpers styles ────────────────────────────────────────────────────────
    private TextField styledTextField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        styleInput(f);
        return f;
    }

    private void styleInput(Control field) {
        String normal = "-fx-background-color:#f8f9fa;-fx-border-color:#dee2e6;-fx-border-radius:8;-fx-background-radius:8;-fx-font-size:13px;-fx-padding:10 12 10 12;";
        String focused = "-fx-background-color:white;-fx-border-color:#1a73e8;-fx-border-width:2;-fx-border-radius:8;-fx-background-radius:8;-fx-font-size:13px;-fx-padding:10 12 10 12;";
        field.setStyle(normal);
        field.focusedProperty().addListener((obs, old, f) -> field.setStyle(f ? focused : normal));
    }

    private Button primaryButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        String normal = "-fx-background-color:#1a73e8;-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:12 0 12 0;-fx-cursor:hand;";
        String hover  = "-fx-background-color:#1557b0;-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:12 0 12 0;-fx-cursor:hand;";
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));
        return b;
    }

    private Button outlineButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        String normal = "-fx-background-color:white;-fx-text-fill:#1a73e8;-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:8;-fx-border-color:#1a73e8;-fx-border-radius:8;-fx-padding:11 0 11 0;-fx-cursor:hand;";
        String hover  = "-fx-background-color:#e8f0fe;-fx-text-fill:#1a73e8;-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:8;-fx-border-color:#1a73e8;-fx-border-radius:8;-fx-padding:11 0 11 0;-fx-cursor:hand;";
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));
        return b;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            setStatus("⚠️ Remplissez tous les champs.", true);
            return;
        }
        connection.login(username, password);
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        User.Role role = roleCombo.getValue();
        if (username.isEmpty() || password.isEmpty() || role == null) {
            setStatus("⚠️ Remplissez tous les champs.", true);
            return;
        }
        connection.register(username, password, role);
    }

    private void handleServerResponse(NetworkPacket packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                case SUCCESS -> {
                    if (packet.getPayload() instanceof User user) {
                        new MainController(stage, connection, user).show();
                    } else {
                        setStatus("✅ Inscription réussie ! Connectez-vous.", false);
                    }
                }
                case ERROR -> setStatus("❌ " + packet.getInfo(), true);
                default -> {}
            }
        });
    }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(isError ? Color.web("#e53935") : Color.web("#43a047"));
    }
}