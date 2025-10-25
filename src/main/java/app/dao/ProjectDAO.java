package app.dao;

import app.model.ClientItem;
import app.model.ProjectRecord;
import app.DB;
import app.model.TariffItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {

    public List<ProjectRecord> listProjects() throws SQLException {
        String sql = "SELECT p.project_id, p.name, p.description, p.status, " +
                "p.planned_start_date, p.planned_end_date, p.planned_budget, " +
                "p.success_criteria, p.tariff_id, p.computer_count, " +
                "t.name AS tariff_name, p.client_id, c.name AS client_name " +
                "FROM project p " +
                "LEFT JOIN tariff t ON t.tariff_id = p.tariff_id " +
                "LEFT JOIN client c ON c.client_id = p.client_id";
        try (Connection c = DB.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {

            List<ProjectRecord> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new ProjectRecord(
                        rs.getInt("project_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getDate("planned_start_date") != null ? rs.getDate("planned_start_date").toString() : null,
                        rs.getDate("planned_end_date") != null ? rs.getDate("planned_end_date").toString() : null,
                        rs.getDouble("planned_budget"),
                        rs.getString("success_criteria"),
                        (Integer) rs.getObject("tariff_id"),
                        (Integer) rs.getObject("computer_count"),
                        rs.getString("tariff_name"),
                        (Integer) rs.getObject("client_id"),
                        rs.getString("client_name")
                ));
            }
            return list;
        }
    }

    public boolean insertProject(String name, String description, String status,
                                 String startDate, String endDate, double budget,
                                 String successCriteria, Integer tariffId, Integer computerCount,
                                 Integer clientId) throws SQLException {
        String sql = "INSERT INTO project " +
                "(name, description, status, planned_start_date, planned_end_date, planned_budget, " +
                " success_criteria, tariff_id, computer_count, client_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, status);
            ps.setString(4, startDate);
            ps.setString(5, endDate);
            ps.setDouble(6, budget);
            ps.setString(7, successCriteria);                     // NOT NULL в БД
            if (tariffId == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, tariffId);
            if (computerCount == null) ps.setNull(9, Types.INTEGER);
            else ps.setInt(9, computerCount);
            if (clientId == null) ps.setNull(10, Types.INTEGER);
            else ps.setInt(10, clientId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<TariffItem> listTariffs() throws SQLException {
        String sql = "SELECT tariff_id, name FROM tariff ORDER BY name";
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<TariffItem> items = new ArrayList<>();
            while (rs.next()) {
                items.add(new TariffItem(rs.getInt("tariff_id"), rs.getString("name")));
            }
            return items;
        }
    }

    public List<ClientItem> listClients() throws SQLException {
        String sql = "SELECT client_id, name FROM client ORDER BY name";
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<ClientItem> items = new ArrayList<>();
            while (rs.next()) {
                items.add(new ClientItem(rs.getInt("client_id"), rs.getString("name")));
            }
            return items;
        }
    }

    // Необязательная «обёртка» для обратной совместимости старых вызовов:
    public boolean insertProject(String n, String d, String s, String st, String en, double b) throws SQLException {
        return insertProject(n, d, s, st, en, b, "Критерий не указан", null, null, null);
    }
}
