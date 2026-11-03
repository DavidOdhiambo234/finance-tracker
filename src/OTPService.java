import java.sql.*;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class OTPService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateOTP() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    public static boolean sendEmailVerification(int userId, String email) {
        try {
            // Generate OTP
            String otpCode = generateOTP();

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

            return EmailService.sendEmail(email, subject, htmlBody);

        } catch (Exception e) {
            System.err.println("❌ Error sending OTP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean sendRegistrationOTP(int userId, String email) {
        try {
            String otpCode = generateOTP();
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "INSERT INTO email_verifications (user_id, email, otp_code, expires_at, purpose) "
                                 + "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE), 'REGISTRATION')")) {
                pst.setInt(1, userId);
                pst.setString(2, email);
                pst.setString(3, otpCode);
                pst.setInt(4, OTP_EXPIRY_MINUTES);
                pst.executeUpdate();
            }

            String username = getUsername(userId);
            String subject = "Verify Your Email - Supreme Money Coach";
            String htmlBody = EmailService.buildVerificationEmail(username, otpCode);
            System.out.println("📧 Sending registration verification email to: " + email);
            return EmailService.sendEmail(email, subject, htmlBody);
        } catch (Exception e) {
            System.err.println("❌ Error sending registration OTP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean confirmRegistrationOTP(int userId, String otpCode) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                int verificationId;
                String email;
                try (PreparedStatement pst = conn.prepareStatement(
                        "SELECT id, email FROM email_verifications "
                                + "WHERE user_id = ? AND otp_code = ? AND expires_at > NOW() "
                                + "AND purpose = 'REGISTRATION' AND used = FALSE "
                                + "ORDER BY created_at DESC LIMIT 1 FOR UPDATE")) {
                    pst.setInt(1, userId);
                    pst.setString(2, otpCode);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        verificationId = rs.getInt("id");
                        email = rs.getString("email");
                    }
                }

                try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE users SET email_verified = TRUE, is_active = TRUE "
                                + "WHERE id = ? AND email = ?")) {
                    pst.setInt(1, userId);
                    pst.setString(2, email);
                    if (pst.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }

                try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE email_verifications SET used = TRUE WHERE id = ?")) {
                    pst.setInt(1, verificationId);
                    pst.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error confirming registration OTP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean sendPasswordResetOTP(String username, String email) {
        int userId;
        String fullname;
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        try (Connection conn = SecureDatabaseConnection.connect()) {
            ensurePasswordResetTable(conn);
            try (PreparedStatement pst = conn.prepareStatement(
                    "SELECT id, fullname FROM users "
                            + "WHERE username = ? AND LOWER(email) = LOWER(?) AND is_active = TRUE")) {
                pst.setString(1, username);
                pst.setString(2, email);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    userId = rs.getInt("id");
                    fullname = rs.getString("fullname");
                }
            }

            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pst = conn.prepareStatement(
                        "SELECT COUNT(*) AS hour_count, "
                                + "COALESCE(SUM(created_at > DATE_SUB(NOW(), INTERVAL 60 SECOND)), 0) "
                                + "AS recent_count FROM password_reset_codes "
                                + "WHERE user_id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)")) {
                    pst.setInt(1, userId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next() && (rs.getInt("hour_count") >= 5
                                || rs.getInt("recent_count") > 0)) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE password_reset_codes SET used = 1 WHERE user_id = ? AND used = 0")) {
                    pst.setInt(1, userId);
                    pst.executeUpdate();
                }
                try (PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO password_reset_codes (user_id, code_hash, expires_at) "
                                + "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE))")) {
                    pst.setInt(1, userId);
                    pst.setString(2, sha256Hex(userId + ":" + code));
                    pst.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("❌ Could not create password reset code: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        String safeName = fullname == null || fullname.trim().isEmpty() ? username : fullname;
        boolean sent = EmailService.sendEmail(email,
                "Your Supreme Money password reset code",
                EmailService.buildOTPEmail(safeName, code, "password reset"));
        if (!sent) {
            invalidatePasswordResetCodes(userId);
        }
        return sent;
    }

    public static boolean resetPasswordWithOTP(String username, String email, String code,
                                               String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return false;
        }
        try (Connection conn = SecureDatabaseConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement pst = conn.prepareStatement(
                        "SELECT id FROM users WHERE username = ? AND LOWER(email) = LOWER(?) "
                                + "AND is_active = TRUE FOR UPDATE")) {
                    pst.setString(1, username);
                    pst.setString(2, email);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        userId = rs.getInt("id");
                    }
                }

                int resetId;
                String storedHash;
                int attempts;
                boolean valid;
                try (PreparedStatement pst = conn.prepareStatement(
                        "SELECT id, code_hash, attempts, (expires_at > NOW()) AS still_valid "
                                + "FROM password_reset_codes WHERE user_id = ? AND used = 0 "
                                + "ORDER BY id DESC LIMIT 1 FOR UPDATE")) {
                    pst.setInt(1, userId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        resetId = rs.getInt("id");
                        storedHash = rs.getString("code_hash");
                        attempts = rs.getInt("attempts");
                        valid = rs.getBoolean("still_valid");
                    }
                }

                if (!valid || attempts >= 5) {
                    try (PreparedStatement pst = conn.prepareStatement(
                            "UPDATE password_reset_codes SET used = 1 WHERE id = ?")) {
                        pst.setInt(1, resetId);
                        pst.executeUpdate();
                    }
                    conn.commit();
                    return false;
                }

                byte[] stored = storedHash.getBytes(StandardCharsets.UTF_8);
                byte[] provided = sha256Hex(userId + ":" + code).getBytes(StandardCharsets.UTF_8);
                if (!MessageDigest.isEqual(stored, provided)) {
                    try (PreparedStatement pst = conn.prepareStatement(
                            "UPDATE password_reset_codes SET attempts = attempts + 1, "
                                    + "used = (attempts + 1 >= 5) WHERE id = ?")) {
                        pst.setInt(1, resetId);
                        pst.executeUpdate();
                    }
                    conn.commit();
                    return false;
                }

                try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE users SET password = ? WHERE id = ?")) {
                    pst.setString(1, PasswordUtil.hashPassword(newPassword));
                    pst.setInt(2, userId);
                    pst.executeUpdate();
                }
                try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE password_reset_codes SET used = 1 WHERE user_id = ?")) {
                    pst.setInt(1, userId);
                    pst.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("❌ Could not reset password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void ensurePasswordResetTable(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS password_reset_codes ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY, "
                            + "user_id INT NOT NULL, "
                            + "code_hash CHAR(64) NOT NULL, "
                            + "expires_at DATETIME NOT NULL, "
                            + "attempts INT NOT NULL DEFAULT 0, "
                            + "used TINYINT(1) NOT NULL DEFAULT 0, "
                            + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                            + "INDEX idx_password_reset_codes_user (user_id))");
        }
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void invalidatePasswordResetCodes(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE password_reset_codes SET used = 1 WHERE user_id = ? AND used = 0")) {
            pst.setInt(1, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Could not invalidate unsent password reset code: " + e.getMessage());
            e.printStackTrace();
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