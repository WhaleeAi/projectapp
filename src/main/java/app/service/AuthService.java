package app.service;

import app.DB;
import app.model.User;

import java.sql.*;

public class AuthService {

    public static class AuthResult {
        public final String status;
        public final String message;
        public final User user;

        public AuthResult(String status, String message, User user) {
            this.status = status;
            this.message = message;
            this.user = user;
        }
    }

    public AuthResult login(String login, String password) {
        String lg = login == null ? "" : login.trim();
        String pw = password == null ? "" : password.trim();
        try (Connection c = DB.get()) {
            if (DB.useStoredProcs()) {
                try (CallableStatement cs = c.prepareCall("{CALL sp_auth_login(?, ?, ?, ?, ?, ?, ?)}")) {
                    cs.setString(1, lg);
                    cs.setString(2, pw);
                    cs.registerOutParameter(3, Types.VARCHAR);
                    cs.registerOutParameter(4, Types.VARCHAR);
                    cs.registerOutParameter(5, Types.INTEGER);
                    cs.registerOutParameter(6, Types.VARCHAR);
                    cs.registerOutParameter(7, Types.TINYINT);
                    cs.execute();
                    String status = cs.getString(3);
                    String message = cs.getString(4);
                    int userId = cs.getInt(5);
                    String role = cs.getString(6);
                    boolean needChange = cs.getInt(7) == 1;
                    User u = (userId > 0) ? fetchUserBasic(c, userId, lg, role, needChange) : null;
                    return new AuthResult(status, message, u);
                }
            } else {
                // fallback (для отладки)
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT user_id, first_name, last_name, auth_role, password_changed FROM user WHERE login=? LIMIT 1")) {
                    ps.setString(1, lg);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return new AuthResult("INVALID_CREDENTIALS", "Неверный логин или пароль", null);
                        int id = rs.getInt("user_id");
                        String role = rs.getString("auth_role");
                        boolean changed = rs.getInt("password_changed") == 1;
                        User u = fetchUserBasic(c, id, lg, role, !changed);
                        return new AuthResult("SUCCESS", "Успешный вход (упрощенная проверка)", u);
                    }
                }
            }
        } catch (SQLException e) {
            return new AuthResult("DB_ERROR", e.getMessage(), null);
        }
    }

    private User fetchUserBasic(Connection c, int userId, String login, String role, boolean changeRequired) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT first_name, last_name FROM user WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                String fn = null, ln = null;
                if (rs.next()) {
                    fn = rs.getString("first_name");
                    ln = rs.getString("last_name");
                }
                return new User(userId, login, fn, ln, role, changeRequired);
            }
        }
    }

    public String changePassword(int userId, String currentPassword, String newPassword, String confirm) {
        try (Connection c = DB.get()) {
            if (DB.useStoredProcs()) {
                try (CallableStatement cs = c.prepareCall("{CALL sp_change_password(?, ?, ?, ?, ?, ?)}")) {
                    cs.setInt(1, userId);
                    cs.setString(2, currentPassword);
                    cs.setString(3, newPassword);
                    cs.setString(4, confirm);
                    cs.registerOutParameter(5, Types.VARCHAR); // o_status
                    cs.registerOutParameter(6, Types.VARCHAR); // o_message
                    cs.execute();
                    return cs.getString(5) + ": " + cs.getString(6);
                }
            } else {
                return "ERROR: Stored procedures disabled; change password requires DB-side logic.";
            }
        } catch (SQLException e) {
            return "DB_ERROR: " + e.getMessage();
        }
    }

    public String adminUnlockUser(int userId) {
        try (Connection c = DB.get()) {
            if (DB.useStoredProcs()) {
                try (CallableStatement cs = c.prepareCall("{CALL sp_admin_unlock_user(?, ?, ?)}")) {
                    cs.setInt(1, userId);
                    cs.registerOutParameter(2, Types.VARCHAR); // o_status
                    cs.registerOutParameter(3, Types.VARCHAR); // o_message
                    cs.execute();
                    return cs.getString(2) + ": " + cs.getString(3);
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE user SET is_active=1, login_attempts=0, locked_at=NULL, locked_reason=NULL WHERE user_id=?")) {
                    ps.setInt(1, userId);
                    int updated = ps.executeUpdate();
                    return updated > 0 ? "SUCCESS: Блокировка снята" : "NOT_FOUND: Пользователь не найден";
                }
            }
        } catch (SQLException e) {
            return "DB_ERROR: " + e.getMessage();
        }
    }
}
