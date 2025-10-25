package app.view;

import app.config.AppConfig;
import app.service.EmailValidationService;
import app.util.TestCaseDoc;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class EmailValidationView extends BorderPane {
    private final TextField emailField = new TextField();
    private final Button btnCheck = new Button("Проверить");
    private final Label lblSource = new Label("-");
    private final Label lblResult = new Label("-");
    private final TextArea log = new TextArea();

    private final EmailValidationService service = new EmailValidationService();
    private final TestCaseDoc testDoc = new TestCaseDoc(AppConfig.TESTCASE_DOCX_PATH);

    public EmailValidationView() {
        setPadding(new Insets(12));
        setMinWidth(800);
        setMinHeight(600);

        emailField.setPromptText("Введите email для проверки");
        HBox top = new HBox(8, new Label("Email:"), emailField, btnCheck);
        top.setAlignment(Pos.CENTER_LEFT);
        setTop(top);

        log.setEditable(false);
        log.setWrapText(true);
        setCenter(log);

        VBox bottom = new VBox(6,
                new Label("Источник проверки:"), lblSource,
                new Label("Итог:"), lblResult
        );
        bottom.setPadding(new Insets(10, 0, 0, 0));
        setBottom(bottom);

        btnCheck.setOnAction(e -> onCheck());

        // Гарантируем минимальный размер окна (если добавят в Stage)
        sceneProperty().addListener((obs, os, ns) -> {
            if (ns != null && ns.getWindow() instanceof javafx.stage.Stage st) {
                st.setMinWidth(800);
                st.setMinHeight(600);
            }
        });

        try {
            testDoc.ensureTemplate();
        } catch (Exception ex) {
            append("Ошибка подготовки DOCX: " + ex.getMessage());
        }

        if (AppConfig.EMAIL_API_KEY.isBlank()) {
            append("⚠ Внимание: EMAIL_API_KEY не задан — используется только локальная проверка.");
        }
    }

    private void onCheck() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) { append("Укажите email."); return; }

        var res = service.validate(email);
        lblSource.setText(res.source);
        lblResult.setText(res.allOk() ? "Пройдено" : "Не пройдено: " + res.message);
        append(String.format("Проверка: email=%s, formatOk=%s, domainOk=%s, source=%s, msg=%s",
                email, res.formatOk, res.domainOk, res.source, res.message));

        try {
            testDoc.updateBookmark("res_email_format",
                    res.formatOk ? "Пройдено" : "Не пройдено: неверный формат");
            testDoc.updateBookmark("res_email_domain",
                    res.domainOk ? "Пройдено" : "Не пройдено: доменная часть некорректна");
            append("ТестКейс.docx обновлён.");
        } catch (Exception ex) {
            append("Ошибка записи в DOCX: " + ex.getMessage());
        }
    }

    private void append(String s) {
        log.appendText(s + System.lineSeparator());
    }
}
