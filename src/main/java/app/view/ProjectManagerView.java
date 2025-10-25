package app.view;

import app.dao.ProjectDAO;
import app.model.ProjectRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

public class ProjectManagerView extends BorderPane {
    private final ProjectDAO dao = new ProjectDAO();
    private final TableView<ProjectRecord> table = new TableView<>();
    private final Label msg = new Label();

    public ProjectManagerView() {
        setPadding(new Insets(12));

        // === Верхняя панель инструментов ===
        Button btnAdd = new Button("Добавить проект");
        Button btnReload = new Button("Обновить");
        HBox toolbar = new HBox(8, btnAdd, btnReload);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0,0,8,0));

        // === Таблица со всеми полями ===
        TableColumn<ProjectRecord, Integer> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        cId.setPrefWidth(70);

        TableColumn<ProjectRecord, String> cName = new TableColumn<>("Название");
        cName.setCellValueFactory(new PropertyValueFactory<>("name"));
        cName.setPrefWidth(200);

        TableColumn<ProjectRecord, String> cDesc = new TableColumn<>("Описание");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        cDesc.setPrefWidth(300);

        TableColumn<ProjectRecord, String> cClient = new TableColumn<>("Клиент");
        cClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        cClient.setPrefWidth(200);

        TableColumn<ProjectRecord, String> cStatus = new TableColumn<>("Статус");
        cStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        cStatus.setPrefWidth(160);

        TableColumn<ProjectRecord, String> cStart = new TableColumn<>("Дата начала");
        cStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        cStart.setPrefWidth(140);

        TableColumn<ProjectRecord, String> cEnd = new TableColumn<>("Дата окончания");
        cEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        cEnd.setPrefWidth(160);

        TableColumn<ProjectRecord, Double> cBudget = new TableColumn<>("Бюджет");
        cBudget.setCellValueFactory(new PropertyValueFactory<>("budget"));
        cBudget.setPrefWidth(140);

        TableColumn<ProjectRecord, String> cSC = new TableColumn<>("Критерий успеха");
        cSC.setCellValueFactory(new PropertyValueFactory<>("successCriteria"));
        cSC.setPrefWidth(240);

        TableColumn<ProjectRecord, String> cTariff = new TableColumn<>("Тариф");
        cTariff.setCellValueFactory(new PropertyValueFactory<>("tariffName"));
        cTariff.setPrefWidth(180);

        TableColumn<ProjectRecord, Integer> cPC = new TableColumn<>("Компьютеры");
        cPC.setCellValueFactory(new PropertyValueFactory<>("computerCount"));
        cPC.setPrefWidth(120);

        table.getColumns().addAll(cId, cName, cDesc, cStatus, cStart, cEnd, cBudget, cSC, cTariff, cPC, cClient);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);

        VBox center = new VBox(8, toolbar, table, msg);
        setCenter(center);


        // Действия
        btnAdd.setOnAction(e -> openAddDialog());
        btnReload.setOnAction(e -> reload());

        // Двойной клик по строке — модальное окно с деталями
        table.setRowFactory(tv -> {
            TableRow<ProjectRecord> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    showDetailsDialog(row.getItem());
                }
            });
            return row;
        });

        reload();
    }

    private void openAddDialog() {
        AddProjectView dlg = new AddProjectView(dao);
        dlg.showAndWait();
        if (dlg.isSuccess()) {
            msg.setText("Проект добавлен.");
            reload();
        }
    }

    private void reload() {
        try {
            List<ProjectRecord> list = dao.listProjects();
            ObservableList<ProjectRecord> data = FXCollections.observableArrayList(list);
            table.setItems(data);
            msg.setText("Загружено проектов: " + data.size());
        } catch (SQLException ex) {
            msg.setText("Ошибка загрузки: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void showDetailsDialog(ProjectRecord p) {
        if (p == null) return;
        // используем тот же компонент внутри диалога
        ProjectDetailsView pane = new ProjectDetailsView();
        pane.setProject(p);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Информация о проекте");
        dlg.getDialogPane().setContent(pane);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.setResizable(true);
        dlg.getDialogPane().setPrefSize(640, 520);
        dlg.showAndWait();
    }
}
