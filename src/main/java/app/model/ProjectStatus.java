package app.model;

public enum ProjectStatus {
    PLANNING("планирование"),
    ACTIVE("активен"),
    UNDER_REVIEW("на проверке"),
    COMPLETED("завершен"),
    CANCELED("отменен");

    private final String dbValue;
    ProjectStatus(String dbValue) { this.dbValue = dbValue; }
    public String getDbValue() { return dbValue; }

    @Override public String toString() { return dbValue; } // чтобы в ComboBox показывалось по-русски

    public static ProjectStatus fromDb(String v) {
        if (v == null) return null;
        for (ProjectStatus s : values()) if (s.dbValue.equalsIgnoreCase(v)) return s;
        throw new IllegalArgumentException("Unknown status: " + v);
    }
}
