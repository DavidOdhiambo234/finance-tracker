import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailService {

    private static Properties config = new Properties();
    private static boolean configured = false;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            config.load(fis);
            configured = true;
            System.out.println("✅ Email configuration loaded successfully");
        } catch (IOException e) {
            System.err.println("⚠️ Could not load email config: " + e.getMessage());
            configured = false;
        }
    }

    // =====================================================
    // MAIN SEND EMAIL METHOD - Used by NotificationService
    // =====================================================
    public static boolean sendEmail(String toEmail, String subject, String htmlBody) {
        if (!configured) {
            System.err.println("⚠️ Email service not configured");
            return false;
        }

        String apiKey = config.getProperty("sendgrid.api.key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_SENDGRID_API_KEY_HERE")) {
            System.err.println("⚠️ SendGrid API key not set in config.properties");
            return false;
        }

        try {
            String json = buildSendGridPayload(toEmail, subject, htmlBody);

            URL url = new URL("https://api.sendgrid.com/v3/mail/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 202) {
                System.out.println("✅ Email sent successfully to: " + toEmail);
                return true;
            } else {
                // Read error response
                StringBuilder error = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                }
                System.err.println("❌ SendGrid error (" + responseCode + "): " + error.toString());
                return false;
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
                message.replace("\n", "<br>")
        );

        return sendEmail(toEmail, subject, htmlBody);
    }

    private static String buildSendGridPayload(String toEmail, String subject, String htmlBody) {
        String fromEmail = config.getProperty("sendgrid.from.email", "davidodhiambo4576@gmail.com");
        String fromName = config.getProperty("sendgrid.from.name", "Supreme Money Coach");

        String escapedSubject = subject.replace("\"", "\\\"").replace("\n", " ");
        String escapedBody = htmlBody.replace("\"", "\\\"").replace("\n", "\\n");

        return String.format(
                "{" +
                        "  \"personalizations\": [" +
                        "    {" +
                        "      \"to\": [{\"email\": \"%s\"}]" +
                        "    }" +
                        "  ]," +
                        "  \"from\": {\"email\": \"%s\", \"name\": \"%s\"}," +
                        "  \"subject\": \"%s\"," +
                        "  \"content\": [" +
                        "    {" +
                        "      \"type\": \"text/html\"," +
                        "      \"value\": \"%s\"" +
                        "    }" +
                        "  ]" +
                        "}",
                toEmail, fromEmail, fromName, escapedSubject, escapedBody
        );
    }

    // =====================================================
    // EMAIL TEMPLATE BUILDERS
    // =====================================================
    public static String buildVerificationEmail(String username, String otpCode) {
        String footer = config.getProperty("email.footer", "Supreme Money Coach - Your Financial Intelligence Partner");
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
                username, otpCode, footer
        );
    }

    public static String buildOTPEmail(String username, String otpCode, String purpose) {
        String footer = config.getProperty("email.footer", "Supreme Money Coach - Your Financial Intelligence Partner");
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
                username, purpose, otpCode, footer
        );
    }

    // Test method
    public static boolean testEmailConfig() {
        if (!configured) {
            System.err.println("❌ Email service not configured");
            return false;
        }

        String apiKey = config.getProperty("sendgrid.api.key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_SENDGRID_API_KEY_HERE")) {
            System.err.println("❌ SendGrid API key not set");
            System.err.println("Please get your API key from: https://app.sendgrid.com/settings/api_keys");
            return false;
        }

        System.out.println("📧 Email Configuration Test:");
        System.out.println("  Provider: SendGrid");
        System.out.println("  From Email: " + config.getProperty("sendgrid.from.email"));
        System.out.println("  From Name: " + config.getProperty("sendgrid.from.name"));
        System.out.println("  API Key: " + apiKey.substring(0, 10) + "...");
        System.out.println("✅ Configuration loaded successfully");
        return true;
    }
}