import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmailService {

    // =====================================================
    // MAIN SEND EMAIL METHOD - Used by NotificationService
    // =====================================================
    public static boolean sendEmail(String toEmail, String subject, String htmlBody) {
        String apiKey = configuredValue("BREVO_API_KEY", "brevo.api.key");
        String fromEmail = configuredValue("MAIL_FROM_EMAIL", "brevo.from.email");
        String fromName = configuredValue("MAIL_FROM_NAME", "brevo.from.name");
        if (fromName.isEmpty()) {
            fromName = "Supreme Money Coach";
        }
        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            System.err.println("⚠️ Email service not configured: set BREVO_API_KEY and MAIL_FROM_EMAIL");
            return false;
        }

        try {
            String json = buildBrevoPayload(toEmail, subject, htmlBody, fromEmail, fromName);
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", apiKey);
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    System.out.println("✅ Email sent successfully to: " + toEmail);
                    return true;
                }

                StringBuilder error = new StringBuilder();
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            error.append(line);
                        }
                    }
                }
                System.err.println("❌ Brevo error (" + responseCode + "): " + error);
                return false;
            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // METHOD FOR NOTIFICATION SERVICE
    // =====================================================
    public static boolean sendNotificationEmail(String toEmail, String subject, String message) {
        // Build a simple HTML email for notifications
        String htmlBody = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif; background-color: #0a1628; color: #e0e8f0; padding: 20px;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; background: #142337; padding: 20px; border-radius: 10px; border: 1px solid #ffc107;'>" +
                        "<h2 style='color: #ffc107;'>◈ SUPREME MONEY COACH</h2>" +
                        "<hr style='border-color: #2a3a55;'>" +
                        "<p style='color: #00a86b;'>%s</p>" +
                        "<hr style='border-color: #2a3a55;'>" +
                        "<p style='color: #8899bb; font-size: 12px;'>This is an automated message from Supreme Money Coach.</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                htmlEscape(message).replace("\n", "<br>")
        );

        return sendEmail(toEmail, subject, htmlBody);
    }

    private static String buildBrevoPayload(String toEmail, String subject, String htmlBody,
                                            String fromEmail, String fromName) {
        return "{\"sender\":{\"email\":\"" + jsonEscape(fromEmail) + "\",\"name\":\""
                + jsonEscape(fromName) + "\"},\"to\":[{\"email\":\"" + jsonEscape(toEmail)
                + "\"}],\"subject\":\"" + jsonEscape(subject) + "\",\"htmlContent\":\""
                + jsonEscape(htmlBody) + "\"}";
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }

    private static String configuredValue(String environmentName, String propertyName) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }
        return ConfigLoader.getProperty(propertyName);
    }

    private static String htmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    // =====================================================
    // EMAIL TEMPLATE BUILDERS
    // =====================================================
    public static String buildVerificationEmail(String username, String otpCode) {
        String footer = ConfigLoader.getProperty("email.footer");
        if (footer.isEmpty()) {
            footer = "Supreme Money Coach - Your Financial Intelligence Partner";
        }
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif; background-color: #0a1628; color: #e0e8f0; padding: 20px;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; background: #142337; padding: 20px; border-radius: 10px; border: 1px solid #ffc107;'>" +
                        "<h2 style='color: #ffc107;'>◈ SUPREME MONEY COACH</h2>" +
                        "<p style='color: #00a86b;'>Hello <strong>%s</strong>,</p>" +
                        "<p>Please verify your email address by entering the OTP code below:</p>" +
                        "<div style='background: #0a1628; padding: 20px; text-align: center; border-radius: 8px; border: 2px dashed #ffc107; margin: 20px 0;'>" +
                        "<span style='font-size: 36px; font-weight: bold; color: #ffc107; letter-spacing: 8px;'>%s</span>" +
                        "</div>" +
                        "<p style='color: #ff6b6b; font-size: 12px;'>⚠️ This code will expire in 10 minutes</p>" +
                        "<hr style='border-color: #2a3a55;'>" +
                        "<p style='color: #8899bb; font-size: 12px;'>%s</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                htmlEscape(username), otpCode, htmlEscape(footer)
        );
    }

    public static String buildOTPEmail(String username, String otpCode, String purpose) {
        String footer = ConfigLoader.getProperty("email.footer");
        if (footer.isEmpty()) {
            footer = "Supreme Money Coach - Your Financial Intelligence Partner";
        }
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif; background-color: #0a1628; color: #e0e8f0; padding: 20px;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; background: #142337; padding: 20px; border-radius: 10px; border: 1px solid #ffc107;'>" +
                        "<h2 style='color: #ffc107;'>◈ SUPREME MONEY COACH</h2>" +
                        "<p style='color: #00a86b;'>Hello <strong>%s</strong>,</p>" +
                        "<p>You requested an OTP for <strong style='color: #ffc107;'>%s</strong></p>" +
                        "<div style='background: #0a1628; padding: 20px; text-align: center; border-radius: 8px; border: 2px dashed #ffc107; margin: 20px 0;'>" +
                        "<span style='font-size: 36px; font-weight: bold; color: #ffc107; letter-spacing: 8px;'>%s</span>" +
                        "</div>" +
                        "<p style='color: #ff6b6b; font-size: 12px;'>⚠️ This code will expire in 10 minutes. Never share this OTP with anyone.</p>" +
                        "<hr style='border-color: #2a3a55;'>" +
                        "<p style='color: #8899bb; font-size: 12px;'>%s</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                htmlEscape(username), htmlEscape(purpose), otpCode, htmlEscape(footer)
        );
    }

    // Test method
    public static boolean testEmailConfig() {
        String apiKey = configuredValue("BREVO_API_KEY", "brevo.api.key");
        String fromEmail = configuredValue("MAIL_FROM_EMAIL", "brevo.from.email");
        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            System.err.println("❌ Brevo is not configured. Set BREVO_API_KEY and MAIL_FROM_EMAIL.");
            return false;
        }

        System.out.println("📧 Email Configuration Test:");
        System.out.println("  Provider: Brevo");
        System.out.println("  From Email: " + fromEmail);
        System.out.println("✅ Configuration loaded successfully");
        return true;
    }
}