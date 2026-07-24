import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class ChamaGroup {
    private int id;
    private String groupName;
    private String groupCode;
    private int createdBy;
    private int leaderId;
    private double totalGoal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String contributionFrequency;
    private String status;
    private Timestamp createdAt;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }

    public double getTotalGoal() {
        return totalGoal;
    }

    public void setTotalGoal(double totalGoal) {
        this.totalGoal = totalGoal;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getContributionFrequency() {
        return contributionFrequency;
    }

    public void setContributionFrequency(String contributionFrequency) {
        this.contributionFrequency = contributionFrequency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Database Methods
    public static boolean createGroup(ChamaGroup group, List<Integer> memberIds) {
        Connection conn = null;
        try {
            conn = SecureDatabaseConnection.connect();
            conn.setAutoCommit(false);

            System.out.println("=== CREATING NEW CHAMA ===");
            System.out.println("Group Name: " + group.getGroupName());
            System.out.println("Created By: " + group.getCreatedBy());
            System.out.println("Leader ID: " + group.getLeaderId());

            // Generate a unique group code
            String groupCode = generateUniqueGroupCode(conn);
            System.out.println("Generated group code: '" + groupCode + "'");

            // Insert group - make sure id is auto-generated
            PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO chama_groups (group_name, group_code, created_by, leader_id, total_goal, start_date, end_date, contribution_frequency, status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')",
                    Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, group.getGroupName());
            pst.setString(2, groupCode);
            pst.setInt(3, group.getCreatedBy());
            pst.setInt(4, group.getLeaderId());
            pst.setDouble(5, group.getTotalGoal());
            pst.setDate(6, java.sql.Date.valueOf(group.getStartDate()));
            pst.setDate(7, java.sql.Date.valueOf(group.getEndDate()));
            pst.setString(8, group.getContributionFrequency());

            int affected = pst.executeUpdate();
            System.out.println("INSERT affected: " + affected + " rows");

            if (affected == 0) {
                System.out.println("Failed to insert Chama");
                conn.rollback();
                return false;
            }

            ResultSet rs = pst.getGeneratedKeys();
            int chamaId = 0;
            if (rs.next()) {
                chamaId = rs.getInt(1);
                System.out.println("Generated Chama ID: " + chamaId);
            } else {
                System.out.println("Failed to get generated ID");
                conn.rollback();
                return false;
            }

            // CRITICAL: Add the creator as a member (LEADER)
            PreparedStatement addLeader = conn.prepareStatement(
                    "INSERT INTO chama_members (chama_id, user_id, role, status, join_date, requested_at) "
                            + "VALUES (?, ?, 'LEADER', 'APPROVED', CURDATE(), NOW())");
            addLeader.setInt(1, chamaId);
            addLeader.setInt(2, group.getCreatedBy());
            addLeader.executeUpdate();
            System.out.println("Added creator as LEADER to chama_members");

            // Add other members
            if (memberIds != null && !memberIds.isEmpty()) {
                PreparedStatement addMember = conn.prepareStatement(
                        "INSERT INTO chama_members (chama_id, user_id, role, status, join_date, requested_at) "
                                + "VALUES (?, ?, 'MEMBER', 'APPROVED', CURDATE(), NOW())");

                for (int userId : memberIds) {
                    // Skip if it's the same as creator (already added)
                    if (userId != group.getCreatedBy()) {
                        addMember.setInt(1, chamaId);
                        addMember.setInt(2, userId);
                        addMember.addBatch();
                        System.out.println("Queued member: " + userId);
                    }
                }
                int[] results = addMember.executeBatch();
                System.out.println("Added " + results.length + " members");
            }

            conn.commit();
            System.out.println("=== CHAMA CREATED SUCCESSFULLY ===");
            System.out.println("Chama ID: " + chamaId);
            System.out.println("Chama Code: " + groupCode);

            return true;

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Transaction rolled back");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String generateUniqueGroupCode(Connection conn) throws SQLException {
        String code;
        boolean exists;
        do {
            // Generate 8-character alphanumeric code
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                int index = (int) (Math.random() * chars.length());
                sb.append(chars.charAt(index));
            }
            code = sb.toString();

            // Check if code already exists
            PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM chama_groups WHERE group_code = ?");
            pst.setString(1, code);
            ResultSet rs = pst.executeQuery();
            exists = rs.next() && rs.getInt(1) > 0;

        } while (exists);

        return code;
    }
    public static boolean createGroupWithSimpleMembers(ChamaGroup group, List<SimpleMember> members) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            conn.setAutoCommit(false);

            // Create the Chama and get the ID
            int chamaId = createGroupAndReturnId(group, conn);
            if (chamaId == -1) {
                conn.rollback();
                return false;
            }

            // Add the leader as a member (using the existing ChamaMemberManager)
            ChamaMemberManager.addMember(chamaId, Session.getUserId(), "LEADER", conn);

            // Add all simple members using ChamaSimpleMemberManager
            for (SimpleMember member : members) {
                // Generate member code using public method
                String memberCode = ChamaSimpleMemberManager.generateMemberCodePublic(chamaId);

                PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO chama_simple_members (chama_id, fullname, phone_number, mpesa_number, " +
                                "member_code, join_date) VALUES (?, ?, ?, ?, ?, ?)");
                pst.setInt(1, chamaId);
                pst.setString(2, member.getFullname());
                pst.setString(3, member.getPhoneNumber());
                pst.setString(4, member.getMpesaNumber());
                pst.setString(5, memberCode);
                pst.setDate(6, java.sql.Date.valueOf(LocalDate.now()));
                pst.executeUpdate();
                member.setMemberCode(memberCode);
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper method to create Chama and return ID
    private static int createGroupAndReturnId(ChamaGroup group, Connection conn) throws SQLException {
        String code = generateGroupCode();
        PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO chama_groups (group_name, group_code, created_by, leader_id, " +
                        "total_goal, start_date, end_date, contribution_frequency, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')",
                Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, group.getGroupName());
        pst.setString(2, code);
        pst.setInt(3, group.getCreatedBy());
        pst.setInt(4, group.getLeaderId());
        pst.setDouble(5, group.getTotalGoal());
        pst.setDate(6, Date.valueOf(group.getStartDate()));
        pst.setDate(7, Date.valueOf(group.getEndDate()));
        pst.setString(8, group.getContributionFrequency());
        pst.executeUpdate();

        ResultSet keys = pst.getGeneratedKeys();
        if (keys.next()) {
            return keys.getInt(1);
        }
        return -1;
    }
    public static boolean addSimpleMembersToChama(int chamaId, List<SimpleMember> members) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            conn.setAutoCommit(false);

            PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO chama_simple_members (chama_id, fullname, phone_number, mpesa_number, " +
                            "member_code, join_date) VALUES (?, ?, ?, ?, ?, ?)");

            int added = 0;
            for (SimpleMember member : members) {
                String memberCode = ChamaSimpleMemberManager.generateMemberCode(chamaId);

                pst.setInt(1, chamaId);
                pst.setString(2, member.getFullname());
                pst.setString(3, member.getPhoneNumber());
                pst.setString(4, member.getMpesaNumber());
                pst.setString(5, memberCode);
                pst.setDate(6, java.sql.Date.valueOf(LocalDate.now()));
                pst.addBatch();
                member.setMemberCode(memberCode);
                added++;
            }

            int[] results = pst.executeBatch();
            conn.commit();

            System.out.println("✅ Added " + added + " simple members to Chama " + chamaId);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Failed to add simple members: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }



    private static void notifyMembersAdded(int chamaId, String groupName, List<Integer> memberIds) {
        if (memberIds == null) return;

        try (Connection conn = SecureDatabaseConnection.connect()) {
            for (int userId : memberIds) {
                try {
                    NotificationService.create(userId,
                            "🎉 You have been added to Chama '" + groupName + "'!\n\n" +
                                    "Start saving together and reach your group goals!",
                            NotificationService.SUCCESS);
                } catch (Exception e) {
                    System.out.println("Could not send notification to user " + userId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String generateGroupCode(String groupName) {
        // Generate a readable code based on group name
        String prefix = groupName.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (prefix.length() > 4) {
            prefix = prefix.substring(0, 4);
        }
        if (prefix.length() < 2) {
            prefix = "CH";
        }
        return prefix + System.currentTimeMillis() % 10000;
    }

    public static List<Map<String, Object>> getUserChamas(int userId) {
        List<Map<String, Object>> chamas = new ArrayList<>();
        System.out.println("=== getUserChamas called for userId: " + userId + " ===");

        try (Connection conn = SecureDatabaseConnection.connect()) {

            // First, check if user exists in chama_members at all
            PreparedStatement checkPst = conn.prepareStatement(
                    "SELECT COUNT(*) FROM chama_members WHERE user_id = ?");
            checkPst.setInt(1, userId);
            ResultSet checkRs = checkPst.executeQuery();
            if (checkRs.next()) {
                System.out.println("User has " + checkRs.getInt(1) + " records in chama_members");
            }

            // Now get the actual chamas with their details
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT g.id, g.group_name, g.group_code, g.total_goal, g.status as group_status, " +
                            "cm.role, cm.status as member_status, cm.join_date " +
                            "FROM chama_members cm " +
                            "INNER JOIN chama_groups g ON cm.chama_id = g.id " +
                            "WHERE cm.user_id = ? " +
                            "ORDER BY cm.join_date DESC");
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Map<String, Object> chama = new HashMap<>();
                chama.put("id", rs.getInt("id"));
                chama.put("group_name", rs.getString("group_name"));
                chama.put("group_code", rs.getString("group_code"));
                chama.put("total_goal", rs.getDouble("total_goal"));
                chama.put("role", rs.getString("role"));
                chama.put("status", rs.getString("member_status"));
                chamas.add(chama);
                System.out.println("Found Chama: " + rs.getString("group_name") +
                        " (Role: " + rs.getString("role") +
                        ", Status: " + rs.getString("member_status") + ")");
            }

            System.out.println("Total Chamas found: " + chamas.size());

            // If no chamas found, show all chamas in database for debugging
            if (chamas.isEmpty()) {
                System.out.println("\n--- Debug: All Chamas in database ---");
                Statement stmt = conn.createStatement();
                ResultSet allRs = stmt.executeQuery("SELECT id, group_name, status FROM chama_groups");
                while (allRs.next()) {
                    System.out.println("Chama ID: " + allRs.getInt("id") +
                            ", Name: " + allRs.getString("group_name") +
                            ", Status: " + allRs.getString("status"));
                }

                System.out.println("\n--- Debug: All memberships ---");
                ResultSet membersRs = stmt.executeQuery(
                        "SELECT cm.user_id, cm.chama_id, cm.role, cm.status, g.group_name " +
                                "FROM chama_members cm JOIN chama_groups g ON cm.chama_id = g.id");
                while (membersRs.next()) {
                    System.out.println("User " + membersRs.getInt("user_id") +
                            " -> Chama: " + membersRs.getString("group_name") +
                            " (Role: " + membersRs.getString("role") +
                            ", Status: " + membersRs.getString("status") + ")");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting user chamas: " + e.getMessage());
            e.printStackTrace();
        }
        return chamas;
    }

    public static List<Map<String, Object>> getPendingRequests(int chamaId) {
        List<Map<String, Object>> requests = new ArrayList<>();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT cm.user_id, COALESCE(u.fullname, u.username) as fullname, cm.requested_at " +
                             "FROM chama_members cm JOIN users u ON cm.user_id = u.id " +
                             "WHERE cm.chama_id = ? AND cm.status = 'PENDING'")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Map<String, Object> req = new HashMap<>();
                req.put("user_id", rs.getInt("user_id"));
                req.put("fullname", rs.getString("fullname"));
                req.put("requested_at", rs.getTimestamp("requested_at"));
                requests.add(req);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public static boolean respondToJoinRequest(int chamaId, int userId, boolean approve, int adminId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE chama_members SET status = ?, approved_by = ?, approved_at = NOW() " +
                             "WHERE chama_id = ? AND user_id = ?")) {
            pst.setString(1, approve ? "APPROVED" : "REJECTED");
            pst.setInt(2, adminId);
            pst.setInt(3, chamaId);
            pst.setInt(4, userId);
            int rows = pst.executeUpdate();

            if (approve && rows > 0) {
                // Get user details for notification
                String userName = getUserName(userId);
                NotificationService.create(userId,
                        "✅ Your request to join the Chama has been APPROVED!\n\n" +
                                "You can now view Chama details and make contributions.",
                        NotificationService.SUCCESS);

                // Also notify the admin that approval was successful
                NotificationService.create(adminId,
                        "✅ You approved " + userName + " to join the Chama.",
                        NotificationService.INFO);
            }
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static String getUserName(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT COALESCE(fullname, username) as name FROM users WHERE id = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "User " + userId;
    }

    public static boolean requestToJoinChama(String groupCode, int userId, String userFullname) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            // Debug: Print what we're looking for
            System.out.println("Looking for Chama with code: '" + groupCode + "'");

            // Find the Chama - case insensitive, trim whitespace
            PreparedStatement findPst = conn.prepareStatement(
                    "SELECT id, group_name, status, leader_id FROM chama_groups WHERE UPPER(TRIM(group_code)) = UPPER(TRIM(?))");
            findPst.setString(1, groupCode.trim());
            ResultSet rs = findPst.executeQuery();

            if (!rs.next()) {
                System.out.println("No Chama found with code: " + groupCode);
                return false;
            }

            int chamaId = rs.getInt("id");
            String groupName = rs.getString("group_name");
            String status = rs.getString("status");

            System.out.println("Found Chama: " + groupName + " (ID: " + chamaId + ", Status: " + status + ")");

            // Check if Chama is active
            if (!"ACTIVE".equals(status)) {
                System.out.println("Chama is not active. Status: " + status);
                return false;
            }

            // Ensure the user has a full name (if not, use username)
            PreparedStatement updateUserPst = conn.prepareStatement(
                    "UPDATE users SET fullname = COALESCE(fullname, username) WHERE id = ?");
            updateUserPst.setInt(1, userId);
            updateUserPst.executeUpdate();

            // Check if already a member (any status)
            PreparedStatement checkPst = conn.prepareStatement(
                    "SELECT status, role FROM chama_members WHERE chama_id = ? AND user_id = ?");
            checkPst.setInt(1, chamaId);
            checkPst.setInt(2, userId);
            ResultSet checkRs = checkPst.executeQuery();

            if (checkRs.next()) {
                String memberStatus = checkRs.getString("status");
                String role = checkRs.getString("role");
                System.out.println("User is already a member. Status: " + memberStatus + ", Role: " + role);

                if ("PENDING".equals(memberStatus)) {
                    System.out.println("User already has a pending request");
                } else if ("APPROVED".equals(memberStatus) || "LEADER".equals(role)) {
                    System.out.println("User is already an approved member");
                }
                return false;
            }

            // Insert join request
            PreparedStatement insertPst = conn.prepareStatement(
                    "INSERT INTO chama_members (chama_id, user_id, role, status, requested_at) "
                            + "VALUES (?, ?, 'MEMBER', 'PENDING', NOW())");
            insertPst.setInt(1, chamaId);
            insertPst.setInt(2, userId);

            int result = insertPst.executeUpdate();
            System.out.println("Join request inserted. Result: " + result);

            // Notify the Chama leader
            notifyChamaLeader(chamaId, userId, groupName);

            return result > 0;

        } catch (SQLException e) {
            System.err.println("SQL Error in requestToJoinChama: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void notifyChamaLeader(int chamaId, int userId, String groupName) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT leader_id FROM chama_groups WHERE id = ?")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int leaderId = rs.getInt("leader_id");

                // Get user's name
                String userName = getUserName(userId);

                NotificationService.create(leaderId,
                        "📢 New join request for Chama '" + groupName + "' from " + userName + ".\n\n" +
                                "Go to Chama Management → Pending Requests to approve or reject.",
                        NotificationService.INFO);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void debugListAllChamas() {
        try (Connection conn = SecureDatabaseConnection.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, group_name, group_code, status FROM chama_groups")) {
            System.out.println("\n=== ALL CHAMAS IN DATABASE ===");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                        ", Name: " + rs.getString("group_name") +
                        ", Code: '" + rs.getString("group_code") + "'" +
                        ", Status: " + rs.getString("status"));
            }
            System.out.println("===============================\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static boolean recordContribution(int chamaId, int userId, double amount, String paymentMethod, int recordedBy) {
        Connection conn = null;
        try {
            conn = SecureDatabaseConnection.connect();

            // First, check if the user is a member of this Chama
            PreparedStatement checkPst = conn.prepareStatement(
                    "SELECT status FROM chama_members WHERE chama_id = ? AND user_id = ?");
            checkPst.setInt(1, chamaId);
            checkPst.setInt(2, userId);
            ResultSet rs = checkPst.executeQuery();

            if (!rs.next()) {
                System.out.println("User " + userId + " is not a member of Chama " + chamaId);
                return false;
            }

            String memberStatus = rs.getString("status");
            if (!"APPROVED".equals(memberStatus)) {
                System.out.println("User " + userId + " is not an approved member. Status: " + memberStatus);
                return false;
            }

            // Insert the contribution
            PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO chama_contributions (chama_id, user_id, amount, payment_method, recorded_by, contribution_date, status) "
                            + "VALUES (?, ?, ?, ?, ?, NOW(), 'CONFIRMED')");
            pst.setInt(1, chamaId);
            pst.setInt(2, userId);
            pst.setDouble(3, amount);
            pst.setString(4, paymentMethod);
            pst.setInt(5, recordedBy);

            int rows = pst.executeUpdate();
            System.out.println("Contribution recorded: " + rows + " row(s) affected");

            if (rows > 0) {
                // Send notification to the member
                try {
                    NotificationService.create(userId,
                            String.format("💰 Payment of Ksh %,.0f has been recorded for your Chama contribution.\nMethod: %s", amount, paymentMethod),
                            NotificationService.SUCCESS);
                } catch (Exception e) {
                    System.out.println("Could not send notification: " + e.getMessage());
                }
            }

            return rows > 0;

        } catch (SQLException e) {
            System.err.println("SQL Error recording contribution: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static void generateChamaReport(int chamaId, String filepath) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            // Get Chama details
            PreparedStatement chamaPst = conn.prepareStatement(
                    "SELECT g.*, u.username as leader_name " +
                            "FROM chama_groups g " +
                            "JOIN users u ON g.leader_id = u.id " +
                            "WHERE g.id = ?");
            chamaPst.setInt(1, chamaId);
            ResultSet chamaRs = chamaPst.executeQuery();

            if (!chamaRs.next()) {
                System.out.println("Chama not found");
                return;
            }

            String groupName = chamaRs.getString("group_name");
            String groupCode = chamaRs.getString("group_code");
            String leaderName = chamaRs.getString("leader_name");
            double totalGoal = chamaRs.getDouble("total_goal");
            String frequency = chamaRs.getString("contribution_frequency");
            Date startDate = chamaRs.getDate("start_date");
            Date endDate = chamaRs.getDate("end_date");
            Timestamp createdAt = chamaRs.getTimestamp("created_at");

            chamaRs.close();
            chamaPst.close();

            // ============================================================
            // GET REGISTERED MEMBERS (users with accounts)
            // ============================================================
            PreparedStatement membersPst = conn.prepareStatement(
                    "SELECT u.id, COALESCE(u.fullname, u.username) as member_name, u.email, " +
                            "cm.role, COALESCE(cm.join_date, DATE(cm.approved_at), CURDATE()) as join_date, cm.approved_at, " +
                            "COALESCE(SUM(c.amount), 0) as total_contributed, " +
                            "COUNT(c.id) as payment_count " +
                            "FROM chama_members cm " +
                            "JOIN users u ON cm.user_id = u.id " +
                            "LEFT JOIN chama_contributions c ON cm.chama_id = c.chama_id AND c.user_id = u.id " +
                            "WHERE cm.chama_id = ? AND cm.status = 'APPROVED' " +
                            "GROUP BY u.id, u.username, u.fullname, u.email, cm.role, cm.join_date, cm.approved_at " +
                            "ORDER BY total_contributed DESC",
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            membersPst.setInt(1, chamaId);
            ResultSet membersRs = membersPst.executeQuery();

            // ============================================================
            // GET SIMPLE MEMBERS (no account required)
            // ============================================================
            PreparedStatement simpleMembersPst = conn.prepareStatement(
                    "SELECT sm.id, sm.fullname, sm.phone_number, sm.join_date, " +
                            "COALESCE(SUM(sc.amount), 0) as total_contributed, " +
                            "COUNT(sc.id) as payment_count " +
                            "FROM chama_simple_members sm " +
                            "LEFT JOIN chama_simple_contributions sc ON sm.id = sc.member_id " +
                            "WHERE sm.chama_id = ? " +
                            "GROUP BY sm.id, sm.fullname, sm.phone_number, sm.join_date " +
                            "ORDER BY total_contributed DESC");
            simpleMembersPst.setInt(1, chamaId);
            ResultSet simpleMembersRs = simpleMembersPst.executeQuery();

            // ============================================================
            // GET ALL CONTRIBUTIONS (both types)
            // ============================================================
            // Registered members contributions
            PreparedStatement contribPst = conn.prepareStatement(
                    "SELECT c.amount, c.contribution_date, c.payment_method, " +
                            "COALESCE(u.fullname, u.username) as member_name " +
                            "FROM chama_contributions c " +
                            "JOIN users u ON c.user_id = u.id " +
                            "WHERE c.chama_id = ? " +
                            "ORDER BY c.contribution_date DESC LIMIT 50",
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            contribPst.setInt(1, chamaId);
            ResultSet contribRs = contribPst.executeQuery();

            // Simple members contributions
            PreparedStatement simpleContribPst = conn.prepareStatement(
                    "SELECT sc.amount, sc.contribution_date, sc.payment_method, " +
                            "sm.fullname as member_name " +
                            "FROM chama_simple_contributions sc " +
                            "JOIN chama_simple_members sm ON sc.member_id = sm.id " +
                            "WHERE sc.chama_id = ? " +
                            "ORDER BY sc.contribution_date DESC LIMIT 50",
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            simpleContribPst.setInt(1, chamaId);
            ResultSet simpleContribRs = simpleContribPst.executeQuery();

            // ============================================================
            // CALCULATE TOTALS
            // ============================================================
            double totalCollected = 0;
            int totalPayments = 0;
            List<Map<String, Object>> contributionsList = new ArrayList<>();

            // Add registered member contributions
            while (contribRs.next()) {
                Map<String, Object> contrib = new HashMap<>();
                double amount = contribRs.getDouble("amount");
                totalCollected += amount;
                totalPayments++;
                contrib.put("contribution_date", contribRs.getString("contribution_date"));
                contrib.put("member_name", contribRs.getString("member_name"));
                contrib.put("amount", amount);
                contrib.put("payment_method", contribRs.getString("payment_method"));
                contrib.put("status", "CONFIRMED");
                contributionsList.add(contrib);
            }
            contribRs.close();
            contribPst.close();

            // Add simple member contributions
            while (simpleContribRs.next()) {
                Map<String, Object> contrib = new HashMap<>();
                double amount = simpleContribRs.getDouble("amount");
                totalCollected += amount;
                totalPayments++;
                contrib.put("contribution_date", simpleContribRs.getString("contribution_date"));
                contrib.put("member_name", simpleContribRs.getString("member_name") + " (No Account)");
                contrib.put("amount", amount);
                contrib.put("payment_method", simpleContribRs.getString("payment_method"));
                contrib.put("status", "CONFIRMED");
                contributionsList.add(contrib);
            }
            simpleContribRs.close();
            simpleContribPst.close();

            // ============================================================
            // COUNT TOTAL MEMBERS (both types)
            // ============================================================
            int memberCount = 0;
            // Count registered members
            PreparedStatement countPst = conn.prepareStatement(
                    "SELECT COUNT(*) FROM chama_members WHERE chama_id = ? AND status = 'APPROVED'");
            countPst.setInt(1, chamaId);
            ResultSet countRs = countPst.executeQuery();
            if (countRs.next()) {
                memberCount += countRs.getInt(1);
            }
            countRs.close();
            countPst.close();

            // Count simple members
            PreparedStatement simpleCountPst = conn.prepareStatement(
                    "SELECT COUNT(*) FROM chama_simple_members WHERE chama_id = ?");
            simpleCountPst.setInt(1, chamaId);
            ResultSet simpleCountRs = simpleCountPst.executeQuery();
            if (simpleCountRs.next()) {
                memberCount += simpleCountRs.getInt(1);
            }
            simpleCountRs.close();
            simpleCountPst.close();

            double progress = (totalCollected / totalGoal) * 100;
            double remaining = totalGoal - totalCollected;

            // ============================================================
            // CREATE PDF DOCUMENT
            // ============================================================
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filepath));
            document.open();

            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 22, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10);
            com.itextpdf.text.Font boldFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font greenFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL,
                    com.itextpdf.text.BaseColor.GREEN);
            com.itextpdf.text.Font orangeFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL,
                    new com.itextpdf.text.BaseColor(255, 140, 0));
            com.itextpdf.text.Font purpleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL,
                    new com.itextpdf.text.BaseColor(111, 66, 193));

            // Header
            Paragraph header = new Paragraph("SUPREME MONEY COACH", titleFont);
            header.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subtitle = new Paragraph("Chama Financial Report", headerFont);
            subtitle.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Generated on: " + new java.util.Date(), normalFont));
            document.add(new Paragraph("Report ID: CHM-" + chamaId + "-" + System.currentTimeMillis(), normalFont));
            document.add(new Paragraph(" "));

            // Chama Information
            document.add(new Paragraph("CHAMA INFORMATION", headerFont));
            document.add(new Paragraph("─────────────────────────────────────────────────", normalFont));
            document.add(new Paragraph("Chama Name: " + groupName, normalFont));
            document.add(new Paragraph("Chama Code: " + groupCode, normalFont));
            document.add(new Paragraph("Leader: " + leaderName, normalFont));
            document.add(new Paragraph("Created: " + createdAt, normalFont));
            document.add(new Paragraph("Period: " + startDate + " to " + endDate, normalFont));
            document.add(new Paragraph("Contribution Frequency: " + frequency, normalFont));
            document.add(new Paragraph("Member Count: " + memberCount + " (includes " +
                    (memberCount - 0) + " registered users and " +
                    getSimpleMemberCount(chamaId) + " non-registered members)", normalFont));
            document.add(new Paragraph(" "));

            // Financial Summary
            document.add(new Paragraph("FINANCIAL SUMMARY", headerFont));
            document.add(new Paragraph("─────────────────────────────────────────────────", normalFont));
            document.add(new Paragraph("Total Goal: Ksh " + String.format("%,.0f", totalGoal), normalFont));
            document.add(new Paragraph("Total Collected: Ksh " + String.format("%,.0f", totalCollected), greenFont));
            document.add(new Paragraph("Remaining: Ksh " + String.format("%,.0f", remaining), normalFont));
            document.add(new Paragraph("Progress: " + String.format("%.1f%%", progress), boldFont));
            document.add(new Paragraph("Total Payments: " + totalPayments, normalFont));
            document.add(new Paragraph(" "));

            // Progress Bar
            int barLength = 50;
            int filled = (int) ((progress / 100) * barLength);
            String progressBar = "[";
            for (int i = 0; i < barLength; i++) {
                progressBar += (i < filled) ? "█" : "░";
            }
            progressBar += "] " + String.format("%.1f%%", progress);
            document.add(new Paragraph(progressBar, boldFont));
            document.add(new Paragraph(" "));

            // ============================================================
            // MEMBERS TABLE - INCLUDING SIMPLE MEMBERS
            // ============================================================
            document.add(new Paragraph("MEMBERS & CONTRIBUTIONS", headerFont));
            document.add(new Paragraph("─────────────────────────────────────────────────", normalFont));

            PdfPTable memberTable = new PdfPTable(5);
            memberTable.setWidthPercentage(100);
            memberTable.setWidths(new float[]{30f, 15f, 15f, 15f, 25f});

            String[] headers = {"Member Name", "Role", "Join Date", "Payments", "Total Contributed"};
            for (String h : headers) {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new Paragraph(h, boldFont));
                cell.setBackgroundColor(new com.itextpdf.text.BaseColor(200, 200, 200));
                cell.setPadding(5);
                memberTable.addCell(cell);
            }

            // Add registered members
            while (membersRs.next()) {
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph(membersRs.getString("member_name"), normalFont)));
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph(membersRs.getString("role"), normalFont)));
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph(membersRs.getString("join_date") != null ?
                                membersRs.getString("join_date") : "-", normalFont)));
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph(String.valueOf(membersRs.getInt("payment_count")), normalFont)));

                double total = membersRs.getDouble("total_contributed");
                com.itextpdf.text.Font amountFont = total > 0 ? greenFont : normalFont;
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph("Ksh " + String.format("%,.0f", total), amountFont)));
            }

            // Add simple members
            while (simpleMembersRs.next()) {
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph(simpleMembersRs.getString("fullname") + " (No Account)", orangeFont)));
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph("MEMBER", normalFont)));
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph(simpleMembersRs.getString("join_date") != null ?
                                simpleMembersRs.getString("join_date") : "-", normalFont)));
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph(String.valueOf(simpleMembersRs.getInt("payment_count")), normalFont)));

                double total = simpleMembersRs.getDouble("total_contributed");
                com.itextpdf.text.Font amountFont = total > 0 ? greenFont : normalFont;
                memberTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                        new Paragraph("Ksh " + String.format("%,.0f", total), amountFont)));
            }

            membersRs.close();
            membersPst.close();
            simpleMembersRs.close();
            simpleMembersPst.close();

            document.add(memberTable);
            document.add(new Paragraph(" "));

            // ============================================================
            // RECENT TRANSACTIONS (both types)
            // ============================================================
            if (totalPayments > 0) {
                document.add(new Paragraph("RECENT TRANSACTIONS", headerFont));
                document.add(new Paragraph("─────────────────────────────────────────────────", normalFont));

                PdfPTable transTable = new PdfPTable(5);
                transTable.setWidthPercentage(100);
                transTable.setWidths(new float[]{18f, 28f, 15f, 20f, 19f});

                String[] transHeaders = {"Date", "Member", "Amount", "Method", "Status"};
                for (String h : transHeaders) {
                    com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new Paragraph(h, boldFont));
                    cell.setBackgroundColor(new com.itextpdf.text.BaseColor(200, 200, 200));
                    cell.setPadding(5);
                    transTable.addCell(cell);
                }

                // Sort by date (most recent first)
                contributionsList.sort((a, b) -> {
                    String dateA = (String) a.get("contribution_date");
                    String dateB = (String) b.get("contribution_date");
                    return dateB.compareTo(dateA);
                });

                int count = 0;
                for (Map<String, Object> contrib : contributionsList) {
                    if (count++ >= 20) break;
                    transTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                            new Paragraph((String) contrib.get("contribution_date"), normalFont)));
                    transTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                            new Paragraph((String) contrib.get("member_name"), normalFont)));
                    transTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                            new Paragraph("Ksh " + String.format("%,.0f", (Double) contrib.get("amount")), greenFont)));
                    transTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                            new Paragraph(contrib.get("payment_method") != null ?
                                    (String) contrib.get("payment_method") : "CASH", normalFont)));

                    String status = (String) contrib.get("status");
                    com.itextpdf.text.Font statusFont = "CONFIRMED".equals(status) ? greenFont : normalFont;
                    transTable.addCell(new com.itextpdf.text.pdf.PdfPCell(
                            new Paragraph(status, statusFont)));
                }

                document.add(transTable);
                document.add(new Paragraph(" "));
            }

            // ============================================================
            // FINANCIAL INSIGHTS
            // ============================================================
            document.add(new Paragraph("FINANCIAL INSIGHTS", headerFont));
            document.add(new Paragraph("─────────────────────────────────────────────────", normalFont));

            double avgContribution = totalPayments > 0 ? totalCollected / totalPayments : 0;
            double avgPerMember = memberCount > 0 ? totalCollected / memberCount : 0;

            document.add(new Paragraph("• Average Contribution per Payment: Ksh " + String.format("%,.0f", avgContribution), normalFont));
            document.add(new Paragraph("• Average Contribution per Member: Ksh " + String.format("%,.0f", avgPerMember), normalFont));
            document.add(new Paragraph("• Total Members: " + memberCount + " (" +
                    getRegisteredMemberCount(chamaId) + " registered, " +
                    getSimpleMemberCount(chamaId) + " no account)", normalFont));

            if (progress >= 100) {
                document.add(new Paragraph("🎉 GOAL ACHIEVED! Congratulations to all members!",
                        new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12,
                                com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.GREEN)));
            } else if (progress >= 75) {
                document.add(new Paragraph("🌟 Excellent progress! " + String.format("%.1f%%", progress) + " of goal reached!", normalFont));
            } else if (progress >= 50) {
                document.add(new Paragraph("👍 Good progress! Keep the momentum going!", normalFont));
            } else if (progress > 0) {
                document.add(new Paragraph("💪 Keep pushing! Every contribution brings you closer to your goal.", normalFont));
            } else {
                document.add(new Paragraph("📢 Start contributing today to reach your Chama goal!", normalFont));
            }

            document.add(new Paragraph(" "));

            // Footer
            document.add(new Paragraph("─────────────────────────────────────────────────", normalFont));
            document.add(new Paragraph("This report was generated automatically by Supreme Money Coach.", normalFont));
            document.add(new Paragraph("For support, contact your Chama administrator.", normalFont));
            document.add(new Paragraph("© " + java.time.Year.now().getValue() + " Supreme Money Coach - Your Path to Financial Freedom", normalFont));

            document.close();
            System.out.println("✅ Chama report saved to: " + filepath);

        } catch (Exception e) {
            System.err.println("Error generating Chama report: " + e.getMessage());
            e.printStackTrace();
        }
    }

// ============================================================
// HELPER METHODS FOR COUNTING
// ============================================================

    private static int getRegisteredMemberCount(int chamaId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT COUNT(*) FROM chama_members WHERE chama_id = ? AND status = 'APPROVED'")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int getSimpleMemberCount(int chamaId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT COUNT(*) FROM chama_simple_members WHERE chama_id = ?")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static boolean removeMember(int chamaId, int userId, int adminId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "DELETE FROM chama_members WHERE chama_id = ? AND user_id = ?")) {
            pst.setInt(1, chamaId);
            pst.setInt(2, userId);
            int rows = pst.executeUpdate();

            NotificationService.create(userId,
                    "You have been removed from the Chama by the administrator.",
                    NotificationService.ALERT);
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteChama(int chamaId, int adminId) {
        Connection conn = null;
        try {
            conn = SecureDatabaseConnection.connect();
            conn.setAutoCommit(false);

            System.out.println("=== DELETING CHAMA ID: " + chamaId + " ===");

            // First, delete all contributions for this Chama
            PreparedStatement deleteContributions = conn.prepareStatement(
                    "DELETE FROM chama_contributions WHERE chama_id = ?");
            deleteContributions.setInt(1, chamaId);
            int contribDeleted = deleteContributions.executeUpdate();
            System.out.println("Deleted " + contribDeleted + " contributions");

            // Second, delete all members from this Chama
            PreparedStatement deleteMembers = conn.prepareStatement(
                    "DELETE FROM chama_members WHERE chama_id = ?");
            deleteMembers.setInt(1, chamaId);
            int membersDeleted = deleteMembers.executeUpdate();
            System.out.println("Deleted " + membersDeleted + " members");

            // Finally, delete the Chama group itself
            PreparedStatement deleteGroup = conn.prepareStatement(
                    "DELETE FROM chama_groups WHERE id = ?");
            deleteGroup.setInt(1, chamaId);
            int groupDeleted = deleteGroup.executeUpdate();
            System.out.println("Chama deleted: " + (groupDeleted > 0));

            conn.commit();

            // Verify deletion
            verifyChamaDeleted(chamaId);

            return groupDeleted > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting Chama: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Transaction rolled back");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void verifyChamaDeleted(int chamaId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id FROM chama_groups WHERE id = ?")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                System.out.println("WARNING: Chama still exists after deletion!");
            } else {
                System.out.println("Confirmed: Chama successfully deleted from database");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private static String generateGroupCode() {
        return "CHM" + System.currentTimeMillis() + new Random().nextInt(1000);
    }
}