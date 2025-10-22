package app.model;

public class UserRecord {
    private int userId;
    private String login;
    private String firstName;
    private String lastName;
    private String role;          // хранит значение как в БД (например, ADMIN/USER)
    private boolean active;
    private int loginAttempts;
    private String lockedReason;

    public UserRecord(int userId, String login, String firstName, String lastName, String role,
                      boolean active, int loginAttempts, String lockedReason) {
        this.userId = userId;
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.active = active;
        this.loginAttempts = loginAttempts;
        this.lockedReason = lockedReason;
    }

    public int getUserId() { return userId; }
    public String getLogin() { return login; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public int getLoginAttempts() { return loginAttempts; }
    public String getLockedReason() { return lockedReason; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setRole(String role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
}
