package app.view;

import app.model.ProjectRecord;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProjectDetailsView extends ScrollPane {
    private final Label vId = new Label();
    private final Label vName = new Label();
    private final Label vClient = new Label();
    private final Label vTariff = new Label();
    private final Label vStatus = new Label();
    private final Label vStart = new Label();
    private final Label vEnd = new Label();
    private final Label vBudget = new Label();
    private final Label vComputers = new Label();
    private final Label vSuccess = new Label();
    private final Label vDesc = new Label();

    public ProjectDetailsView() {
        setFitToWidth(true);

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.setPadding(new Insets(12));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPrefWidth(160);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);

        addRow(g, 0, "ID", vId);
        addRow(g, 1, "Название", vName);
        addRow(g, 2, "Клиент", vClient);
        addRow(g, 3, "Тариф", vTariff);
        addRow(g, 4, "Статус", vStatus);
        addRow(g, 5, "Дата начала", vStart);
        addRow(g, 6, "Дата окончания", vEnd);
        addRow(g, 7, "Бюджет", vBudget);
        addRow(g, 8, "Компьютеры", vComputers);
        addRow(g, 9, "Критерий успеха", vSuccess);

        // Описание — отдельным блоком на всю ширину
        VBox box = new VBox(6,
                g,
                new Label("Описание"),
                vDesc
        );
        box.setPadding(new Insets(8, 12, 12, 12));
        vDesc.setWrapText(true);

        setContent(box);
        setPlaceholder();
    }

    private void addRow(GridPane g, int row, String title, Label value) {
        Label k = new Label(title + ":");
        k.getStyleClass().add("muted");
        g.add(k, 0, row);
        g.add(value, 1, row);
        value.setWrapText(true);
    }

    public void setProject(ProjectRecord p) {
        if (p == null) {
            setPlaceholder();
            return;
        }
        vId.setText(String.valueOf(p.getProjectId()));
        vName.setText(nz(p.getName()));
        vClient.setText(nz(p.getClientName()));
        vTariff.setText(nz(p.getTariffName()));
        vStatus.setText(nz(p.getStatus()));
        vStart.setText(nz(p.getStartDate()));
        vEnd.setText(nz(p.getEndDate()));
        vBudget.setText(p.getBudget() == 0.0 ? "—" : String.format("%.2f", p.getBudget()));
        vComputers.setText(p.getComputerCount() == null ? "—" : String.valueOf(p.getComputerCount()));
        vSuccess.setText(nz(p.getSuccessCriteria()));
        vDesc.setText(nz(p.getDescription()));
    }

    private void setPlaceholder() {
        vId.setText("—");
        vName.setText("—");
        vClient.setText("—");
        vTariff.setText("—");
        vStatus.setText("—");
        vStart.setText("—");
        vEnd.setText("—");
        vBudget.setText("—");
        vComputers.setText("—");
        vSuccess.setText("—");
        vDesc.setText("—");
    }

    private static String nz(String s) { return (s == null || s.isBlank()) ? "—" : s; }
}
