package app.view;

import app.dao.ProjectDAO;
import app.model.ClientItem;
import app.model.TariffItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AddProjectView extends Stage {
    private final TextField nameField = new TextField();
    private final TextArea descField = new TextArea();
    private final TextField statusField = new TextField();
    private final DatePicker startDatePicker = new DatePicker();
    private final DatePicker endDatePicker = new DatePicker();
    private final TextField budgetField = new TextField();
    private final ComboBox<app.model.ProjectStatus> statusBox = new ComboBox<>();
    private final ComboBox<TariffItem> tariffBox = new ComboBox<>();
    private final ComboBox<ClientItem> clientBox = new ComboBox<>();
    // фрагмент внутри класса AddProjectDialog
    private final TextField computersField = new TextField();
    private final TextArea successCriteriaField = new TextArea();


    private final Label msg = new Label();

    private final ProjectDAO dao;
    private boolean success = false;

    public AddProjectView(ProjectDAO dao) {
        this.dao = dao;

        setTitle("Добавляем проект");
        initModality(Modality.APPLICATION_MODAL);

        setMinWidth(800);
        setMinHeight(600);

        // Форма
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.add(new Label("Название:"), 0, 0);           form.add(nameField, 1, 0);
        form.add(new Label("Клиент:"), 0, 9);  form.add(clientBox, 1, 9);
        form.add(new Label("Описание:"), 0, 1);           {
            descField.setPrefRowCount(4);
            form.add(descField, 1, 1);
        }
        form.add(new Label("Статус:"), 0, 2);             form.add(statusBox, 1, 2);
        form.add(new Label("Дата начала:"), 0, 3);        form.add(startDatePicker, 1, 3);
        form.add(new Label("Дата окончания:"), 0, 4);     form.add(endDatePicker, 1, 4);
        form.add(new Label("Бюджет:"), 0, 5);             form.add(budgetField, 1, 5);
        form.add(new Label("Тариф:"), 0, 6);   form.add(tariffBox, 1, 6);
        form.add(new Label("Кол-во компьютеров:"), 0, 7);   form.add(computersField, 1, 7);
        form.add(new Label("Критерий успеха:"), 0, 8);
        { successCriteriaField.setPrefRowCount(3); form.add(successCriteriaField, 1, 8); }


        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPrefWidth(140);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c0, c1);

        // Кнопки
        Button btnSave = new Button("Сохранить");
        Button btnCancel = new Button("Отмена");
        HBox buttons = new HBox(8, btnSave, btnCancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12,
                form,
                msg,
                new Separator(),
                buttons
        );
        root.setPadding(new Insets(12));

        btnCancel.setOnAction(e -> close());
        btnSave.setOnAction(e -> onSave());

        setScene(new Scene(root, 540, 420));

        // Загрузка тарифов в выпадающий список
        try {
            tariffBox.getItems().setAll(dao.listTariffs());
            tariffBox.setEditable(false);
            tariffBox.setPromptText("выберите тариф");
            } catch (Exception ex) {
            msg.setText("Не удалось загрузить тарифы: " + ex.getMessage());
        }

        // клиенты
        try {
            clientBox.getItems().setAll(dao.listClients());
            clientBox.setEditable(false);
            clientBox.setPromptText("выберите клиента");
        } catch (Exception ex) { msg.setText("Не удалось загрузить клиентов: " + ex.getMessage()); }

        // Статусы проекта (жёстко заданный список)
        statusBox.getItems().setAll(app.model.ProjectStatus.values());
        statusBox.setEditable(false);
        statusBox.setPromptText("выберите статус");
        statusBox.getSelectionModel().select(app.model.ProjectStatus.PLANNING);
    }

    private void onSave() {
        try {
            String name = nameField.getText();
            String desc = descField.getText();

            app.model.ProjectStatus statusEnum = statusBox.getValue();
            if (statusEnum == null) {
                msg.setText("Выберите статус проекта.");
                return;
            }

            String status = statusEnum.getDbValue();
            String start = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : null;
            String end   = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : null;
            double budget = budgetField.getText().isBlank() ? 0.0 : Double.parseDouble(budgetField.getText());

            if (name == null || name.isBlank()) {
                msg.setText("Укажите название проекта.");
                return;
            }
            if (status == null || status.isBlank()) {
                msg.setText("Укажите статус проекта.");
                return;
            }

            String sc = successCriteriaField.getText();
            if (sc == null || sc.isBlank()) {
                msg.setText("Укажите критерий успеха.");
                return;
            }
            Integer tariffId = (tariffBox.getValue() == null) ? null : tariffBox.getValue().getId();
            Integer computers = computersField.getText().isBlank() ? null : Integer.parseInt(computersField.getText());
            Integer clientId = (clientBox.getValue() == null) ? null : clientBox.getValue().getId();

            boolean ok = dao.insertProject(
                    name, desc, status, start, end, budget,
                    sc, tariffId, computers, clientId
            );

            if (ok) {
                success = true;
                close();
            } else {
                msg.setText("Не удалось сохранить проект.");
            }
        } catch (NumberFormatException nfe) {
            msg.setText("Бюджет должен быть числом.");
        } catch (Exception ex) {
            msg.setText("Ошибка: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public boolean isSuccess() {
        return success;
    }
}
