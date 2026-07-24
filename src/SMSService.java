import java.io.*;
import java.net.*;

public class SMSService {

    private static final String API_KEY   = ConfigLoader.getProperty("sms.api.key");
    private static final String USERNAME  = ConfigLoader.getProperty("sms.username");
    private static final String SENDER_ID = ConfigLoader.getProperty("sms.sender.id");

    // ── Use sandbox URL for testing, live URL for production ──
    // Sandbox: api.sandbox.africastalking.com
    // Live:    api.africastalking.com
    private static final boolean SANDBOX  =
            "sandbox".equalsIgnoreCase(ConfigLoader.getProperty("sms.username"));

    private static final String URL = SANDBOX
            ? "https://api.sandbox.africastalking.com/version1/messaging"
            : "https://api.africastalking.com/version1/messaging";

    public static boolean send(String phone, String message) {
        if (API_KEY == null || API_KEY.isEmpty() ||
                API_KEY.equals("YOUR_AFRICASTALKING_API_KEY")) {
            System.out.println("SMS skipped: API key not configured.");
            return false;
        }
        try {
            String formatted = formatPhone(phone);
            if (formatted.isEmpty()) {
                System.out.println("SMS skipped: empty phone.");
                return false;
            }

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apiKey",       API_KEY);
            conn.setRequestProperty("Accept",       "application/json");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            StringBuilder body = new StringBuilder();
            body.append("username=").append(URLEncoder.encode(USERNAME, "UTF-8"));
            body.append("&to=").append(URLEncoder.encode(formatted, "UTF-8"));
            body.append("&message=").append(URLEncoder.encode(message, "UTF-8"));
            if (SENDER_ID != null && !SENDER_ID.trim().isEmpty() && !SANDBOX) {
                body.append("&from=").append(URLEncoder.encode(SENDER_ID, "UTF-8"));
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            System.out.println("SMS to " + formatted + " → HTTP " + code +
                    (SANDBOX ? " [SANDBOX]" : " [LIVE]"));

            if (code != 200 && code != 201) {
                java.util.Scanner err = new java.util.Scanner(
                        conn.getErrorStream(), "UTF-8");
                StringBuilder eb = new StringBuilder();
                while (err.hasNextLine()) eb.append(err.nextLine());
                err.close();
                System.err.println("SMS Error body: " + eb);
            }

            return code == 200 || code == 201;

        } catch (Exception ex) {
            System.err.println("SMS Exception: " + ex.getMessage());
            return false;
        }
    }

    public static boolean sendDebtReminder(String phone, String userName,
                                           String personName, double amount,
                                           String type, String dueDate) {
        String dir = type.equals("I_OWE")
                ? "You owe " + personName + " Ksh " + String.format("%,.0f", amount)
                : personName + " owes you Ksh " + String.format("%,.0f", amount);
        return send(phone,
                "DAVID SAVINGS BANK: Hi " + userName + ". DEBT REMINDER - " +
                        dir + ". Due: " + dueDate + ". Please settle on time.");
    }

    public static boolean sendDepositAlert(String phone, String user, int amount, int balance) {
        return send(phone,
                "DAVID SAVINGS BANK: Hi " + user + ", Ksh " + amount +
                        " deposited. Balance: Ksh " + balance + ". Keep saving!");
    }

    public static boolean sendWithdrawalAlert(String phone, String user, int amount, int balance) {
        return send(phone,
                "DAVID SAVINGS BANK: Hi " + user + ", Ksh " + amount +
                        " withdrawn. Balance: Ksh " + balance + ".");
    }

    public static boolean sendOTP(String phone, String otp) {
        return send(phone,
                "DAVID SAVINGS BANK: Your OTP is " + otp +
                        ". Valid 10 mins. Do NOT share.");
    }

    private static String formatPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return "";
        phone = phone.trim().replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("0") && phone.length() == 10)
            phone = "+254" + phone.substring(1);
        else if (phone.startsWith("254") && !phone.startsWith("+"))
            phone = "+" + phone;
        else if (!phone.startsWith("+"))
            phone = "+" + phone;
        return phone;
    }
    // Add this method to SMSService.java
    public static void sendAnnouncement(String phoneNumber, String username, String title, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty() || phoneNumber.equals("N/A")) {
            System.out.println("No valid phone number for user: " + username);
            return;
        }

        try {
            String formattedMessage = String.format(
                    "📢 DAVID SAVINGS BANK ANNOUNCEMENT\n\n" +
                            "Title: %s\n\n" +
                            "%s\n\n" +
                            "Thank you for banking with us.\n" +
                            "Reply STOP to unsubscribe.",
                    title, message
            );

            // Use your existing SMS sending logic
            sendSms(phoneNumber, formattedMessage);
            System.out.println("Announcement SMS sent to " + phoneNumber);

        } catch (Exception e) {
            System.err.println("Failed to send announcement SMS to " + phoneNumber + ": " + e.getMessage());
        }
    }

    // Make sure you have this base sendSms method
    private static void sendSms(String phoneNumber, String message) {
        // Your existing SMS sending implementation
        // This should use Africa's Talking API or similar
        try {
            // Example using Africa's Talking (configure in config.properties)
            String apiKey = ConfigLoader.getProperty("sms.api.key");
            String username = ConfigLoader.getProperty("sms.username");
            String url = ConfigLoader.getProperty("sms.url");

            // Your SMS sending logic here
            // ...

        } catch (Exception e) {
            System.err.println("SMS error: " + e.getMessage());
        }
    }
}