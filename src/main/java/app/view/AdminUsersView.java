package app.view;

import app.dao.UserDAO;
import app.model.UserRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

public class AdminUsersView extends BorderPane {
    private final UserDAO dao = new UserDAO();
    private final TableView<UserRecord> table = new TableView<>();
    private final TextField search = new TextField();
    private final Label msg = new Label();

    // add form
    private final TextField addLogin = new TextField();
    private final TextField addFirst = new TextField();
    private final TextField addLast  = new TextField();
    private final ComboBox<String> addRole = new ComboBox<>();
    private final PasswordField addPass = new PasswordField();

    // edit form
    private final TextField edFirst = new TextField();
    private final TextField edLast  = new TextField();
    private final ComboBox<String> edRole = new ComboBox<>();
    private final CheckBox edActive = new CheckBox("Активен");

    public AdminUsersView() {
        setPadding(new Insets(12));

        // top: поиск и обновить
        search.setPromptText("Поиск по логину/имени/фамилии");
        Button refresh = new Button("Обновить");
        refresh.setOnAction(e -> reload());
        HBox top = new HBox(8, search, refresh);
        top.setAlignment(Pos.CENTER_LEFT);
        setTop(top);

        // center: таблица
        TableColumn<UserRecord, Integer> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        cId.setPrefWidth(60);

        TableColumn<UserRecord, String> cLogin = new TableColumn<>("Логин");
        cLogin.setCellValueFactory(new PropertyValueFactory<>("login"));
        cLogin.setPrefWidth(180);

        TableColumn<UserRecord, String> cFN = new TableColumn<>("Имя");
        cFN.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        cFN.setPrefWidth(120);

        TableColumn<UserRecord, String> cLN = new TableColumn<>("Фамилия");
        cLN.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        cLN.setPrefWidth(140);

        TableColumn<UserRecord, String> cRole = new TableColumn<>("Роль (БД)");
        cRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        cRole.setPrefWidth(120);

        TableColumn<UserRecord, Boolean> cActive = new TableColumn<>("Активен");
        cActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        cActive.setPrefWidth(80);

        TableColumn<UserRecord, Integer> cAttempts = new TableColumn<>("Попытки");
        cAttempts.setCellValueFactory(new PropertyValueFactory<>("loginAttempts"));
        cAttempts.setPrefWidth(80);

        TableColumn<UserRecord, String> cReason = new TableColumn<>("Причина блокировки");
        cReason.setCellValueFactory(new PropertyValueFactory<>("lockedReason"));
        cReason.setPrefWidth(180);

        table.getColumns().addAll(cId, cLogin, cFN, cLN, cRole, cActive, cAttempts, cReason);
        setCenter(table);

        // left: добавление
        addLogin.setPromptText("Логин (обязательно)");
        addFirst.setPromptText("Имя");
        addLast.setPromptText("Фамилия");
        addRole.setPromptText("Роль");
        addRole.getItems().addAll("Пользователь", "Администратор");
        addRole.getSelectionModel().select("Пользователь");
        addPass.setPromptText("Временный пароль (обязательно)");

        Button addBtn = new Button("Добавить пользователя");
        addBtn.setOnAction(e -> onAddUser());

        VBox left = new VBox(8,
                new Label("Добавить пользователя"),
                addLogin, addFirst, addLast, addRole, addPass, addBtn
        );
        left.setPadding(new Insets(8));
        left.setPrefWidth(300);
        left.setStyle("-fx-background-color: rgba(0,0,0,0.02); -fx-border-color: #ddd;");
        setLeft(left);

        // right: редактирование выбранного
        edRole.getItems().addAll("Пользователь", "Администратор");

        Button saveBtn = new Button("Сохранить изменения");
        saveBtn.setOnAction(e -> onSaveUser());

        Button unlockBtn = new Button("Снять блокировку");
        unlockBtn.setOnAction(e -> onUnlock());

        VBox right = new VBox(8,
                new Label("Изменение пользователя"),
                new Label("Имя"), edFirst,
                new Label("Фамилия"), edLast,
                new Label("Роль"), edRole,
                edActive,
                new HBox(8, saveBtn, unlockBtn)
        );
        right.setPadding(new Insets(8));
        right.setPrefWidth(300);
        right.setStyle("-fx-background-color: rgba(0,0,0,0.02); -fx-border-color: #ddd;");
        setRight(right);

        // bottom: сообщения
        msg.setWrapText(true);
        setBottom(msg);

        // реакция на выбор в таблице
        table.getSelectionModel().selectedItemProperty().addListener((obs, a, sel) -> {
            if (sel != null) {
                edFirst.setText(sel.getFirstName());
                edLast.setText(sel.getLastName());
                edRole.getSelectionModel().select(fromDbRole(sel.getRole()));
                edActive.setSelected(sel.isActive());
            }
        });

        // начальная загрузка
        reload();
    }

    private void reload() {
        try {
            List<UserRecord> list = dao.listUsers(search.getText());
            ObservableList<UserRecord> data = FXCollections.observableArrayList(list);
            table.setItems(data);
            msg.setText("Загружено пользователей: " + data.size());
        } catch (SQLException ex) {
            msg.setText("Ошибка загрузки: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void onAddUser() {
        String login = trim(addLogin.getText());
        String pass  = trim(addPass.getText());
        String fn    = trim(addFirst.getText());
        String ln    = trim(addLast.getText());
        String role  = addRole.getValue();

        if (login.isEmpty() || pass.isEmpty()) {
            msg.setText("Логин и временный пароль обязательны для заполнения.");
            return;
        }
        try {
            if (dao.loginExists(login)) {
                msg.setText("Пользователь с указанным логином уже существует.");
                return;
            }
            int id = dao.insertUser(login, fn, ln, role, pass);
            if (id > 0) {
                msg.setText("Пользователь добавлен (id=" + id + "). При первом входе будет требоваться смена пароля.");
                clearAddForm();
                reload();
            } else {
                msg.setText("Не удалось добавить пользователя.");
            }
        } catch (SQLException ex) {
            msg.setText("Ошибка добавления: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void onSaveUser() {
        UserRecord sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msg.setText("Выберите пользователя в таблице."); return; }

        try {
            boolean ok = dao.updateUser(
                    sel.getUserId(),
                    trim(edFirst.getText()),
                    trim(edLast.getText()),
                    edRole.getValue(),
                    edActive.isSelected()
            );
            if (ok) {
                msg.setText("Изменения сохранены.");
                reload();
            } else {
                msg.setText("Не удалось сохранить изменения.");
            }
        } catch (SQLException ex) {
            msg.setText("Ошибка сохранения: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void onUnlock() {
        UserRecord sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msg.setText("Выберите пользователя в таблице."); return; }
        try {
            boolean ok = dao.resetLock(sel.getUserId());
            if (ok) {
                msg.setText("Блокировка снята.");
                reload();
            } else {
                msg.setText("Не удалось снять блокировку.");
            }
        } catch (SQLException ex) {
            msg.setText("Ошибка разблокировки: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void clearAddForm() {
        addLogin.clear();
        addFirst.clear();
        addLast.clear();
        addPass.clear();
        addRole.getSelectionModel().select("Пользователь");
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private String fromDbRole(String dbRole) {
        if (dbRole == null || dbRole.isBlank()) return "Пользователь";
        String r = dbRole.trim().toLowerCase();
        if (r.contains("админ")) return "Администратор";
        if (r.contains("пользов")) return "Пользователь";
        // если вдруг хранится английское слово
        if (r.contains("admin")) return "Администратор";
        return "Пользователь";
    }
}
