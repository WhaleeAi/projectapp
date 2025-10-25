package app.service;

import app.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class EmailValidationService {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Критерий 1: формат (практичный regex)
    private static final String SIMPLE_REGEX =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public static class EmailCheck {
        public final boolean formatOk;
        public final boolean domainOk;
        public final String  message; // краткий итог
        public final String  source;  // "external" | "local" | "mixed"

        public EmailCheck(boolean formatOk, boolean domainOk, String message, String source) {
            this.formatOk = formatOk;
            this.domainOk = domainOk;
            this.message  = message;
            this.source   = source;
        }
        public boolean allOk() { return formatOk && domainOk; }
    }

    public EmailCheck validate(String email) {
        // Локальные проверки
        boolean localFormat = email != null && email.matches(SIMPLE_REGEX);
        boolean localDomain = checkDomainLocally(email);

        // Если есть внешний ключ — пробуем внешнее API
        if (!AppConfig.EMAIL_API_KEY.isBlank()) {
            try {
                var ext = queryExternal(email);
                // Склейка: если внешнее вернулось — пересекаем с локальным, чтобы точно было 2 критерия
                boolean formatOk = localFormat && ext.formatOk;
                boolean domainOk = localDomain && ext.domainOk;
                String msg = (formatOk && domainOk)
                        ? "OK (external)"
                        : buildMsg(formatOk, domainOk, localFormat, localDomain, ext.message);
                return new EmailCheck(formatOk, domainOk, msg, "external");
            } catch (Exception ex) {
                // падать не будем — уйдем в локальный фолбэк
            }
        }

        // Только локальный результат
        String msg = (localFormat && localDomain)
                ? "OK"
                : buildMsg(localFormat, localDomain, localFormat, localDomain, "local checks failed");
        return new EmailCheck(localFormat, localDomain, msg, "local");
    }

    private static boolean checkDomainLocally(String email) {
        if (email == null) return false;
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) return false;
        String domain = email.substring(at + 1);
        return domain.contains(".")
                && !domain.startsWith(".")
                && !domain.endsWith(".")
                && !domain.contains(" ");
    }

    private static String buildMsg(boolean formatOk, boolean domainOk,
                                   boolean localFormat, boolean localDomain, String extMsg) {
        if (!formatOk && !domainOk) {
            // покажем первопричину по формату в приоритете
            return !localFormat ? "Неверный формат email"
                    : "Доменная часть некорректна";
        }
        if (!formatOk) return "Неверный формат email";
        if (!domainOk) return "Доменная часть некорректна";
        return extMsg == null || extMsg.isBlank() ? "OK" : extMsg;
    }

    /** Вызов внешнего API. Специально делаем "устойчивый" парсинг под разные провайдеры */
    private EmailCheck queryExternal(String email) throws Exception {
        String url = AppConfig.EMAIL_API_URL_TEMPLATE.formatted(
                AppConfig.EMAIL_API_KEY,
                URLEncoder.encode(email == null ? "" : email, StandardCharsets.UTF_8)
        );
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("API status " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode root = MAPPER.readTree(resp.body());

        // Попробуем найти флаги по нескольким типовым путям:
        boolean extFormat = anyBool(root,
                n -> n.at("/is_valid_format/value"),
                n -> n.at("/is_valid_format/value_bool"),
                n -> n.at("/format/valid"),
                n -> n.at("/email_deliverability/is_format_valid"),
                n -> n.at("/is_valid_format") // популярный ключ без вложения
        );

        boolean extDomain = anyBool(root,
                n -> n.at("/is_mx_found/value"),
                n -> n.at("/is_mx_found/value_bool"),
                n -> n.at("/mx/valid"),
                n -> n.at("/email_deliverability/is_mx_valid"),
                n -> n.at("/is_mx_found")
        );

        String reason = firstText(root,
                n -> n.at("/deliverability"),
                n -> n.at("/reason"),
                n -> n.at("/quality"),
                n -> n.at("/status")
        );
        if (reason == null || reason.isBlank()) reason = "OK";

        return new EmailCheck(extFormat, extDomain, reason, "external");
    }

    @SafeVarargs
    private static boolean anyBool(JsonNode root, Function<JsonNode, JsonNode>... candidates) {
        for (var f : candidates) {
            JsonNode v = f.apply(root);
            if (v != null && !v.isMissingNode()) {
                if (v.isBoolean()) return v.asBoolean();
                if (v.isTextual()) {
                    String s = v.asText().trim().toLowerCase();
                    if (s.equals("true") || s.equals("yes") || s.equals("valid")) return true;
                    if (s.equals("false") || s.equals("no") || s.equals("invalid")) return false;
                }
                if (v.isInt() || v.isLong()) return v.asInt() != 0;
            }
        }
        return false;
    }

    @SafeVarargs
    private static String firstText(JsonNode root, Function<JsonNode, JsonNode>... candidates) {
        for (var f : candidates) {
            JsonNode v = f.apply(root);
            if (v != null && !v.isMissingNode() && !v.isNull()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) return s;
            }
        }
        return null;
    }
}
