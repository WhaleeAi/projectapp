package app.dao;

import app.DB;
import app.model.UserRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public List<UserRecord> listUsers(String queryLike) throws SQLException {
        String sql =
                "SELECT user_id, login, first_name, last_name, auth_role, " +
                        "       IFNULL(is_active,1) AS is_active, IFNULL(login_attempts,0) AS login_attempts, locked_reason " +
                        "FROM user " +
                        (queryLike != null && !queryLike.isBlank() ? "WHERE login LIKE ? OR first_name LIKE ? OR last_name LIKE ? " : "") +
                        "ORDER BY user_id DESC";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (queryLike != null && !queryLike.isBlank()) {
                String like = "%" + queryLike.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }

            List<UserRecord> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new UserRecord(
                            rs.getInt("user_id"),
                            rs.getString("login"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("auth_role"),
                            rs.getInt("is_active") == 1,
                            rs.getInt("login_attempts"),
                            rs.getString("locked_reason")
                    ));
                }
            }
            return out;
        }
    }

    public boolean loginExists(String login) throws SQLException {
        String sql = "SELECT 1 FROM user WHERE login=? LIMIT 1";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Возвращает новый user_id */
    public int insertUser(String login, String firstName, String lastName,
                          String role, String companyRole, String password) throws SQLException {
        String sql = "INSERT INTO user (login, first_name, last_name, auth_role, role, password, password_changed) " +
                "VALUES (?, ?, ?, ?, ?, ?, 1)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, login);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, role);
            ps.setString(5, companyRole);
            ps.setString(6, password);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }


    public boolean updateUser(int userId, String firstName, String lastName, String roleUi, boolean active) throws SQLException {
        String roleDb = toDbRole(roleUi); // ← даёт "Администратор"/"Пользователь"
        String sql = "UPDATE user SET first_name=?, last_name=?, auth_role=?, is_active=? WHERE user_id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, firstName == null ? "" : firstName);
            ps.setString(2, lastName  == null ? "" : lastName);
            ps.setString(3, roleDb);
            ps.setInt(4, active ? 1 : 0);
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean resetLock(int userId) throws SQLException {
        String sql = "UPDATE user SET is_active=1, login_attempts=0, locked_at=NULL, locked_reason=NULL WHERE user_id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // Маппинг ролей UI <-> БД
    private String toDbRole(String uiRole) {
        if (uiRole == null) return "Пользователь";
        String r = uiRole.trim().toLowerCase();

        // принимаем любые варианты из UI
        if (r.contains("admin") || r.contains("админ") || r.contains("администратор")) {
            return "Администратор";   // ← РУССКИЙ текст для ENUM(auth_role)
        }
        if (r.contains("user") || r.contains("пользователь")) {
            return "Пользователь";
        }

        // если вдруг из БД уже пришло корректное русское значение — оставим
        if (uiRole.equals("Администратор") || uiRole.equals("Пользователь")) {
            return uiRole;
        }

        // дефолт
        return "Пользователь";
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
