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
import model.Message;
import model.User;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

    private final Stage stage;
    private final ServerConnection connection;
    private final User currentUser;

    private ListView<String> userListView;
    private VBox chatBox;
    private ScrollPane chatScroll;
    private TextField messageField;
    private Label chatTitle;
    private String selectedUser = null;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    public MainController(Stage stage, ServerConnection connection, User currentUser) {
        this.stage = stage;
        this.connection = connection;
        this.currentUser = currentUser;
    }

    public void show() {

        // ══════════════════════════════════════════════════════════════════════
        // PANNEAU GAUCHE — Membres en ligne
        // ══════════════════════════════════════════════════════════════════════
        Label appTitle = new Label("💬 G2 Chat");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        appTitle.setTextFill(Color.web("#1a73e8"));

        Label userInfo = new Label("👤 " + currentUser.getUsername());
        userInfo.setFont(Font.font("Segoe UI", 12));
        userInfo.setTextFill(Color.web("#555555"));

        Label roleLabel = new Label(currentUser.getRole().toString());
        roleLabel.setFont(Font.font("Segoe UI", 10));
        roleLabel.setTextFill(Color.WHITE);
        roleLabel.setStyle(
                "-fx-background-color: #1a73e8;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 2 8 2 8;"
        );

        HBox userInfoBox = new HBox(6, userInfo, roleLabel);
        userInfoBox.setAlignment(Pos.CENTER_LEFT);

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #e0e0e0;");

        Label membersLabel = new Label("🟢 Membres en ligne");
        membersLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        membersLabel.setTextFill(Color.web("#444444"));

        userListView = new ListView<>();
        userListView.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;"
        );
        userListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText("  🟢 " + item);
                    setFont(Font.font("Segoe UI", 13));
                    setStyle(isSelected()
                            ? "-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; -fx-background-radius: 8;"
                            : "-fx-background-color: transparent; -fx-text-fill: #333333;");
                }
            }
        });
        VBox.setVgrow(userListView, Priority.ALWAYS);

        userListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, val) -> onUserSelected(val));

        Button refreshBtn = new Button("🔄  Actualiser");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setStyle(
                "-fx-background-color: #e8f0fe;" +
                        "-fx-text-fill: #1a73e8;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 0 8 0;" +
                        "-fx-cursor: hand;"
        );
        refreshBtn.setOnAction(e -> connection.requestOnlineUsers());

        Button logoutBtn = new Button("Se déconnecter");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle(
                "-fx-background-color: #fce8e6;" +
                        "-fx-text-fill: #e53935;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 0 8 0;" +
                        "-fx-cursor: hand;"
        );
        logoutBtn.setOnAction(e -> handleLogout());

        VBox leftPane = new VBox(10,
                appTitle, userInfoBox, sep1,
                membersLabel, userListView,
                refreshBtn, logoutBtn);
        leftPane.setPadding(new Insets(16));
        leftPane.setPrefWidth(210);
        leftPane.setStyle("-fx-background-color: #f8f9fa;");

        // ══════════════════════════════════════════════════════════════════════
        // PANNEAU DROIT — Zone de chat
        // ══════════════════════════════════════════════════════════════════════

        // Header chat
        chatTitle = new Label("Sélectionnez un membre pour chatter");
        chatTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        chatTitle.setTextFill(Color.web("#333333"));

        HBox chatHeader = new HBox(chatTitle);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(14, 20, 14, 20));
        chatHeader.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 0 0 1 0;"
        );

        // Zone messages
        chatBox = new VBox(8);
        chatBox.setPadding(new Insets(16));
        chatBox.setStyle("-fx-background-color: #f0f4ff;");

        chatScroll = new ScrollPane(chatBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background-color: #f0f4ff; -fx-background: #f0f4ff;");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Barre d'envoi
        messageField = new TextField();
        messageField.setPromptText("Écrivez un message...");
        messageField.setStyle(
                "-fx-background-color: #f8f9fa;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 10 16 10 16;"
        );
        messageField.focusedProperty().addListener((obs, old, f) -> messageField.setStyle(f
                ? "-fx-background-color:white;-fx-border-color:#1a73e8;-fx-border-width:2;-fx-border-radius:20;-fx-background-radius:20;-fx-font-size:13px;-fx-padding:10 16 10 16;"
                : "-fx-background-color:#f8f9fa;-fx-border-color:#dee2e6;-fx-border-radius:20;-fx-background-radius:20;-fx-font-size:13px;-fx-padding:10 16 10 16;"
        ));
        HBox.setHgrow(messageField, Priority.ALWAYS);

        Button sendBtn = new Button("➤");
        sendBtn.setStyle(
                "-fx-background-color: #1a73e8;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-min-width: 42;" +
                        "-fx-min-height: 42;" +
                        "-fx-cursor: hand;"
        );
        sendBtn.setOnAction(e -> handleSend());
        messageField.setOnAction(e -> handleSend());

        HBox inputBar = new HBox(10, messageField, sendBtn);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(12, 16, 12, 16));
        inputBar.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        VBox rightPane = new VBox(chatHeader, chatScroll, inputBar);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // ══════════════════════════════════════════════════════════════════════
        // LAYOUT PRINCIPAL
        // ══════════════════════════════════════════════════════════════════════
        HBox root = new HBox(leftPane, rightPane);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        connection.setOnPacketReceived(this::handleServerResponse);
        connection.setOnDisconnected(() ->
                Platform.runLater(() -> showAlert("Connexion perdue", "Le serveur est inaccessible (RG10)")));

        stage.setTitle("Messagerie G2 — " + currentUser.getUsername());
        stage.setScene(new Scene(root, 820, 560));
        stage.setMinWidth(600);
        stage.setMinHeight(450);
        stage.setOnCloseRequest(e -> handleLogout());
        stage.show();

        connection.requestOnlineUsers();
    }

    // ── Sélection membre ──────────────────────────────────────────────────────
    private void onUserSelected(String username) {
        if (username == null) return;
        selectedUser = username;
        chatTitle.setText("💬 Conversation avec " + username);
        chatBox.getChildren().clear();
        connection.requestHistory(username);
    }

    // ── Envoi message ─────────────────────────────────────────────────────────
    private void handleSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        if (selectedUser == null) {
            showAlert("Aucun destinataire", "Sélectionnez un membre dans la liste.");
            return;
        }
        if (text.length() > 1000) {
            showAlert("Message trop long", "Maximum 1000 caractères (RG7).");
            return;
        }
        connection.sendMessage(selectedUser, text);
        addMessageBubble(currentUser.getUsername(), text, true, null);
        messageField.clear();
    }

    // ── Réception paquets serveur ─────────────────────────────────────────────
    private void handleServerResponse(NetworkPacket packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {

                case USER_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<String> users = (List<String>) packet.getPayload();
                    userListView.getItems().setAll(users);
                    userListView.getItems().remove(currentUser.getUsername());
                }

                case MESSAGE_HISTORY -> {
                    chatBox.getChildren().clear();
                    @SuppressWarnings("unchecked")
                    List<Message> history = (List<Message>) packet.getPayload();
                    for (Message msg : history) {
                        boolean mine = msg.getSender().getUsername().equals(currentUser.getUsername());
                        String time = msg.getDateEnvoi() != null ? msg.getDateEnvoi().format(FMT) : "";
                        addMessageBubble(msg.getSender().getUsername(), msg.getContenu(), mine, time);
                    }
                    scrollToBottom();
                }

                case RECEIVE_MESSAGE -> {
                    Message msg = (Message) packet.getPayload();
                    if (msg.getSender().getUsername().equals(selectedUser)) {
                        String time = msg.getDateEnvoi() != null ? msg.getDateEnvoi().format(FMT) : "";
                        addMessageBubble(msg.getSender().getUsername(), msg.getContenu(), false, time);
                        scrollToBottom();
                    } else {
                        showAlert("📩 Nouveau message", "Message de " + msg.getSender().getUsername());
                    }
                }

                case USER_CONNECTED -> {
                    String who = (String) packet.getPayload();
                    if (!userListView.getItems().contains(who) && !who.equals(currentUser.getUsername()))
                        userListView.getItems().add(who);
                }

                case USER_DISCONNECTED -> userListView.getItems().remove((String) packet.getPayload());

                case ERROR -> showAlert("Erreur", packet.getInfo());

                default -> {}
            }
        });
    }

    // ── Bulle de message ──────────────────────────────────────────────────────
    private void addMessageBubble(String sender, String content, boolean mine, String time) {
        Label bubble = new Label(content);
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setFont(Font.font("Segoe UI", 13));
        bubble.setStyle(mine
                ? "-fx-background-color:#1a73e8;-fx-text-fill:white;-fx-background-radius:18 18 4 18;"
                : "-fx-background-color:white;-fx-text-fill:#222222;-fx-background-radius:18 18 18 4;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);"
        );

        String timeStr = (time != null && !time.isEmpty()) ? time : "";
        Label timeLabel = new Label(timeStr);
        timeLabel.setFont(Font.font("Segoe UI", 10));
        timeLabel.setTextFill(Color.web("#aaaaaa"));

        VBox msgBox = new VBox(3, bubble, timeLabel);
        msgBox.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        msgBox.setMaxWidth(500);

        HBox row = new HBox(msgBox);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 8, 2, 8));
        chatBox.getChildren().add(row);
    }

    private void scrollToBottom() {
        chatScroll.layout();
        chatScroll.setVvalue(1.0);
    }

    private void handleLogout() {
        connection.logout();
        connection.disconnect();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}