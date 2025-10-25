package app.util;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.*;
import java.math.BigInteger;

public class TestCaseDoc {
    private final File file;

    public TestCaseDoc(String path) { this.file = new File(path); }

    public void ensureTemplate() throws Exception {
        if (file.exists()) return;
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tr = title.createRun();
            tr.setBold(true); tr.setFontSize(14); tr.setText("Тест-кейсы валидации email");

            XWPFTable tbl = doc.createTable(3, 3);
            set(tbl, 0, 0, "Действие");
            set(tbl, 0, 1, "Ожидаемый результат");
            set(tbl, 0, 2, "Результат");

            set(tbl, 1, 0, "Валидация email — формат");
            set(tbl, 1, 1, "Email соответствует шаблону name@domain.tld");
            setWithBookmark(tbl, 1, 2, "—", "res_email_format");

            set(tbl, 2, 0, "Валидация email — доменная часть");
            set(tbl, 2, 1, "Домен корректен (MX/проверка домена)");
            setWithBookmark(tbl, 2, 2, "—", "res_email_domain");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                doc.write(fos);
            }
        }
    }

    public void updateBookmark(String bookmarkName, String value) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            boolean updated = false;
            // абзацы
            for (XWPFParagraph p : doc.getParagraphs()) {
                updated |= updateInParagraph(p, bookmarkName, value);
            }
            // таблицы
            if (!updated) {
                for (XWPFTable t : doc.getTables()) {
                    for (XWPFTableRow r : t.getRows()) {
                        for (XWPFTableCell cell : r.getTableCells()) {
                            for (XWPFParagraph p : cell.getParagraphs()) {
                                updated |= updateInParagraph(p, bookmarkName, value);
                            }
                        }
                    }
                }
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                doc.write(fos);
            }
        }
    }

    private static boolean updateInParagraph(XWPFParagraph p, String name, String val) {
        CTP ctp = p.getCTP();
        for (CTBookmark bm : ctp.getBookmarkStartList()) {
            if (name.equals(bm.getName())) {
                XWPFRun run = p.createRun();
                run.setText(val);
                return true;
            }
        }
        return false;
    }

    private static void set(XWPFTable t, int r, int c, String text) {
        XWPFTableCell cell = t.getRow(r).getCell(c);
        if (!cell.getParagraphs().isEmpty()) cell.removeParagraph(0);
        XWPFRun run = cell.addParagraph().createRun();
        run.setText(text);
    }

    private static void setWithBookmark(XWPFTable t, int r, int c, String text, String name) {
        XWPFTableCell cell = t.getRow(r).getCell(c);
        if (!cell.getParagraphs().isEmpty()) cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        CTP ctp = p.getCTP();
        CTBookmark start = ctp.addNewBookmarkStart();
        start.setId(BigInteger.valueOf(System.nanoTime() & 0x7fffffff));
        start.setName(name);
        p.createRun().setText(text);
        CTMarkupRange end = ctp.addNewBookmarkEnd();
        end.setId(start.getId());
    }
}
