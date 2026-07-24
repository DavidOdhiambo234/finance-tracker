import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    public static final String INFO    = "INFO";
    public static final String SUCCESS = "SUCCESS";
    public static final String WARNING = "WARNING";
    public static final String ALERT   = "ALERT";

    // ── Create notification ──
    public static void create(int userId, String message, String type) {
        // ========== FIXED: Don't truncate messages when saving ==========
        // Store the full message - database column should be TEXT or VARCHAR(500)
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO notifications (user_id, message, type, created_at, is_read) VALUES (?, ?, ?, NOW(), 0)")) {
            pst.setInt(1, userId);
            pst.setString(2, message); // Full message
            pst.setString(3, type);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Get unread count ──
    public static int getUnreadCount(int userId) {
        try (
                Connection conn = SecureDatabaseConnection.connect();
                PreparedStatement pst = conn.prepareStatement(
                        "SELECT COUNT(*) FROM notifications " +
                                "WHERE user_id=? AND is_read=FALSE")
        ) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    // ── Get all notifications ──
    public static List<String[]> getAll(int userId) {
        List<String[]> notifications = new ArrayList<>();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, message, type, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                notifications.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("message"), // Full message
                        rs.getString("type"),
                        String.valueOf(rs.getBoolean("is_read")),
                        rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notifications;
    }

    // ── Mark all as read ──
    public static void markAllRead(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0")) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Delete all ──
    public static void clearAll(int userId) {
        try (
                Connection conn = SecureDatabaseConnection.connect();
                PreparedStatement pst = conn.prepareStatement(
                        "DELETE FROM notifications WHERE user_id=?")
        ) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ── Auto-check milestones and notify ──
    public static void checkMilestones(int userId, int balance, int goal) {
        double pct = ((double) balance / goal) * 100;

        if (balance >= goal) {
            create(userId,
                    "🎉 GOAL REACHED! You have saved Ksh " + goal +
                            " — your full savings goal! Amazing achievement!", SUCCESS);
        } else if (pct >= 75) {
            create(userId,
                    "🔥 75% milestone! You have saved Ksh " + balance +
                            " — only Ksh " + (goal - balance) + " remaining!", INFO);
        } else if (pct >= 50) {
            create(userId,
                    "💪 Halfway there! Ksh " + balance + " saved out of Ksh " +
                            goal + ". Keep going!", INFO);
        } else if (pct >= 25) {
            create(userId,
                    "📈 25% milestone reached! Ksh " + balance +
                            " saved so far. Great start!", INFO);
        }
    }

    // ── Notify on deposit ──
    public static void onDeposit(int userId, int amount, int newBalance, int goal) {
        create(userId,
                "✅ Deposit confirmed: Ksh " + amount +
                        " | New balance: Ksh " + newBalance, SUCCESS);
        checkMilestones(userId, newBalance, goal);
    }

    // ── Notify on withdrawal ──
    public static void onWithdrawal(int userId, int amount, int newBalance) {
        create(userId,
                "⚠ Withdrawal: Ksh " + amount +
                        " | Remaining balance: Ksh " + newBalance, WARNING);
        if (newBalance == 0) {
            create(userId,
                    "🚨 ALERT: Your savings balance is now Ksh 0. " +
                            "Consider making a deposit to stay on track.", ALERT);
        }
    }

    // ── Send email if verified ──
    private static void sendEmailIfVerified(int userId, String message) {
        new Thread(() -> {
            try (
                    Connection conn = SecureDatabaseConnection.connect();
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT email, username FROM users " +
                                    "WHERE id=? AND email_verified=TRUE AND email IS NOT NULL")
            ) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    EmailService.sendNotificationEmail(
                            rs.getString("email"),
                            rs.getString("username"),
                            message);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}