package app.model;

public class ProjectRecord {
    private int projectId;
    private String name;
    private String description;
    private String status;
    private String startDate;
    private String endDate;
    private double budget;
    private Integer tariffId;         // NULLable в БД
    private Integer computerCount;    // NULLable в БД
    private String successCriteria;   // NOT NULL в БД
    private String tariffName;
    private Integer clientId;
    private String clientName;

    public ProjectRecord(int projectId, String name, String description, String status,
                         String startDate, String endDate, double budget,
                         String successCriteria, Integer tariffId, Integer computerCount,
                         String tariffName, Integer clientId, String clientName) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budget = budget;

        this.successCriteria = successCriteria;
        this.tariffId = tariffId;
        this.computerCount = computerCount;
        this.tariffName = tariffName;
        this.clientId = clientId;
        this.clientName = clientName;
    }

    public int getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public double getBudget() { return budget; }
    public Integer getTariffId() { return tariffId; }
    public Integer getComputerCount() { return computerCount; }
    public String getSuccessCriteria() { return successCriteria; }
    public String getTariffName() { return tariffName; }
    public Integer getClientId() { return clientId; }
    public String getClientName() { return clientName; }
}
