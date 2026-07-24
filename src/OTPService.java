import java.sql.*;
import java.util.Random;

public class OTPService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    public static String generateOTP() {
        Random random = new Random();
        // Generate 6-digit OTP
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public static boolean sendEmailVerification(int userId, String email) {
        try {
            // Generate OTP
            String otpCode = generateOTP();
            System.out.println("🔑 Generated OTP for " + email + ": " + otpCode);

            // Store OTP in database
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "INSERT INTO email_verifications (user_id, email, otp_code, expires_at, purpose) "
                                 + "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE), 'VERIFICATION')")) {
                pst.setInt(1, userId);
                pst.setString(2, email);
                pst.setString(3, otpCode);
                pst.setInt(4, OTP_EXPIRY_MINUTES);
                int rows = pst.executeUpdate();
                System.out.println("✅ OTP stored in database. Rows affected: " + rows);
            }

            // Get username
            String username = getUsername(userId);

            // Build email with OTP
            String subject = "🔐 Verify Your Email - Supreme Money Coach";
            String htmlBody = EmailService.buildVerificationEmail(username, otpCode);

            System.out.println("📧 Sending verification email to: " + email);
            System.out.println("📝 OTP Code: " + otpCode);

            return EmailService.sendEmail(email, subject, htmlBody);

        } catch (Exception e) {
            System.err.println("❌ Error sending verification email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean sendOTPToUser(int userId, String purpose) {
        try {
            // Get user email
            String email = getUserEmail(userId);
            if (email == null || email.isEmpty()) {
                System.err.println("❌ No email found for user: " + userId);
                return false;
            }

            // Generate OTP
            String otpCode = generateOTP();
            System.out.println("🔑 Generated OTP for " + email + " (" + purpose + "): " + otpCode);

            // Store OTP in database
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "INSERT INTO email_verifications (user_id, email, otp_code, expires_at, purpose) "
                                 + "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE), ?)")) {
                pst.setInt(1, userId);
                pst.setString(2, email);
                pst.setString(3, otpCode);
                pst.setInt(4, OTP_EXPIRY_MINUTES);
                pst.setString(5, purpose);
                pst.executeUpdate();
            }

            // Get username
            String username = getUsername(userId);

            // Build email with OTP
            String subject = "🔑 Your OTP Code - Supreme Money Coach";
            String htmlBody = EmailService.buildOTPEmail(username, otpCode, purpose);

            System.out.println("📧 Sending OTP email to: " + email);
            System.out.println("📝 OTP Code: " + otpCode);

            return EmailService.sendEmail(email, subject, htmlBody);

        } catch (Exception e) {
            System.err.println("❌ Error sending OTP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean confirmEmailToken(int userId, String otpCode) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, email FROM email_verifications "
                             + "WHERE user_id = ? AND otp_code = ? AND expires_at > NOW() "
                             + "AND (purpose = 'VERIFICATION' OR purpose = 'EMAIL_VERIFICATION') "
                             + "AND used = FALSE "
                             + "ORDER BY created_at DESC LIMIT 1")) {
            pst.setInt(1, userId);
            pst.setString(2, otpCode);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int verificationId = rs.getInt("id");
                String email = rs.getString("email");

                // Mark email as verified
                try (PreparedStatement updatePst = conn.prepareStatement(
                        "UPDATE users SET email_verified = TRUE, email = ? WHERE id = ?")) {
                    updatePst.setString(1, email);
                    updatePst.setInt(2, userId);
                    updatePst.executeUpdate();
                }

                // Mark OTP as used
                try (PreparedStatement updatePst = conn.prepareStatement(
                        "UPDATE email_verifications SET used = TRUE WHERE id = ?")) {
                    updatePst.setInt(1, verificationId);
                    updatePst.executeUpdate();
                }

                System.out.println("✅ Email verified successfully for user: " + userId);
                return true;
            }

            System.err.println("❌ Invalid or expired OTP for user: " + userId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean verifyOTP(int userId, String otpCode, String purpose) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id FROM email_verifications "
                             + "WHERE user_id = ? AND otp_code = ? AND expires_at > NOW() "
                             + "AND purpose = ? AND used = FALSE "
                             + "ORDER BY created_at DESC LIMIT 1")) {
            pst.setInt(1, userId);
            pst.setString(2, otpCode);
            pst.setString(3, purpose);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                // Mark OTP as used
                try (PreparedStatement updatePst = conn.prepareStatement(
                        "UPDATE email_verifications SET used = TRUE WHERE id = ?")) {
                    updatePst.setInt(1, rs.getInt("id"));
                    updatePst.executeUpdate();
                }
                System.out.println("✅ OTP verified successfully for user: " + userId);
                return true;
            }

            System.err.println("❌ Invalid or expired OTP for user: " + userId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getUsername(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT username FROM users WHERE id = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "User";
    }

    private static String getUserEmail(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT email FROM users WHERE id = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("email");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}