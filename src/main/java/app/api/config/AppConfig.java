package app.config;

public class AppConfig {
    /** Базовый URL внешнего сервиса валидации.
     *  По умолчанию — AbstractAPI Email Validation (можно заменить на любой). */
    public static String EMAIL_API_URL_TEMPLATE =
            System.getenv().getOrDefault("EMAIL_API_URL_TEMPLATE",
                    "https://emailvalidation.abstractapi.com/v1/?api_key=%s&email=%s");

    /** API-ключ внешнего сервиса. Храним вне кода: ENV EMAIL_API_KEY. */
    public static String EMAIL_API_KEY =
            System.getenv().getOrDefault("EMAIL_API_KEY", "b182a374445e4143ac01f6d7b391f005&email=usernameeasy@mail.ru");

    /** Путь к файлу DOCX с тест-кейсами. */
    public static String TESTCASE_DOCX_PATH =
            System.getenv().getOrDefault("TESTCASE_DOCX_PATH", "ТестКейс.docx");
}
