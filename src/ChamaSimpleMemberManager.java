// ChamaSimpleMemberManager.java
import java.sql.*;
import java.util.*;

public class ChamaSimpleMemberManager {

    public static boolean addSimpleMember(int chamaId, String fullname, String phoneNumber, String mpesaNumber) {
        String memberCode = generateMemberCode(chamaId);

        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO chama_simple_members (chama_id, fullname, phone_number, mpesa_number, " +
                             "member_code, join_date) VALUES (?, ?, ?, ?, ?, ?)")) {
            pst.setInt(1, chamaId);
            pst.setString(2, fullname);
            pst.setString(3, phoneNumber);
            pst.setString(4, mpesaNumber);
            pst.setString(5, memberCode);
            pst.setDate(6, java.sql.Date.valueOf(java.time.LocalDate.now()));
            pst.executeUpdate();
            System.out.println("✅ Member added with code: " + memberCode);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<SimpleMember> getSimpleMembers(int chamaId) {
        List<SimpleMember> members = new ArrayList<>();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, chama_id, fullname, phone_number, mpesa_number, member_code, " +
                             "join_date, is_registered, registered_user_id, total_contributions, status " +
                             "FROM chama_simple_members WHERE chama_id = ? ORDER BY join_date DESC")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                SimpleMember member = new SimpleMember();
                member.setId(rs.getInt("id"));
                member.setChamaId(rs.getInt("chama_id"));
                member.setFullname(rs.getString("fullname"));
                member.setPhoneNumber(rs.getString("phone_number"));
                member.setMpesaNumber(rs.getString("mpesa_number"));
                member.setMemberCode(rs.getString("member_code"));
                member.setJoinDate(rs.getDate("join_date"));  // ✅ Make sure this is set
                member.setRegistered(rs.getBoolean("is_registered"));
                member.setRegisteredUserId(rs.getInt("registered_user_id"));
                if (rs.wasNull()) {
                    member.setRegisteredUserId(null);
                }
                member.setTotalContributions(rs.getDouble("total_contributions"));
                member.setStatus(rs.getString("status"));
                members.add(member);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public static boolean recordSimpleContribution(int chamaId, int memberId, double amount,
                                                   String paymentMethod, String transactionId, int recordedBy) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            conn.setAutoCommit(false);

            // Insert contribution
            PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO chama_simple_contributions (chama_id, member_id, amount, " +
                            "payment_method, transaction_id, recorded_by) VALUES (?, ?, ?, ?, ?, ?)");
            pst.setInt(1, chamaId);
            pst.setInt(2, memberId);
            pst.setDouble(3, amount);
            pst.setString(4, paymentMethod);
            pst.setString(5, transactionId);
            pst.setInt(6, recordedBy);
            pst.executeUpdate();

            // Update member total
            PreparedStatement updatePst = conn.prepareStatement(
                    "UPDATE chama_simple_members SET total_contributions = total_contributions + ? WHERE id = ?");
            updatePst.setDouble(1, amount);
            updatePst.setInt(2, memberId);
            updatePst.executeUpdate();

            conn.commit();
            System.out.println("✅ Contribution recorded: Ksh " + amount + " for member " + memberId);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean linkMemberToUser(int memberId, int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE chama_simple_members SET is_registered = TRUE, registered_user_id = ? WHERE id = ?")) {
            pst.setInt(1, userId);
            pst.setInt(2, memberId);
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // Add this public method to ChamaSimpleMemberManager.java
    public static String generateMemberCodePublic(int chamaId) {
        return generateMemberCode(chamaId);
    }

    // Make the existing generateMemberCode method public
    public static String generateMemberCode(int chamaId) {
        Random random = new Random();
        int number = random.nextInt(9000) + 1000;
        return "CHAMA-" + chamaId + "-" + number;
    }
}