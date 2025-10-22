package app;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DB {
    private static final Properties props = new Properties();
    private static boolean useStoredProcs = true;
    private static volatile boolean configLoaded = false;
    private static volatile String configError = null;

    static {
        try (InputStream in = DB.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                configError = "config.properties not found in resources (ожидается src/main/resources/config.properties)";
            } else {
                props.load(in);
                useStoredProcs = Boolean.parseBoolean(props.getProperty("db.use_stored_procs", "true"));
                configLoaded = true;
            }
        } catch (IOException e) {
            configError = "Failed to load DB config: " + e.getMessage();
        }
    }

    public static Connection get() throws SQLException {
        if (!configLoaded) {
            throw new SQLException(configError != null ? configError : "DB config not loaded");
        }
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, pass);
    }

    public static boolean useStoredProcs() {
        return useStoredProcs;
    }
}

