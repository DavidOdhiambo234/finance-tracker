import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiClient {

    private static final String API_KEY = ConfigLoader.getProperty("gemini.api.key");
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public static String callGeminiAPI(String prompt) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return "❌ Gemini API Key not configured!";
        }

        try {
            URL url = URI.create(GEMINI_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            JSONArray parts = new JSONArray();
            parts.put(textPart);
            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", parts);
            JSONArray contents = new JSONArray();
            contents.put(contentObj);
            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                String errorResponse = "";
                try (Scanner err = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8)) {
                    while (err.hasNextLine()) errorResponse += err.nextLine();
                }
                return "⚠️ API Error " + responseCode + "\n" + errorResponse;
            }

            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder rb = new StringBuilder();
                while (scanner.hasNextLine()) rb.append(scanner.nextLine());
                String response = rb.toString();

                JSONObject json = new JSONObject(response);
                String text = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                return text.replace("**", "").replace("##", "")
                        .replace("*", "-").replace("#", "");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "⚠️ Error connecting to AI service: " + ex.getMessage();
        }
    }
}