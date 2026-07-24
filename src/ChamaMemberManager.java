// ChamaMemberManager.java
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ChamaMemberManager {

    public static void addMember(int chamaId, int userId, String role, Connection conn) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO chama_members (chama_id, user_id, role, status, join_date) " +
                        "VALUES (?, ?, ?, 'APPROVED', ?)");
        pst.setInt(1, chamaId);
        pst.setInt(2, userId);
        pst.setString(3, role);
        pst.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
        pst.executeUpdate();
        System.out.println("✅ Member added: User " + userId + " as " + role);
    }

    public static void addMember(int chamaId, int userId, String role) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            addMember(chamaId, userId, role, conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Map<String, Object>> getMembers(int chamaId) {
        List<Map<String, Object>> members = new ArrayList<>();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT cm.user_id, cm.role, cm.status, cm.join_date, cm.approved_at, " +
                             "COALESCE(u.fullname, u.username) as display_name " +
                             "FROM chama_members cm " +
                             "LEFT JOIN users u ON cm.user_id = u.id " +
                             "WHERE cm.chama_id = ? AND cm.status = 'APPROVED' " +
                             "ORDER BY cm.approved_at DESC")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Map<String, Object> member = new HashMap<>();
                member.put("user_id", rs.getInt("user_id"));
                member.put("role", rs.getString("role"));
                member.put("status", rs.getString("status"));
                member.put("join_date", rs.getDate("join_date"));
                member.put("approved_at", rs.getTimestamp("approved_at"));
                member.put("display_name", rs.getString("display_name"));
                members.add(member);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public static boolean removeMember(int chamaId, int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "DELETE FROM chama_members WHERE chama_id = ? AND user_id = ?")) {
            pst.setInt(1, chamaId);
            pst.setInt(2, userId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void updateStatus(int chamaId, int userId, String status) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE chama_members SET status = ?, approved_at = NOW() " +
                             "WHERE chama_id = ? AND user_id = ?")) {
            pst.setString(1, status);
            pst.setInt(2, chamaId);
            pst.setInt(3, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}