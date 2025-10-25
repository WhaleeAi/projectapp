package app.model;

public class User {
    private int userId;
    private String login;
    private String firstName;
    private String lastName;
    private String authRole;
    private boolean passwordChangeRequired;
    private String role;

    public User(int userId, String login, String firstName, String lastName,
                String authRole, boolean passwordChangeRequired, String role) {
        this.userId = userId;
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.authRole = authRole;
        this.passwordChangeRequired = passwordChangeRequired;
        this.role = role;
    }

    public int getUserId() { return userId; }
    public String getLogin() { return login; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getAuthRole() { return authRole; }
    public String getRole() { return role; }

    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }

    public String getDisplayName() {
        String fn = (firstName == null ? "" : firstName);
        String ln = (lastName == null ? "" : lastName);
        return (fn + " " + ln).trim();
    }
}
