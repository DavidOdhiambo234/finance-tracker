import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Executors;

public class MobileApiServer {

    private static final int PORT = 8080;
    private static HttpServer server;

    public static void main(String[] args) throws IOException {
        System.out.println("🚀 Starting Mobile API Server...");
        System.out.println("📁 Working Directory: " + System.getProperty("user.dir"));

        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // ============================================================
        // AUTH ENDPOINTS
        // ============================================================
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());

        // ============================================================
        // CHAMA ENDPOINTS
        // ============================================================
        server.createContext("/api/chama/list", new ChamaListHandler());
        server.createContext("/api/chama/details", new ChamaDetailsHandler());
        server.createContext("/api/chama/contribute", new ContributeHandler());
        server.createContext("/api/chama/members", new MembersHandler());
        server.createContext("/api/chama/contributions", new ContributionsHandler());
        server.createContext("/api/chama/create", new CreateChamaHandler());
        server.createContext("/api/chama/add-members", new AddMembersHandler());
        server.createContext("/api/chama/join", new JoinChamaHandler());
        server.createContext("/api/chama/pending-requests", new GetPendingRequestsHandler());
        server.createContext("/api/chama/process-request", new ProcessRequestHandler());
        server.createContext("/api/chama/export-report", new ExportChamaReportHandler());
        server.createContext("/api/chama/update-payment", new UpdatePaymentDetailsHandler());

        // ============================================================
        // USER / PROFILE ENDPOINTS
        // ============================================================
        server.createContext("/api/member/profile", new ProfileHandler());
        server.createContext("/api/member/update", new UpdateProfileHandler());
        server.createContext("/api/user/change-password", new ChangePasswordHandler());
        server.createContext("/api/user/upload-photo", new UploadProfilePhotoHandler());
        server.createContext("/api/user/get-photo", new GetProfilePhotoHandler());
        server.createContext("/api/user/remove-photo", new RemoveProfilePhotoHandler());
        server.createContext("/api/user/upload-photo-base64", new UploadProfilePhotoBase64Handler());

        // ============================================================
        // NOTIFICATION ENDPOINTS
        // ============================================================
        server.createContext("/api/notifications", new GetNotificationsHandler());
        server.createContext("/api/notifications/mark-read", new MarkNotificationReadHandler());
        server.createContext("/api/notifications/mark-all-read", new MarkAllNotificationsReadHandler());
        server.createContext("/api/notifications/delete", new DeleteNotificationHandler());

        // ============================================================
        // SAVINGS ENDPOINTS
        // ============================================================
        server.createContext("/api/savings", new GetSavingsHandler());
        server.createContext("/api/savings/deposit", new DepositHandler());
        server.createContext("/api/savings/withdraw", new WithdrawHandler());
        server.createContext("/api/savings/statement", new StatementHandler());

        // ============================================================
        // DEBT ENDPOINTS
        // ============================================================
        server.createContext("/api/debts", new GetDebtsHandler());
        server.createContext("/api/debts/add", new AddDebtHandler());
        server.createContext("/api/debts/pay", new PayDebtHandler());
        server.createContext("/api/debts/delete", new DeleteDebtHandler());
        server.createContext("/api/debts/remind", new RemindDebtHandler());

        // ============================================================
        // AI ENDPOINTS
        // ============================================================
        server.createContext("/api/ai/chat", new AIChatHandler());
        server.createContext("/api/ai/savings-insight", new AISavingsInsightHandler());
        // ============================================================
// EXPENSE & BUDGET ENDPOINTS
// ============================================================
        server.createContext("/api/expenses/categories", new ExpenseCategoriesHandler());
        server.createContext("/api/expenses", new GetExpensesHandler());
        server.createContext("/api/expenses/add", new AddExpenseHandler());
        server.createContext("/api/expenses/budget", new BudgetHandler());

        // ============================================================
        // VIDEO ENDPOINTS
        // ============================================================
        server.createContext("/api/videos", new GetVideosHandler());
        server.createContext("/api/videos/submit", new SubmitVideoHandler());
        server.createContext("/api/videos/view", new IncrementVideoViewsHandler());
        // ============================================================
        // PAYMENT PROMPT ENDPOINTS
        // ============================================================

        server.createContext("/api/payment/initiate", new InitiatePaymentHandler());
        server.createContext("/api/payment/status", new PaymentStatusHandler());
        server.createContext("/api/payment/methods", new PaymentMethodsHandler());
        server.createContext("/api/payment/history", new PaymentHistoryHandler());

        // ============================================================
        // ✅ ROOT HANDLER - MUST BE BEFORE server.start()
        // ============================================================
        server.createContext("/", exchange -> {
            try {
                // Log current directory
                System.out.println("🔍 Root handler called");
                System.out.println("📁 Current directory: " + System.getProperty("user.dir"));

                // Log all files in /app/src
                File srcDir = new File("src");
                if (srcDir.exists()) {
                    System.out.println("📂 Files in src/:");
                    for (File f : srcDir.listFiles()) {
                        System.out.println("   - " + f.getName() + " (size: " + f.length() + " bytes)");
                    }
                } else {
                    System.out.println("❌ src/ directory does not exist!");
                }

                // Try to find mobile_app.html in multiple locations
                String[] paths = {"src/mobile_app.html", "./src/mobile_app.html", "/app/src/mobile_app.html"};
                String response = null;
                String contentType = "text/html; charset=UTF-8";
                byte[] responseBytes = null;

                for (String path : paths) {
                    File file = new File(path);
                    System.out.println("📂 Checking: " + file.getAbsolutePath() + " - exists: " + file.exists());
                    if (file.exists()) {
                        responseBytes = Files.readAllBytes(file.toPath());
                        response = new String(responseBytes, StandardCharsets.UTF_8);
                        System.out.println("✅ Found file at: " + path + " (size: " + responseBytes.length + " bytes)");
                        break;
                    }
                }

                if (responseBytes == null) {
                    System.out.println("⚠️ No HTML file found, using fallback");
                    response = "<html><head><title>Supreme Money Coach</title></head>" +
                            "<body style='background:#0a1628;color:#e0e8f0;font-family:Arial;text-align:center;padding:50px;'>" +
                            "<h1 style='color:#ffc107;'>💰 Supreme Money Coach</h1>" +
                            "<p style='color:#00a86b;'>✅ API is running!</p>" +
                            "<p>📱 Access API at: <code>/api/</code></p>" +
                            "<p>🔐 Login: <code>/api/login</code></p>" +
                            "<p>📝 Register: <code>/api/register</code></p>" +
                            "</body></html>";
                    responseBytes = response.getBytes(StandardCharsets.UTF_8);
                }

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    String error = "Error: " + e.getMessage();
                    byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(500, errorBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(errorBytes);
                    os.close();
                } catch (IOException ignored) {}
            }
        });

        // ============================================================
        // START THE SERVER (MUST BE LAST!)
        // ============================================================
        server.start();

        System.out.println("✅ Mobile API Server started successfully!");
        System.out.println("📱 Access API at: http://localhost:" + PORT + "/api/");
        System.out.println("");
        System.out.println("📍 Available endpoints:");
        System.out.println("   POST /api/login");
        System.out.println("   POST /api/register");
        System.out.println("   GET  /api/chama/list");
        System.out.println("   GET  /api/chama/details?id=1");
        System.out.println("   POST /api/chama/contribute");
        System.out.println("   GET  /api/chama/members?id=1");
        System.out.println("   GET  /api/chama/contributions?id=1");
        System.out.println("   GET  /api/member/profile");
        System.out.println("   POST /api/member/update");
        System.out.println("   POST /api/chama/create");
        System.out.println("   POST /api/chama/add-members");
        System.out.println("   POST /api/chama/join");
        System.out.println("   POST /api/user/change-password");
        System.out.println("   POST /api/user/upload-photo-base64");
        System.out.println("   GET  /api/user/get-photo");
        System.out.println("   DELETE /api/user/remove-photo");
        System.out.println("   GET  /api/chama/pending-requests");
        System.out.println("   POST /api/chama/process-request");
        System.out.println("   GET  /api/notifications");
        System.out.println("   POST /api/notifications/mark-read");
        System.out.println("   POST /api/notifications/mark-all-read");
        System.out.println("   DELETE /api/notifications/delete");
        System.out.println("   GET  /api/chama/export-report");
        System.out.println("   POST /api/chama/update-payment");
        System.out.println("   GET  /api/savings");
        System.out.println("   POST /api/savings/deposit");
        System.out.println("   POST /api/savings/withdraw");
        System.out.println("   GET  /api/savings/statement");
        System.out.println("   GET  /api/debts");
        System.out.println("   POST /api/debts/add");
        System.out.println("   POST /api/debts/pay");
        System.out.println("   DELETE /api/debts/delete");
        System.out.println("   POST /api/debts/remind");
        System.out.println("   POST /api/ai/chat");
        System.out.println("   POST /api/ai/savings-insight");
        System.out.println("   GET  /api/videos");
        System.out.println("   POST /api/videos/submit");
        System.out.println("   POST /api/videos/view");
        System.out.println("");
        System.out.println("Press Ctrl+C to stop the server");

    }

    // ============================================================
    // CORS HANDLER
    // ============================================================

    private static void handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        handleCors(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }

    // ============================================================
    // COMMON HELPER METHODS
    // ============================================================

    private static JSONObject readRequestBody(HttpExchange exchange) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int length;
        while ((length = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, length);
        }
        return new JSONObject(sb.toString());
    }

    private static String readRequestBodyAsString(HttpExchange exchange) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int length;
        while ((length = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, length);
        }
        return sb.toString();
    }

    private static String generateToken(int userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return userId + "_" + timestamp + "_" + random;
    }

    private static int getUserIdFromToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            System.out.println("⚠️ No Bearer token found");
            return -1;
        }

        String token = auth.substring(7);
        System.out.println("🔍 Token received: " + token);

        try {
            String[] parts = token.split("_");
            if (parts.length >= 1) {
                int userId = Integer.parseInt(parts[0]);
                System.out.println("✅ Extracted user ID from token: " + userId);

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement("SELECT id FROM users WHERE id = ?");
                    pst.setInt(1, userId);
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        System.out.println("✅ User ID " + userId + " exists in database");
                        return userId;
                    } else {
                        System.out.println("❌ User ID " + userId + " NOT found in database!");
                        return -1;
                    }
                } catch (SQLException e) {
                    System.err.println("❌ Database error checking user: " + e.getMessage());
                    return -1;
                }
            }
            return -1;
        } catch (Exception e) {
            System.err.println("❌ Failed to extract user ID from token: " + e.getMessage());
            return -1;
        }
    }

    private static Map<String, String> getQueryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
        }
        return params;
    }

    private static String getUserName(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT COALESCE(fullname, username) as name FROM users WHERE id = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "User";
    }

    private static String getChamaName(int chamaId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT group_name FROM chama_groups WHERE id = ?")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("group_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Chama";
    }

    private static boolean isChamaLeader(int chamaId, int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT COUNT(*) FROM chama_members WHERE chama_id = ? AND user_id = ? AND role = 'LEADER'")) {
            pst.setInt(1, chamaId);
            pst.setInt(2, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean recordContribution(int chamaId, int userId, double amount, String paymentMethod, int recordedBy) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO chama_contributions (chama_id, user_id, amount, payment_method, recorded_by, contribution_date, status) " +
                             "VALUES (?, ?, ?, ?, ?, NOW(), 'CONFIRMED')")) {
            pst.setInt(1, chamaId);
            pst.setInt(2, userId);
            pst.setDouble(3, amount);
            pst.setString(4, paymentMethod);
            pst.setInt(5, recordedBy);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================================================
    // LOGIN HANDLER
    // ============================================================
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                JSONObject request = readRequestBody(exchange);
                String username = request.getString("username");
                String password = request.getString("password");

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "SELECT id, username, fullname, role, is_admin, is_active, password FROM users WHERE username = ?")) {
                    pst.setString(1, username);
                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        String storedPassword = rs.getString("password");

                        System.out.println("🔑 Login attempt for user: " + username + " (ID: " + userId + ")");

                        if (PasswordUtil.verifyPassword(password, storedPassword)) {
                            if (!rs.getBoolean("is_active")) {
                                sendResponse(exchange, 403, "{\"error\":\"Account deactivated\"}");
                                return;
                            }

                            String token = generateToken(userId);
                            System.out.println("✅ Token generated for user ID: " + userId);

                            JSONObject response = new JSONObject();
                            response.put("success", true);
                            response.put("token", token);
                            response.put("user_id", userId);
                            response.put("username", rs.getString("username"));
                            response.put("fullname", rs.getString("fullname"));

                            String role = rs.getString("role");
                            boolean isAdmin = rs.getBoolean("is_admin");
                            if (isAdmin || "ADMIN".equalsIgnoreCase(role)) {
                                response.put("role", "ADMIN");
                            } else {
                                response.put("role", role != null ? role : "USER");
                            }

                            sendResponse(exchange, 200, response.toString());
                        } else {
                            sendResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}");
                        }
                    } else {
                        sendResponse(exchange, 401, "{\"error\":\"User not found\"}");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // REGISTER HANDLER
    // ============================================================
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                JSONObject request = readRequestBody(exchange);
                String fullname = request.getString("fullname");
                String username = request.getString("username");
                String phone = request.getString("phone_number");
                String password = request.getString("password");
                String email = request.optString("email", "");

                if (fullname.isEmpty() || username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"All fields required\"}");
                    return;
                }

                String hashedPassword = PasswordUtil.hashPassword(password);

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement checkPst = conn.prepareStatement(
                            "SELECT id FROM users WHERE username = ? OR phone_number = ?");
                    checkPst.setString(1, username);
                    checkPst.setString(2, phone);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (checkRs.next()) {
                        sendResponse(exchange, 409, "{\"error\":\"Username or phone number already exists\"}");
                        return;
                    }
                    checkRs.close();
                    checkPst.close();

                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO users (" +
                                    "fullname, username, phone_number, email, password, " +
                                    "role, is_admin, is_active, email_verified, phone_verified, " +
                                    "occupation, monthly_income, monthly_expenses, goal_type, risk_level, savings_goal" +
                                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);

                    pst.setString(1, fullname);
                    pst.setString(2, username);
                    pst.setString(3, phone);
                    pst.setString(4, email);
                    pst.setString(5, hashedPassword);
                    pst.setString(6, "USER");
                    pst.setBoolean(7, false);
                    pst.setBoolean(8, true);
                    pst.setBoolean(9, false);
                    pst.setBoolean(10, false);
                    pst.setString(11, "Not specified");
                    pst.setInt(12, 0);
                    pst.setInt(13, 0);
                    pst.setString(14, "General");
                    pst.setString(15, "Medium");
                    pst.setDouble(16, 15000.00);

                    pst.executeUpdate();

                    ResultSet rs = pst.getGeneratedKeys();
                    int userId = 0;
                    if (rs.next()) {
                        userId = rs.getInt(1);
                    }
                    rs.close();
                    pst.close();

                    if (userId > 0) {
                        System.out.println("✅ User registered successfully: " + username + " (ID: " + userId + ")");

                        JSONObject response = new JSONObject();
                        response.put("success", true);
                        response.put("user_id", userId);
                        response.put("message", "Registration successful! Please login.");
                        sendResponse(exchange, 201, response.toString());
                    } else {
                        sendResponse(exchange, 500, "{\"error\":\"Failed to create user\"}");
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    if (e.getMessage().contains("Duplicate entry")) {
                        sendResponse(exchange, 409, "{\"error\":\"Username or phone number already exists\"}");
                    } else {
                        sendResponse(exchange, 500, "{\"error\":\"Database error: " + e.getMessage() + "\"}");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // CHAMA LIST HANDLER
    // ============================================================
    static class ChamaListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                List<Map<String, Object>> chamas = getUserChamasFromDb(userId);
                JSONArray chamaArray = new JSONArray();

                for (Map<String, Object> chama : chamas) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", chama.get("id"));
                    obj.put("name", chama.get("group_name"));
                    obj.put("role", chama.get("role"));
                    obj.put("status", chama.get("status"));
                    obj.put("total_goal", chama.get("total_goal"));
                    obj.put("member_count", getMemberCountFromDb((Integer) chama.get("id")));
                    chamaArray.put(obj);
                }

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("chamas", chamaArray);
                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // CHAMA DETAILS HANDLER
    // ============================================================
    static class ChamaDetailsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                Map<String, String> params = getQueryParams(exchange);
                int chamaId = Integer.parseInt(params.getOrDefault("id", "0"));
                if (chamaId == 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Chama ID required\"}");
                    return;
                }

                JSONObject chamaDetails = getChamaDetailsFromDb(chamaId, userId);
                if (chamaDetails == null) {
                    sendResponse(exchange, 404, "{\"error\":\"Chama not found\"}");
                    return;
                }

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("chama", chamaDetails);
                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // CONTRIBUTE HANDLER - ONLY LEADER
    // ============================================================
    static class ContributeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int chamaId = request.getInt("chama_id");
                int memberUserId = request.getInt("user_id");
                double amount = request.getDouble("amount");
                String method = request.optString("payment_method", "MPESA");

                if (amount <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Amount must be greater than 0\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement checkPst = conn.prepareStatement(
                            "SELECT leader_id, group_name FROM chama_groups WHERE id = ?");
                    checkPst.setInt(1, chamaId);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (!checkRs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"Chama not found\"}");
                        return;
                    }

                    int leaderId = checkRs.getInt("leader_id");
                    String groupName = checkRs.getString("group_name");

                    if (leaderId != userId) {
                        sendResponse(exchange, 403, "{\"error\":\"Only Chama leader can record payments\"}");
                        return;
                    }
                    checkRs.close();
                    checkPst.close();

                    PreparedStatement memberCheck = conn.prepareStatement(
                            "SELECT status FROM chama_members WHERE chama_id = ? AND user_id = ?");
                    memberCheck.setInt(1, chamaId);
                    memberCheck.setInt(2, memberUserId);
                    ResultSet memberRs = memberCheck.executeQuery();

                    boolean isMember = false;
                    if (memberRs.next()) {
                        String status = memberRs.getString("status");
                        if ("APPROVED".equals(status)) {
                            isMember = true;
                        }
                    }
                    memberRs.close();
                    memberCheck.close();

                    if (!isMember) {
                        PreparedStatement simpleCheck = conn.prepareStatement(
                                "SELECT id FROM chama_simple_members WHERE chama_id = ? AND phone_number = (SELECT phone_number FROM users WHERE id = ?)");
                        simpleCheck.setInt(1, chamaId);
                        simpleCheck.setInt(2, memberUserId);
                        ResultSet simpleRs = simpleCheck.executeQuery();
                        if (simpleRs.next()) {
                            isMember = true;
                        }
                        simpleRs.close();
                        simpleCheck.close();
                    }

                    if (!isMember) {
                        sendResponse(exchange, 403, "{\"error\":\"User is not a member of this Chama\"}");
                        return;
                    }

                    boolean success = recordContribution(chamaId, memberUserId, amount, method, userId);

                    if (success) {
                        String memberName = getUserName(memberUserId);

                        JSONObject response = new JSONObject();
                        response.put("success", true);
                        response.put("message", "Payment recorded successfully!");
                        response.put("amount", amount);
                        response.put("payment_method", method);
                        response.put("member_name", memberName);
                        response.put("chama_name", groupName);

                        sendResponse(exchange, 200, response.toString());
                    } else {
                        sendResponse(exchange, 500, "{\"error\":\"Failed to record payment\"}");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // MEMBERS HANDLER
    // ============================================================
    static class MembersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                Map<String, String> params = getQueryParams(exchange);
                int chamaId = Integer.parseInt(params.getOrDefault("id", "0"));
                if (chamaId == 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Chama ID required\"}");
                    return;
                }

                JSONArray members = getChamaMembersFromDb(chamaId);
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("members", members);
                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // CONTRIBUTIONS HANDLER
    // ============================================================
    static class ContributionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                Map<String, String> params = getQueryParams(exchange);
                int chamaId = Integer.parseInt(params.getOrDefault("id", "0"));
                if (chamaId == 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Chama ID required\"}");
                    return;
                }

                JSONArray contributions = getChamaContributionsFromDb(chamaId);
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("contributions", contributions);
                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // PROFILE HANDLER
    // ============================================================
    static class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                System.out.println("📋 Getting profile for user ID: " + userId);

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "SELECT id, username, fullname, phone_number, email, occupation, " +
                                     "monthly_income, monthly_expenses, goal_type, savings_goal, role, " +
                                     "is_active, email_verified, phone_verified FROM users WHERE id = ?")) {
                    pst.setInt(1, userId);
                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {
                        JSONObject profile = new JSONObject();
                        profile.put("user_id", rs.getInt("id"));
                        profile.put("username", rs.getString("username"));
                        profile.put("fullname", rs.getString("fullname"));
                        profile.put("phone_number", rs.getString("phone_number"));
                        profile.put("email", rs.getString("email") != null ? rs.getString("email") : "");
                        profile.put("occupation", rs.getString("occupation") != null ? rs.getString("occupation") : "Not specified");
                        profile.put("monthly_income", rs.getInt("monthly_income"));
                        profile.put("monthly_expenses", rs.getInt("monthly_expenses"));
                        profile.put("goal_type", rs.getString("goal_type") != null ? rs.getString("goal_type") : "General");
                        profile.put("savings_goal", rs.getDouble("savings_goal"));
                        profile.put("role", rs.getString("role") != null ? rs.getString("role") : "USER");
                        profile.put("is_active", rs.getBoolean("is_active"));
                        profile.put("email_verified", rs.getBoolean("email_verified"));
                        profile.put("phone_verified", rs.getBoolean("phone_verified"));

                        JSONObject response = new JSONObject();
                        response.put("success", true);
                        response.put("profile", profile);
                        sendResponse(exchange, 200, response.toString());
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // UPDATE PROFILE HANDLER
    // ============================================================
    static class UpdateProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                System.out.println("📝 Updating profile for user ID: " + userId);

                StringBuilder sql = new StringBuilder("UPDATE users SET ");
                List<String> updates = new ArrayList<>();
                List<Object> values = new ArrayList<>();

                if (request.has("fullname")) {
                    updates.add("fullname = ?");
                    values.add(request.getString("fullname"));
                }
                if (request.has("phone_number")) {
                    updates.add("phone_number = ?");
                    values.add(request.getString("phone_number"));
                }
                if (request.has("email")) {
                    updates.add("email = ?");
                    values.add(request.getString("email"));
                }
                if (request.has("occupation")) {
                    updates.add("occupation = ?");
                    values.add(request.getString("occupation"));
                }
                if (request.has("monthly_income")) {
                    updates.add("monthly_income = ?");
                    values.add(request.getInt("monthly_income"));
                }
                if (request.has("monthly_expenses")) {
                    updates.add("monthly_expenses = ?");
                    values.add(request.getInt("monthly_expenses"));
                }
                if (request.has("goal_type")) {
                    updates.add("goal_type = ?");
                    values.add(request.getString("goal_type"));
                }
                if (request.has("savings_goal")) {
                    updates.add("savings_goal = ?");
                    values.add(request.getDouble("savings_goal"));
                }

                if (updates.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"No fields to update\"}");
                    return;
                }

                sql.append(String.join(", ", updates));
                sql.append(" WHERE id = ?");
                values.add(userId);

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(sql.toString())) {

                    for (int i = 0; i < values.size(); i++) {
                        pst.setObject(i + 1, values.get(i));
                    }

                    int updated = pst.executeUpdate();

                    JSONObject response = new JSONObject();
                    if (updated > 0) {
                        response.put("success", true);
                        response.put("message", "Profile updated successfully");
                    } else {
                        response.put("success", false);
                        response.put("error", "No changes made");
                    }
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // CHANGE PASSWORD HANDLER
    // ============================================================
    static class ChangePasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                String currentPassword = request.getString("current_password");
                String newPassword = request.getString("new_password");

                if (currentPassword == null || currentPassword.isEmpty() ||
                        newPassword == null || newPassword.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Current and new password required\"}");
                    return;
                }

                if (newPassword.length() < 6) {
                    sendResponse(exchange, 400, "{\"error\":\"New password must be at least 6 characters\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT password FROM users WHERE id = ?");
                    pst.setInt(1, userId);
                    ResultSet rs = pst.executeQuery();

                    if (!rs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
                        return;
                    }

                    String storedPassword = rs.getString("password");
                    rs.close();
                    pst.close();

                    if (!PasswordUtil.verifyPassword(currentPassword, storedPassword)) {
                        sendResponse(exchange, 401, "{\"error\":\"Current password is incorrect\"}");
                        return;
                    }

                    String newHashedPassword = PasswordUtil.hashPassword(newPassword);
                    PreparedStatement updatePst = conn.prepareStatement(
                            "UPDATE users SET password = ? WHERE id = ?");
                    updatePst.setString(1, newHashedPassword);
                    updatePst.setInt(2, userId);
                    updatePst.executeUpdate();
                    updatePst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Password changed successfully");
                    sendResponse(exchange, 200, response.toString());

                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // UPLOAD PROFILE PHOTO HANDLER
    // ============================================================
    static class UploadProfilePhotoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.contains("multipart/form-data")) {
                    sendResponse(exchange, 400, "{\"error\":\"Expected multipart/form-data\"}");
                    return;
                }

                String boundary = extractBoundary(contentType);
                byte[] imageData = extractImageData(exchange, boundary);

                if (imageData == null || imageData.length == 0) {
                    sendResponse(exchange, 400, "{\"error\":\"No image data found\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "UPDATE users SET profile_pic = ?, pic_type = 'jpg' WHERE id = ?")) {
                    pst.setBytes(1, imageData);
                    pst.setInt(2, userId);
                    pst.executeUpdate();

                    System.out.println("✅ Profile photo updated for user: " + userId);

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Profile photo uploaded successfully");
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        private String extractBoundary(String contentType) {
            String[] parts = contentType.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("boundary=")) {
                    return part.trim().substring(9);
                }
            }
            return null;
        }

        private byte[] extractImageData(HttpExchange exchange, String boundary) throws IOException {
            InputStream is = exchange.getRequestBody();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] data = baos.toByteArray();

            String dataStr = new String(data, StandardCharsets.ISO_8859_1);
            String boundaryStr = "--" + boundary;

            int startIdx = dataStr.indexOf(boundaryStr);
            while (startIdx != -1) {
                int endIdx = dataStr.indexOf(boundaryStr, startIdx + 1);
                if (endIdx == -1) {
                    endIdx = dataStr.length();
                }

                String part = dataStr.substring(startIdx, endIdx);

                if (part.contains("Content-Type: image/")) {
                    int headerEnd = part.indexOf("\r\n\r\n");
                    if (headerEnd != -1) {
                        String imageDataStr = part.substring(headerEnd + 4);
                        int trailingBoundary = imageDataStr.indexOf("--");
                        if (trailingBoundary != -1) {
                            imageDataStr = imageDataStr.substring(0, trailingBoundary);
                        }
                        return imageDataStr.getBytes(StandardCharsets.ISO_8859_1);
                    }
                }

                startIdx = dataStr.indexOf(boundaryStr, endIdx);
            }

            return null;
        }
    }

    // ============================================================
    // GET PROFILE PHOTO HANDLER
    // ============================================================
    static class GetProfilePhotoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                int userId = -1;

                if (query != null && query.contains("user_id=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("user_id=")) {
                            userId = Integer.parseInt(param.substring(8));
                            break;
                        }
                    }
                }

                if (userId < 0) {
                    userId = getUserIdFromToken(exchange);
                }

                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                System.out.println("📸 Getting profile photo for user ID: " + userId);

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "SELECT profile_pic, pic_type FROM users WHERE id = ?")) {
                    pst.setInt(1, userId);
                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {
                        byte[] imageData = rs.getBytes("profile_pic");
                        String picType = rs.getString("pic_type");

                        if (imageData != null && imageData.length > 0) {
                            System.out.println("✅ Profile photo found, size: " + imageData.length + " bytes");

                            String contentType = "image/jpeg";
                            if (picType != null) {
                                if (picType.equalsIgnoreCase("png")) {
                                    contentType = "image/png";
                                } else if (picType.equalsIgnoreCase("gif")) {
                                    contentType = "image/gif";
                                }
                            }

                            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                            exchange.getResponseHeaders().set("Content-Type", contentType);
                            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                            exchange.sendResponseHeaders(200, imageData.length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(imageData);
                            os.close();
                            return;
                        } else {
                            System.out.println("⚠️ No profile photo found for user: " + userId);
                        }
                    } else {
                        System.out.println("❌ User not found: " + userId);
                    }
                }

                String errorResponse = "{\"error\":\"No profile photo found\"}";
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, errorResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                String errorResponse = "{\"error\":\"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        }
    }

    // ============================================================
    // REMOVE PROFILE PHOTO HANDLER
    // ============================================================
    static class RemoveProfilePhotoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if (!"DELETE".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "UPDATE users SET profile_pic = NULL, pic_type = NULL WHERE id = ?")) {
                    pst.setInt(1, userId);
                    pst.executeUpdate();

                    System.out.println("✅ Profile photo removed for user: " + userId);

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Profile photo removed successfully");
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // UPLOAD PROFILE PHOTO BASE64 HANDLER
    // ============================================================
    static class UploadProfilePhotoBase64Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                System.out.println("📸 Uploading profile photo (base64) for user ID: " + userId);

                JSONObject request = readRequestBody(exchange);
                String base64Image = request.getString("image");
                String imageType = request.optString("type", "jpg");

                if (base64Image == null || base64Image.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"No image data\"}");
                    return;
                }

                byte[] imageData = java.util.Base64.getDecoder().decode(base64Image);
                System.out.println("📸 Image data size: " + imageData.length + " bytes");

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "UPDATE users SET profile_pic = ?, pic_type = ? WHERE id = ?")) {
                    pst.setBytes(1, imageData);
                    pst.setString(2, imageType);
                    pst.setInt(3, userId);
                    int updated = pst.executeUpdate();

                    if (updated > 0) {
                        System.out.println("✅ Profile photo updated for user: " + userId);

                        JSONObject response = new JSONObject();
                        response.put("success", true);
                        response.put("message", "Profile photo uploaded successfully");
                        sendResponse(exchange, 200, response.toString());
                    } else {
                        sendResponse(exchange, 500, "{\"error\":\"Failed to update database\"}");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // GET PENDING REQUESTS HANDLER
    // ============================================================
    static class GetPendingRequestsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                int chamaId = -1;

                if (query != null && query.contains("chama_id=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("chama_id=")) {
                            chamaId = Integer.parseInt(param.substring(9));
                            break;
                        }
                    }
                }

                if (chamaId < 0) {
                    sendResponse(exchange, 400, "{\"error\":\"chama_id is required\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement checkPst = conn.prepareStatement(
                            "SELECT leader_id FROM chama_groups WHERE id = ?");
                    checkPst.setInt(1, chamaId);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (!checkRs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"Chama not found\"}");
                        return;
                    }

                    int leaderId = checkRs.getInt("leader_id");
                    if (leaderId != userId) {
                        sendResponse(exchange, 403, "{\"error\":\"Only Chama leader can view pending requests\"}");
                        return;
                    }
                    checkRs.close();
                    checkPst.close();

                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT cm.id, cm.user_id, cm.requested_at, " +
                                    "COALESCE(u.fullname, u.username) as display_name, " +
                                    "u.username, u.phone_number, u.email " +
                                    "FROM chama_members cm JOIN users u ON cm.user_id = u.id " +
                                    "WHERE cm.chama_id = ? AND cm.status = 'PENDING' " +
                                    "ORDER BY cm.requested_at DESC");
                    pst.setInt(1, chamaId);
                    ResultSet rs = pst.executeQuery();

                    JSONArray requests = new JSONArray();
                    while (rs.next()) {
                        JSONObject request = new JSONObject();
                        request.put("id", rs.getInt("id"));
                        request.put("user_id", rs.getInt("user_id"));
                        request.put("display_name", rs.getString("display_name"));
                        request.put("username", rs.getString("username"));
                        request.put("phone_number", rs.getString("phone_number") != null ? rs.getString("phone_number") : "N/A");
                        request.put("email", rs.getString("email") != null ? rs.getString("email") : "N/A");
                        request.put("requested_at", rs.getString("requested_at"));
                        requests.put(request);
                    }
                    rs.close();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("requests", requests);
                    response.put("count", requests.length());
                    sendResponse(exchange, 200, response.toString());

                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // PROCESS REQUEST HANDLER
    // ============================================================
    static class ProcessRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int requestId = request.getInt("request_id");
                String action = request.getString("action");

                if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
                    sendResponse(exchange, 400, "{\"error\":\"Action must be APPROVE or REJECT\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement getPst = conn.prepareStatement(
                            "SELECT cm.chama_id, cm.user_id, u.username, u.fullname, g.leader_id " +
                                    "FROM chama_members cm JOIN chama_groups g ON cm.chama_id = g.id " +
                                    "JOIN users u ON cm.user_id = u.id " +
                                    "WHERE cm.id = ? AND cm.status = 'PENDING'");
                    getPst.setInt(1, requestId);
                    ResultSet rs = getPst.executeQuery();

                    if (!rs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"Request not found or already processed\"}");
                        return;
                    }

                    int chamaId = rs.getInt("chama_id");
                    int memberUserId = rs.getInt("user_id");
                    String memberUsername = rs.getString("username");
                    String memberFullname = rs.getString("fullname");
                    int leaderId = rs.getInt("leader_id");

                    if (leaderId != userId) {
                        sendResponse(exchange, 403, "{\"error\":\"Only Chama leader can process requests\"}");
                        return;
                    }
                    rs.close();
                    getPst.close();

                    String status = "APPROVE".equals(action) ? "APPROVED" : "REJECTED";
                    PreparedStatement updatePst = conn.prepareStatement(
                            "UPDATE chama_members SET status = ?, approved_by = ?, approved_at = NOW() WHERE id = ?");
                    updatePst.setString(1, status);
                    updatePst.setInt(2, userId);
                    updatePst.setInt(3, requestId);
                    updatePst.executeUpdate();
                    updatePst.close();

                    System.out.println("✅ Request " + requestId + " " + status + " by user: " + userId);

                    try {
                        String message = "APPROVED".equals(action)
                                ? "🎉 Your request to join the Chama has been APPROVED! You can now view and contribute."
                                : "❌ Your request to join the Chama has been REJECTED.";
                        NotificationService.create(memberUserId, message,
                                "APPROVED".equals(action) ? NotificationService.SUCCESS : NotificationService.WARNING);
                        System.out.println("✅ Notification sent to user: " + memberUserId);
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not send notification: " + e.getMessage());
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Request " + action.toLowerCase() + "d successfully");
                    response.put("status", status);
                    response.put("member_username", memberUsername);
                    sendResponse(exchange, 200, response.toString());

                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // GET NOTIFICATIONS HANDLER
    // ============================================================
    static class GetNotificationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                int limit = 50;
                if (query != null && query.contains("limit=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("limit=")) {
                            limit = Integer.parseInt(param.substring(6));
                            break;
                        }
                    }
                }

                System.out.println("📋 Getting notifications for user: " + userId);

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "SELECT id, message, type, is_read, created_at " +
                                     "FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?")) {
                    pst.setInt(1, userId);
                    pst.setInt(2, limit);
                    ResultSet rs = pst.executeQuery();

                    JSONArray notifications = new JSONArray();
                    int unreadCount = 0;

                    while (rs.next()) {
                        JSONObject notif = new JSONObject();
                        notif.put("id", rs.getInt("id"));
                        notif.put("message", rs.getString("message"));
                        notif.put("type", rs.getString("type"));
                        notif.put("is_read", rs.getBoolean("is_read"));
                        notif.put("created_at", rs.getString("created_at"));
                        notifications.put(notif);

                        if (!rs.getBoolean("is_read")) {
                            unreadCount++;
                        }
                    }
                    rs.close();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("notifications", notifications);
                    response.put("unread_count", unreadCount);
                    response.put("total", notifications.length());
                    sendResponse(exchange, 200, response.toString());

                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // MARK NOTIFICATION READ HANDLER
    // ============================================================
    static class MarkNotificationReadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int notificationId = request.getInt("notification_id");

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?")) {
                    pst.setInt(1, notificationId);
                    pst.setInt(2, userId);
                    int updated = pst.executeUpdate();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", updated > 0);
                    if (updated > 0) {
                        response.put("message", "Notification marked as read");
                    } else {
                        response.put("message", "Notification not found");
                    }
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // MARK ALL NOTIFICATIONS READ HANDLER
    // ============================================================
    static class MarkAllNotificationsReadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE")) {
                    pst.setInt(1, userId);
                    int updated = pst.executeUpdate();
                    pst.close();

                    System.out.println("✅ Marked " + updated + " notifications as read for user: " + userId);

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", updated + " notifications marked as read");
                    response.put("count", updated);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // DELETE NOTIFICATION HANDLER
    // ============================================================
    static class DeleteNotificationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"DELETE".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                int notificationId = -1;
                if (query != null && query.contains("id=")) {
                    notificationId = Integer.parseInt(query.substring(3));
                }

                if (notificationId < 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Notification ID required\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "DELETE FROM notifications WHERE id = ? AND user_id = ?")) {
                    pst.setInt(1, notificationId);
                    pst.setInt(2, userId);
                    int deleted = pst.executeUpdate();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", deleted > 0);
                    if (deleted > 0) {
                        response.put("message", "Notification deleted");
                    } else {
                        response.put("message", "Notification not found");
                    }
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // EXPORT CHAMA REPORT HANDLER
    // ============================================================
    static class ExportChamaReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                int chamaId = -1;
                if (query != null && query.contains("chama_id=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("chama_id=")) {
                            chamaId = Integer.parseInt(param.substring(9));
                            break;
                        }
                    }
                }

                if (chamaId < 0) {
                    sendResponse(exchange, 400, "{\"error\":\"chama_id is required\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement checkPst = conn.prepareStatement(
                            "SELECT status, role FROM chama_members WHERE chama_id = ? AND user_id = ?");
                    checkPst.setInt(1, chamaId);
                    checkPst.setInt(2, userId);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (!checkRs.next()) {
                        sendResponse(exchange, 403, "{\"error\":\"You are not a member of this Chama\"}");
                        return;
                    }

                    String status = checkRs.getString("status");
                    String role = checkRs.getString("role");

                    if (!"APPROVED".equals(status) && !"LEADER".equals(role)) {
                        sendResponse(exchange, 403, "{\"error\":\"Your membership is not approved\"}");
                        return;
                    }

                    checkRs.close();
                    checkPst.close();

                    String reportData = generateChamaReportText(chamaId, userId, conn);

                    if (reportData == null) {
                        sendResponse(exchange, 500, "{\"error\":\"Failed to generate report\"}");
                        return;
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("report", reportData);
                    sendResponse(exchange, 200, response.toString());

                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // UPDATE PAYMENT DETAILS HANDLER
    // ============================================================
    static class UpdatePaymentDetailsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int chamaId = request.getInt("chama_id");
                JSONObject paymentDetails = request.getJSONObject("payment_details");

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement checkPst = conn.prepareStatement(
                            "SELECT leader_id FROM chama_groups WHERE id = ?");
                    checkPst.setInt(1, chamaId);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (!checkRs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"Chama not found\"}");
                        return;
                    }

                    int leaderId = checkRs.getInt("leader_id");
                    if (leaderId != userId) {
                        sendResponse(exchange, 403, "{\"error\":\"Only Chama leader can update payment details\"}");
                        return;
                    }
                    checkRs.close();
                    checkPst.close();

                    PreparedStatement updatePst = conn.prepareStatement(
                            "UPDATE chama_groups SET payment_details = ? WHERE id = ?");
                    updatePst.setString(1, paymentDetails.toString());
                    updatePst.setInt(2, chamaId);
                    updatePst.executeUpdate();
                    updatePst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Payment details updated successfully");
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // CREATE CHAMA HANDLER
    // ============================================================
    static class CreateChamaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                String groupName = request.getString("group_name");
                double totalGoal = request.getDouble("total_goal");
                String frequency = request.getString("contribution_frequency");
                String startDate = request.getString("start_date");
                String endDate = request.getString("end_date");
                JSONObject paymentDetails = request.optJSONObject("payment_details");

                if (groupName == null || groupName.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Chama name is required\"}");
                    return;
                }

                if (totalGoal <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Goal must be greater than 0\"}");
                    return;
                }

                String groupCode = generateGroupCode();

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    conn.setAutoCommit(false);

                    String paymentDetailsStr = paymentDetails != null ? paymentDetails.toString() : "{}";

                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO chama_groups (group_name, group_code, created_by, leader_id, total_goal, start_date, end_date, contribution_frequency, status, payment_details) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    pst.setString(1, groupName);
                    pst.setString(2, groupCode);
                    pst.setInt(3, userId);
                    pst.setInt(4, userId);
                    pst.setDouble(5, totalGoal);
                    pst.setDate(6, java.sql.Date.valueOf(startDate));
                    pst.setDate(7, java.sql.Date.valueOf(endDate));
                    pst.setString(8, frequency);
                    pst.setString(9, paymentDetailsStr);
                    pst.executeUpdate();

                    ResultSet rs = pst.getGeneratedKeys();
                    int chamaId = 0;
                    if (rs.next()) {
                        chamaId = rs.getInt(1);
                    }
                    rs.close();
                    pst.close();

                    if (chamaId == 0) {
                        conn.rollback();
                        sendResponse(exchange, 500, "{\"error\":\"Failed to create Chama\"}");
                        return;
                    }

                    PreparedStatement memberPst = conn.prepareStatement(
                            "INSERT INTO chama_members (chama_id, user_id, role, status, join_date) VALUES (?, ?, 'LEADER', 'APPROVED', CURDATE())");
                    memberPst.setInt(1, chamaId);
                    memberPst.setInt(2, userId);
                    memberPst.executeUpdate();
                    memberPst.close();

                    conn.commit();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Chama created successfully!");
                    response.put("chama_id", chamaId);
                    response.put("group_code", groupCode);
                    response.put("group_name", groupName);

                    sendResponse(exchange, 201, response.toString());

                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"Database error: " + e.getMessage() + "\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        private String generateGroupCode() {
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                int index = (int) (Math.random() * chars.length());
                sb.append(chars.charAt(index));
            }
            return sb.toString();
        }
    }

    // ============================================================
    // ADD MEMBERS HANDLER
    // ============================================================
    static class AddMembersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int chamaId = request.getInt("chama_id");
                JSONArray membersArray = request.getJSONArray("members");

                if (chamaId <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid Chama ID\"}");
                    return;
                }

                if (membersArray.length() == 0) {
                    sendResponse(exchange, 400, "{\"error\":\"No members to add\"}");
                    return;
                }

                if (!isChamaLeader(chamaId, userId)) {
                    sendResponse(exchange, 403, "{\"error\":\"Only Chama leader can add members\"}");
                    return;
                }

                String chamaName = getChamaName(chamaId);
                int added = 0;
                List<String> memberNames = new ArrayList<>();
                List<String> memberCodes = new ArrayList<>();

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    conn.setAutoCommit(false);

                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO chama_simple_members (chama_id, fullname, phone_number, mpesa_number, member_code, join_date) " +
                                    "VALUES (?, ?, ?, ?, ?, ?)");

                    for (int i = 0; i < membersArray.length(); i++) {
                        JSONObject member = membersArray.getJSONObject(i);
                        String fullname = member.getString("fullname");
                        String phone = member.getString("phone_number");
                        String mpesa = member.optString("mpesa_number", phone);
                        String memberCode = generateMemberCode(chamaId);

                        pst.setInt(1, chamaId);
                        pst.setString(2, fullname);
                        pst.setString(3, phone);
                        pst.setString(4, mpesa);
                        pst.setString(5, memberCode);
                        pst.setDate(6, java.sql.Date.valueOf(java.time.LocalDate.now()));
                        pst.addBatch();

                        memberNames.add(fullname);
                        memberCodes.add(memberCode);
                        added++;
                    }

                    pst.executeBatch();
                    conn.commit();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", added + " members added successfully to " + chamaName);
                    response.put("added_count", added);
                    response.put("chama_name", chamaName);

                    JSONArray addedMembers = new JSONArray();
                    for (int i = 0; i < memberNames.size(); i++) {
                        JSONObject m = new JSONObject();
                        m.put("name", memberNames.get(i));
                        m.put("code", memberCodes.get(i));
                        addedMembers.put(m);
                    }
                    response.put("members", addedMembers);

                    sendResponse(exchange, 200, response.toString());

                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"Database error: " + e.getMessage() + "\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        private String generateMemberCode(int chamaId) {
            Random random = new Random();
            int number = random.nextInt(9000) + 1000;
            return "CHAMA-" + chamaId + "-" + number;
        }
    }

    // ============================================================
    // JOIN CHAMA HANDLER
    // ============================================================
    static class JoinChamaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                String requestBody = readRequestBodyAsString(exchange);
                JSONObject request;
                try {
                    request = new JSONObject(requestBody);
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid JSON format: " + e.getMessage() + "\"}");
                    return;
                }

                String groupCode = request.optString("group_code", "");
                System.out.println("🔍 Looking for Chama with code: " + groupCode.trim().toUpperCase());

                if (groupCode == null || groupCode.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Chama code is required\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement userCheck = conn.prepareStatement(
                            "SELECT id, username, fullname FROM users WHERE id = ?");
                    userCheck.setInt(1, userId);
                    ResultSet userRs = userCheck.executeQuery();

                    if (!userRs.next()) {
                        System.out.println("❌ User not found with ID: " + userId);
                        sendResponse(exchange, 404, "{\"error\":\"User not found. Please login again.\"}");
                        return;
                    }

                    String username = userRs.getString("username");
                    String fullname = userRs.getString("fullname");
                    System.out.println("✅ User found: " + username + " (ID: " + userId + ")");
                    userRs.close();
                    userCheck.close();

                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT id, group_name, status, leader_id FROM chama_groups WHERE UPPER(TRIM(group_code)) = ?");
                    pst.setString(1, groupCode.trim().toUpperCase());
                    ResultSet rs = pst.executeQuery();

                    if (!rs.next()) {
                        System.out.println("❌ Chama not found with code: " + groupCode);
                        sendResponse(exchange, 404, "{\"error\":\"Chama not found with this code\"}");
                        return;
                    }

                    int chamaId = rs.getInt("id");
                    String chamaName = rs.getString("group_name");
                    String status = rs.getString("status");
                    int leaderId = rs.getInt("leader_id");

                    System.out.println("✅ Found Chama: " + chamaName + " (ID: " + chamaId + ")");

                    if (!"ACTIVE".equals(status)) {
                        sendResponse(exchange, 400, "{\"error\":\"This Chama is not active\"}");
                        return;
                    }

                    PreparedStatement checkPst = conn.prepareStatement(
                            "SELECT id, status FROM chama_members WHERE chama_id = ? AND user_id = ?");
                    checkPst.setInt(1, chamaId);
                    checkPst.setInt(2, userId);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (checkRs.next()) {
                        int memberId = checkRs.getInt("id");
                        String memberStatus = checkRs.getString("status");

                        if ("APPROVED".equals(memberStatus)) {
                            sendResponse(exchange, 400, "{\"error\":\"You are already a member of this Chama\"}");
                            return;
                        } else if ("PENDING".equals(memberStatus)) {
                            PreparedStatement updatePst = conn.prepareStatement(
                                    "UPDATE chama_members SET requested_at = NOW() WHERE id = ?");
                            updatePst.setInt(1, memberId);
                            updatePst.executeUpdate();
                            updatePst.close();

                            System.out.println("✅ Updated existing pending request for user: " + userId);

                            JSONObject response = new JSONObject();
                            response.put("success", true);
                            response.put("message", "Your join request has been resent to the leader!");
                            response.put("chama_name", chamaName);
                            response.put("chama_id", chamaId);
                            response.put("status", "PENDING");
                            response.put("already_pending", true);

                            sendResponse(exchange, 200, response.toString());
                            return;
                        }
                    }
                    checkRs.close();
                    checkPst.close();

                    PreparedStatement insertPst = conn.prepareStatement(
                            "INSERT INTO chama_members (chama_id, user_id, role, status, join_date, requested_at) " +
                                    "VALUES (?, ?, 'MEMBER', 'PENDING', NULL, NOW())");
                    insertPst.setInt(1, chamaId);
                    insertPst.setInt(2, userId);
                    int inserted = insertPst.executeUpdate();
                    insertPst.close();

                    if (inserted > 0) {
                        System.out.println("✅ New join request inserted for user: " + userId + " to Chama: " + chamaId);
                    } else {
                        sendResponse(exchange, 500, "{\"error\":\"Failed to insert join request\"}");
                        return;
                    }

                    try {
                        NotificationService.create(leaderId,
                                "📢 New join request for Chama '" + chamaName + "' from " + fullname + " (" + username + ")!\n\n" +
                                        "Go to Chama Management → Pending Requests to approve or reject.",
                                NotificationService.INFO);
                        System.out.println("✅ Notification sent to leader: " + leaderId);
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not send notification: " + e.getMessage());
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Join request sent successfully!");
                    response.put("chama_name", chamaName);
                    response.put("chama_id", chamaId);
                    response.put("status", "PENDING");
                    response.put("already_pending", false);

                    sendResponse(exchange, 200, response.toString());

                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"Database error: " + e.getMessage() + "\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // SAVINGS HANDLERS
    // ============================================================

    // GET SAVINGS
    static class GetSavingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT COALESCE(SUM(amount), 0) as total_savings, " +
                                    "COALESCE(savings_goal, 15000) as goal " +
                                    "FROM mysaving2 m RIGHT JOIN users u ON m.user_id = u.id WHERE u.id = ?");
                    pst.setInt(1, userId);
                    ResultSet rs = pst.executeQuery();

                    JSONObject savings = new JSONObject();
                    if (rs.next()) {
                        double total = rs.getDouble("total_savings");
                        double goal = rs.getDouble("goal");
                        savings.put("total", total);
                        savings.put("goal", goal);
                        savings.put("progress", goal > 0 ? (total / goal) * 100 : 0);
                        savings.put("remaining", Math.max(goal - total, 0));
                    } else {
                        PreparedStatement goalPst = conn.prepareStatement(
                                "SELECT COALESCE(savings_goal, 15000) as goal FROM users WHERE id = ?");
                        goalPst.setInt(1, userId);
                        ResultSet goalRs = goalPst.executeQuery();
                        if (goalRs.next()) {
                            savings.put("total", 0);
                            savings.put("goal", goalRs.getDouble("goal"));
                            savings.put("progress", 0);
                            savings.put("remaining", goalRs.getDouble("goal"));
                        }
                        goalRs.close();
                        goalPst.close();
                    }
                    rs.close();
                    pst.close();

                    PreparedStatement txnPst = conn.prepareStatement(
                            "SELECT amount, dateOfPayment, day, category FROM mysaving2 " +
                                    "WHERE user_id = ? ORDER BY dateOfPayment DESC LIMIT 10");
                    txnPst.setInt(1, userId);
                    ResultSet txnRs = txnPst.executeQuery();

                    JSONArray transactions = new JSONArray();
                    while (txnRs.next()) {
                        JSONObject txn = new JSONObject();
                        txn.put("amount", txnRs.getDouble("amount"));
                        txn.put("date", txnRs.getString("dateOfPayment"));
                        txn.put("day", txnRs.getString("day"));
                        txn.put("category", txnRs.getString("category") != null ? txnRs.getString("category") : "General");
                        txn.put("type", txnRs.getDouble("amount") >= 0 ? "DEPOSIT" : "WITHDRAWAL");
                        transactions.put(txn);
                    }
                    txnRs.close();
                    txnPst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("savings", savings);
                    response.put("transactions", transactions);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // DEPOSIT
    static class DepositHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                double amount = request.getDouble("amount");
                String source = request.optString("source", "Deposit");
                String note = request.optString("note", "");

                if (amount <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Amount must be greater than 0\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement weekPst = conn.prepareStatement(
                            "SELECT COALESCE(MAX(weekNO), 0) + 1 FROM mysaving2 WHERE user_id = ?");
                    weekPst.setInt(1, userId);
                    ResultSet weekRs = weekPst.executeQuery();
                    int weekNo = 1;
                    if (weekRs.next()) {
                        weekNo = weekRs.getInt(1);
                    }
                    weekRs.close();
                    weekPst.close();

                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO mysaving2 (user_id, weekNO, dateOfPayment, amount, day, category, reason) " +
                                    "VALUES (?, ?, CURDATE(), ?, DAYNAME(CURDATE()), ?, ?)");
                    pst.setInt(1, userId);
                    pst.setInt(2, weekNo);
                    pst.setDouble(3, amount);
                    pst.setString(4, source);
                    pst.setString(5, note.isEmpty() ? "Deposit" : note);
                    pst.executeUpdate();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Deposit successful!");
                    response.put("amount", amount);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // WITHDRAW
    static class WithdrawHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                double amount = request.getDouble("amount");
                String category = request.optString("category", "General");
                String reason = request.optString("reason", "");

                if (amount <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Amount must be greater than 0\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement balancePst = conn.prepareStatement(
                            "SELECT COALESCE(SUM(amount), 0) as balance FROM mysaving2 WHERE user_id = ?");
                    balancePst.setInt(1, userId);
                    ResultSet balanceRs = balancePst.executeQuery();
                    double balance = 0;
                    if (balanceRs.next()) {
                        balance = balanceRs.getDouble("balance");
                    }
                    balanceRs.close();
                    balancePst.close();

                    if (amount > balance) {
                        sendResponse(exchange, 400, "{\"error\":\"Insufficient balance. Available: Ksh " + balance + "\"}");
                        return;
                    }

                    PreparedStatement weekPst = conn.prepareStatement(
                            "SELECT COALESCE(MAX(weekNO), 0) + 1 FROM mysaving2 WHERE user_id = ?");
                    weekPst.setInt(1, userId);
                    ResultSet weekRs = weekPst.executeQuery();
                    int weekNo = 1;
                    if (weekRs.next()) {
                        weekNo = weekRs.getInt(1);
                    }
                    weekRs.close();
                    weekPst.close();

                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO mysaving2 (user_id, weekNO, dateOfPayment, amount, day, category, reason) " +
                                    "VALUES (?, ?, CURDATE(), ?, DAYNAME(CURDATE()), ?, ?)");
                    pst.setInt(1, userId);
                    pst.setInt(2, weekNo);
                    pst.setDouble(3, -amount);
                    pst.setString(4, category);
                    pst.setString(5, reason.isEmpty() ? "Withdrawal" : reason);
                    pst.executeUpdate();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Withdrawal successful!");
                    response.put("amount", amount);
                    response.put("remaining_balance", balance - amount);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // STATEMENT
    static class StatementHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("╔════════════════════════════════════════════════════════════════╗\n");
                sb.append("║                    SUPREME MONEY COACH                          ║\n");
                sb.append("║                     SAVINGS STATEMENT                           ║\n");
                sb.append("╚════════════════════════════════════════════════════════════════╝\n\n");

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement userPst = conn.prepareStatement(
                            "SELECT fullname, username, savings_goal FROM users WHERE id = ?");
                    userPst.setInt(1, userId);
                    ResultSet userRs = userPst.executeQuery();
                    if (userRs.next()) {
                        sb.append("  Account: ").append(userRs.getString("fullname"))
                                .append(" (").append(userRs.getString("username")).append(")\n");
                        double goal = userRs.getDouble("savings_goal");
                        sb.append("  Goal: Ksh ").append(String.format("%,.0f", goal)).append("\n");
                    }
                    userRs.close();
                    userPst.close();

                    PreparedStatement totalPst = conn.prepareStatement(
                            "SELECT COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) as deposits, " +
                                    "COALESCE(SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END), 0) as withdrawals, " +
                                    "COALESCE(SUM(amount), 0) as balance FROM mysaving2 WHERE user_id = ?");
                    totalPst.setInt(1, userId);
                    ResultSet totalRs = totalPst.executeQuery();
                    if (totalRs.next()) {
                        sb.append("  Total Deposits   : Ksh ").append(String.format("%,.0f", totalRs.getDouble("deposits"))).append("\n");
                        sb.append("  Total Withdrawals: Ksh ").append(String.format("%,.0f", totalRs.getDouble("withdrawals"))).append("\n");
                        sb.append("  Net Balance      : Ksh ").append(String.format("%,.0f", totalRs.getDouble("balance"))).append("\n");
                    }
                    totalRs.close();
                    totalPst.close();

                    sb.append("\n  ─────────────────────────────────────────────────────────────\n");
                    sb.append("  TRANSACTIONS\n");
                    sb.append("  ─────────────────────────────────────────────────────────────\n");
                    sb.append(String.format("  %-6s %-12s %-10s %-15s %s\n", "ID", "DATE", "AMOUNT", "CATEGORY", "DAY"));
                    sb.append("  ─────────────────────────────────────────────────────────────\n");

                    PreparedStatement txnPst = conn.prepareStatement(
                            "SELECT id, dateOfPayment, amount, category, day FROM mysaving2 " +
                                    "WHERE user_id = ? ORDER BY dateOfPayment DESC LIMIT 50");
                    txnPst.setInt(1, userId);
                    ResultSet txnRs = txnPst.executeQuery();
                    while (txnRs.next()) {
                        double amt = txnRs.getDouble("amount");
                        sb.append(String.format("  %-6d %-12s %s%-10s %-15s %s\n",
                                txnRs.getInt("id"),
                                txnRs.getDate("dateOfPayment"),
                                amt >= 0 ? "+" : "-",
                                String.format("Ksh %,.0f", Math.abs(amt)),
                                txnRs.getString("category") != null ? txnRs.getString("category") : "General",
                                txnRs.getString("day") != null ? txnRs.getString("day") : "-"
                        ));
                    }
                    txnRs.close();
                    txnPst.close();

                    sb.append("\n  ════════════════════════════════════════════════════════════\n");
                    sb.append("  Report generated: ").append(new Date()).append("\n");
                    sb.append("  © Supreme Money Coach - Your Path to Financial Freedom\n");
                }

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("statement", sb.toString());
                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // DEBT HANDLERS
    // ============================================================

    // GET DEBTS
    static class GetDebtsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT id, person_name, amount, type, due_date, status, paid_amount FROM debts WHERE user_id = ? ORDER BY due_date ASC");
                    pst.setInt(1, userId);
                    ResultSet rs = pst.executeQuery();

                    JSONArray debts = new JSONArray();
                    double totalIOwe = 0, totalTheyOwe = 0;
                    int overdueCount = 0;

                    while (rs.next()) {
                        JSONObject debt = new JSONObject();
                        debt.put("id", rs.getInt("id"));
                        debt.put("person_name", rs.getString("person_name"));
                        debt.put("amount", rs.getDouble("amount"));
                        debt.put("type", rs.getString("type"));
                        debt.put("due_date", rs.getString("due_date"));
                        debt.put("status", rs.getString("status"));
                        debt.put("paid_amount", rs.getDouble("paid_amount"));

                        String dueDate = rs.getString("due_date");
                        if (dueDate != null && !"PAID".equals(rs.getString("status"))) {
                            try {
                                java.time.LocalDate due = java.time.LocalDate.parse(dueDate);
                                if (due.isBefore(java.time.LocalDate.now())) {
                                    debt.put("overdue", true);
                                    overdueCount++;
                                } else {
                                    debt.put("overdue", false);
                                }
                            } catch (Exception e) {
                                debt.put("overdue", false);
                            }
                        } else {
                            debt.put("overdue", false);
                        }

                        if ("I_OWE".equals(rs.getString("type"))) {
                            totalIOwe += rs.getDouble("amount");
                        } else {
                            totalTheyOwe += rs.getDouble("amount");
                        }
                        debts.put(debt);
                    }
                    rs.close();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("debts", debts);
                    response.put("total_i_owe", totalIOwe);
                    response.put("total_they_owe", totalTheyOwe);
                    response.put("net_position", totalTheyOwe - totalIOwe);
                    response.put("overdue_count", overdueCount);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ADD DEBT
    static class AddDebtHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                String personName = request.getString("person_name");
                double amount = request.getDouble("amount");
                String type = request.getString("type");
                String dueDate = request.getString("due_date");
                String description = request.optString("description", "");

                if (personName == null || personName.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Person name is required\"}");
                    return;
                }

                if (amount <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Amount must be greater than 0\"}");
                    return;
                }

                if (!"I_OWE".equals(type) && !"THEY_OWE".equals(type)) {
                    sendResponse(exchange, 400, "{\"error\":\"Type must be I_OWE or THEY_OWE\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO debts (user_id, person_name, amount, type, due_date, description, status) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')",
                            Statement.RETURN_GENERATED_KEYS);
                    pst.setInt(1, userId);
                    pst.setString(2, personName);
                    pst.setDouble(3, amount);
                    pst.setString(4, type);
                    pst.setDate(5, java.sql.Date.valueOf(dueDate));
                    pst.setString(6, description);
                    pst.executeUpdate();

                    ResultSet rs = pst.getGeneratedKeys();
                    int debtId = 0;
                    if (rs.next()) {
                        debtId = rs.getInt(1);
                    }
                    rs.close();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Debt added successfully!");
                    response.put("debt_id", debtId);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // PAY DEBT
    static class PayDebtHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int debtId = request.getInt("debt_id");
                double amount = request.getDouble("amount");
                String method = request.optString("method", "CASH");

                if (amount <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Amount must be greater than 0\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement getPst = conn.prepareStatement(
                            "SELECT amount, status, person_name FROM debts WHERE id = ? AND user_id = ?");
                    getPst.setInt(1, debtId);
                    getPst.setInt(2, userId);
                    ResultSet rs = getPst.executeQuery();

                    if (!rs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"Debt not found\"}");
                        return;
                    }

                    double currentAmount = rs.getDouble("amount");
                    String personName = rs.getString("person_name");
                    rs.close();
                    getPst.close();

                    if (amount > currentAmount) {
                        sendResponse(exchange, 400, "{\"error\":\"Payment amount exceeds remaining debt: Ksh " + currentAmount + "\"}");
                        return;
                    }

                    double newAmount = currentAmount - amount;
                    String newStatus = newAmount <= 0 ? "PAID" : "PARTIAL";
                    double paidAmount = amount;

                    PreparedStatement updatePst = conn.prepareStatement(
                            "UPDATE debts SET amount = ?, status = ?, paid_amount = COALESCE(paid_amount, 0) + ? WHERE id = ? AND user_id = ?");
                    updatePst.setDouble(1, newAmount);
                    updatePst.setString(2, newStatus);
                    updatePst.setDouble(3, paidAmount);
                    updatePst.setInt(4, debtId);
                    updatePst.setInt(5, userId);
                    updatePst.executeUpdate();
                    updatePst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Payment recorded successfully!");
                    response.put("remaining", newAmount);
                    response.put("status", newStatus);
                    response.put("person_name", personName);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // DELETE DEBT
    static class DeleteDebtHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"DELETE".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int debtId = request.getInt("debt_id");

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "DELETE FROM debts WHERE id = ? AND user_id = ?");
                    pst.setInt(1, debtId);
                    pst.setInt(2, userId);
                    int deleted = pst.executeUpdate();
                    pst.close();

                    JSONObject response = new JSONObject();
                    if (deleted > 0) {
                        response.put("success", true);
                        response.put("message", "Debt deleted successfully!");
                    } else {
                        response.put("success", false);
                        response.put("message", "Debt not found");
                    }
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // REMIND DEBT
    static class RemindDebtHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int debtId = request.getInt("debt_id");

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT person_name, amount, type, due_date FROM debts WHERE id = ? AND user_id = ?");
                    pst.setInt(1, debtId);
                    pst.setInt(2, userId);
                    ResultSet rs = pst.executeQuery();

                    if (!rs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"Debt not found\"}");
                        return;
                    }

                    String personName = rs.getString("person_name");
                    double amount = rs.getDouble("amount");
                    String type = rs.getString("type");
                    String dueDate = rs.getString("due_date");

                    String message;
                    if ("I_OWE".equals(type)) {
                        message = "🔔 DEBT REMINDER: You owe " + personName + " Ksh " + String.format("%,.0f", amount) +
                                ". Due: " + (dueDate != null ? dueDate : "Not set") + ". Please clear your debt.";
                    } else {
                        message = "🔔 DEBT REMINDER: " + personName + " owes you Ksh " + String.format("%,.0f", amount) +
                                ". Due: " + (dueDate != null ? dueDate : "Not set") + ". Follow up on this debt.";
                    }

                    NotificationService.create(userId, message, NotificationService.WARNING);

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Reminder sent successfully!");
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // AI CHAT HANDLER
    // ============================================================
    static class AIChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                String message = request.getString("message");

                if (message == null || message.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Message is required\"}");
                    return;
                }

                String userContext = getUserFinancialContextForAI(userId);
                String prompt = "You are Supreme Money Coach AI financial advisor in Kenya. " +
                        "Reply in English, concisely and helpfully. " +
                        "User context: " + userContext + "\n" +
                        "User question: " + message;

                String aiResponse = GeminiClient.callGeminiAPI(prompt);

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("response", aiResponse);
                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // AI SAVINGS INSIGHT HANDLER
    // ============================================================
    static class AISavingsInsightHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                String userContext = getUserFinancialContextForAI(userId);
                String prompt = "You are a financial advisor AI. Based on this user's data, " +
                        "provide personalized savings insights and recommendations. " +
                        "Be concise and practical. User data: " + userContext;

                String aiResponse = GeminiClient.callGeminiAPI(prompt);

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("insight", aiResponse);
                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // GET VIDEOS HANDLER
    // ============================================================
    static class GetVideosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT id, video_title, business_name, video_url, video_description, " +
                                    "platform, views, payment_status, status, submitted_at, is_featured " +
                                    "FROM video_submissions WHERE status IN ('APPROVED', 'LIVE') " +
                                    "ORDER BY is_featured DESC, views DESC, submitted_at DESC");
                    ResultSet rs = pst.executeQuery();

                    JSONArray videos = new JSONArray();
                    while (rs.next()) {
                        JSONObject video = new JSONObject();
                        video.put("id", rs.getInt("id"));
                        video.put("title", rs.getString("video_title"));
                        video.put("business_name", rs.getString("business_name"));
                        video.put("url", rs.getString("video_url"));
                        video.put("description", rs.getString("video_description"));
                        video.put("platform", rs.getString("platform"));
                        video.put("views", rs.getInt("views"));
                        video.put("status", rs.getString("status"));
                        video.put("is_featured", rs.getBoolean("is_featured"));
                        video.put("submitted_at", rs.getString("submitted_at"));
                        videos.put(video);
                    }
                    rs.close();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("videos", videos);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // SUBMIT VIDEO HANDLER
    // ============================================================
    static class SubmitVideoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                String title = request.getString("title");
                String business = request.getString("business_name");
                String url = request.getString("url");
                String description = request.optString("description", "");
                String platform = request.optString("platform", "YOUTUBE");
                String mpesaCode = request.optString("mpesa_code", "");

                if (title == null || title.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Video title is required\"}");
                    return;
                }

                if (business == null || business.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Business name is required\"}");
                    return;
                }

                if (url == null || url.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Video URL is required\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    String username = getUserName(userId);

                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO video_submissions (user_id, user_name, business_name, video_title, " +
                                    "video_description, platform, video_url, mpesa_code, payment_status, payment_amount, status, submitted_at) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PAID', 20.00, 'PENDING', NOW())",
                            Statement.RETURN_GENERATED_KEYS);
                    pst.setInt(1, userId);
                    pst.setString(2, username);
                    pst.setString(3, business);
                    pst.setString(4, title);
                    pst.setString(5, description);
                    pst.setString(6, platform);
                    pst.setString(7, url);
                    pst.setString(8, mpesaCode);
                    pst.executeUpdate();

                    ResultSet rs = pst.getGeneratedKeys();
                    int videoId = 0;
                    if (rs.next()) {
                        videoId = rs.getInt(1);
                    }
                    rs.close();
                    pst.close();

                    try {
                        NotificationService.create(1,
                                "📹 New video submission: " + title + " from " + username,
                                NotificationService.INFO);
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not notify admin: " + e.getMessage());
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Video submitted successfully! Awaiting admin approval.");
                    response.put("video_id", videoId);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // INCREMENT VIDEO VIEWS HANDLER
    // ============================================================
    static class IncrementVideoViewsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                JSONObject request = readRequestBody(exchange);
                int videoId = request.getInt("video_id");

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "UPDATE video_submissions SET views = views + 1 WHERE id = ?");
                    pst.setInt(1, videoId);
                    pst.executeUpdate();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ============================================================
    // DATABASE HELPER METHODS (Moved to class level)
    // ============================================================

    private static List<Map<String, Object>> getUserChamasFromDb(int userId) {
        List<Map<String, Object>> chamas = new ArrayList<>();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT g.id, g.group_name, g.group_code, g.total_goal, g.status as group_status, " +
                             "cm.role, cm.status as member_status " +
                             "FROM chama_members cm INNER JOIN chama_groups g ON cm.chama_id = g.id " +
                             "WHERE cm.user_id = ? ORDER BY cm.join_date DESC")) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chamas;
    }

    private static int getMemberCountFromDb(int chamaId) {
        int count = 0;
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT COUNT(*) FROM chama_members WHERE chama_id = ? AND status = 'APPROVED'");
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    private static JSONObject getChamaDetailsFromDb(int chamaId, int userId) {
        JSONObject chama = new JSONObject();
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT g.*, COALESCE(u.fullname, u.username) as leader_name " +
                            "FROM chama_groups g JOIN users u ON g.leader_id = u.id WHERE g.id = ?");
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                return null;
            }

            chama.put("id", rs.getInt("id"));
            chama.put("group_name", rs.getString("group_name"));
            chama.put("group_code", rs.getString("group_code"));
            chama.put("leader_name", rs.getString("leader_name"));
            chama.put("total_goal", rs.getDouble("total_goal"));
            chama.put("contribution_frequency", rs.getString("contribution_frequency"));
            chama.put("start_date", rs.getString("start_date"));
            chama.put("end_date", rs.getString("end_date"));
            chama.put("status", rs.getString("status"));

            String paymentDetailsStr = rs.getString("payment_details");
            if (paymentDetailsStr != null && !paymentDetailsStr.isEmpty()) {
                try {
                    JSONObject paymentDetails = new JSONObject(paymentDetailsStr);
                    chama.put("payment_details", paymentDetails);
                } catch (Exception e) {
                    chama.put("payment_details", new JSONObject());
                }
            } else {
                chama.put("payment_details", new JSONObject());
            }

            PreparedStatement totalPst = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) as total FROM chama_contributions WHERE chama_id = ?");
            totalPst.setInt(1, chamaId);
            ResultSet totalRs = totalPst.executeQuery();
            if (totalRs.next()) {
                chama.put("total_collected", totalRs.getDouble("total"));
                double progress = (totalRs.getDouble("total") / rs.getDouble("total_goal")) * 100;
                chama.put("progress", Math.min(progress, 100));
            }
            totalRs.close();
            totalPst.close();

            chama.put("member_count", getMemberCountFromDb(chamaId));

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return chama;
    }

    private static JSONArray getChamaMembersFromDb(int chamaId) {
        JSONArray members = new JSONArray();
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT u.id, COALESCE(u.fullname, u.username) as name, cm.role, cm.join_date " +
                            "FROM chama_members cm JOIN users u ON cm.user_id = u.id " +
                            "WHERE cm.chama_id = ? AND cm.status = 'APPROVED'");
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                JSONObject member = new JSONObject();
                member.put("user_id", rs.getInt("id"));
                member.put("name", rs.getString("name"));
                member.put("role", rs.getString("role"));
                member.put("type", "REGISTERED");
                members.put(member);
            }
            rs.close();
            pst.close();

            List<SimpleMember> simpleMembers = ChamaSimpleMemberManager.getSimpleMembers(chamaId);
            for (SimpleMember sm : simpleMembers) {
                JSONObject member = new JSONObject();
                member.put("user_id", sm.getId());
                member.put("name", sm.getFullname());
                member.put("role", "MEMBER");
                member.put("type", "SIMPLE");
                members.put(member);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    private static JSONArray getChamaContributionsFromDb(int chamaId) {
        JSONArray contributions = new JSONArray();
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT c.amount, c.contribution_date, c.payment_method, " +
                            "COALESCE(u.fullname, u.username) as member_name " +
                            "FROM chama_contributions c JOIN users u ON c.user_id = u.id " +
                            "WHERE c.chama_id = ? ORDER BY c.contribution_date DESC LIMIT 20");
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                JSONObject contrib = new JSONObject();
                contrib.put("amount", rs.getDouble("amount"));
                contrib.put("member_name", rs.getString("member_name"));
                contrib.put("payment_method", rs.getString("payment_method"));
                contrib.put("contribution_date", rs.getString("contribution_date"));
                contrib.put("type", "REGISTERED");
                contributions.put(contrib);
            }
            rs.close();
            pst.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contributions;
    }

    private static String generateChamaReportText(int chamaId, int userId, Connection conn) throws SQLException {
        StringBuilder sb = new StringBuilder();

        PreparedStatement chamaPst = conn.prepareStatement(
                "SELECT g.*, COALESCE(u.fullname, u.username) as leader_name FROM chama_groups g " +
                        "JOIN users u ON g.leader_id = u.id WHERE g.id = ?");
        chamaPst.setInt(1, chamaId);
        ResultSet chamaRs = chamaPst.executeQuery();

        if (!chamaRs.next()) {
            chamaRs.close();
            chamaPst.close();
            return null;
        }

        String groupName = chamaRs.getString("group_name");
        String groupCode = chamaRs.getString("group_code");
        String leaderName = chamaRs.getString("leader_name");
        double totalGoal = chamaRs.getDouble("total_goal");
        String frequency = chamaRs.getString("contribution_frequency");
        String startDate = chamaRs.getString("start_date");
        String endDate = chamaRs.getString("end_date");
        Timestamp createdAt = chamaRs.getTimestamp("created_at");

        chamaRs.close();
        chamaPst.close();

        PreparedStatement membersPst = conn.prepareStatement(
                "SELECT u.id, COALESCE(u.fullname, u.username) as member_name, cm.role, cm.join_date " +
                        "FROM chama_members cm JOIN users u ON cm.user_id = u.id " +
                        "WHERE cm.chama_id = ? AND cm.status = 'APPROVED'");
        membersPst.setInt(1, chamaId);
        ResultSet membersRs = membersPst.executeQuery();

        PreparedStatement contribPst = conn.prepareStatement(
                "SELECT c.amount, c.contribution_date, c.payment_method, COALESCE(u.fullname, u.username) as member_name " +
                        "FROM chama_contributions c JOIN users u ON c.user_id = u.id " +
                        "WHERE c.chama_id = ? AND c.status = 'CONFIRMED' ORDER BY c.contribution_date DESC LIMIT 50");
        contribPst.setInt(1, chamaId);
        ResultSet contribRs = contribPst.executeQuery();

        double totalCollected = 0;
        int totalPayments = 0;
        List<Map<String, Object>> contributions = new ArrayList<>();

        while (contribRs.next()) {
            double amount = contribRs.getDouble("amount");
            totalCollected += amount;
            totalPayments++;

            Map<String, Object> contrib = new HashMap<>();
            contrib.put("amount", amount);
            contrib.put("member_name", contribRs.getString("member_name"));
            contrib.put("date", contribRs.getString("contribution_date"));
            contrib.put("method", contribRs.getString("payment_method"));
            contributions.add(contrib);
        }
        contribRs.close();
        contribPst.close();

        int memberCount = 0;
        List<Map<String, Object>> members = new ArrayList<>();
        while (membersRs.next()) {
            memberCount++;
            Map<String, Object> member = new HashMap<>();
            member.put("name", membersRs.getString("member_name"));
            member.put("role", membersRs.getString("role"));
            member.put("join_date", membersRs.getString("join_date"));
            members.add(member);
        }
        membersRs.close();
        membersPst.close();

        double progress = totalGoal > 0 ? (totalCollected / totalGoal) * 100 : 0;
        double remaining = totalGoal - totalCollected;

        sb.append("╔════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                    SUPREME MONEY COACH                          ║\n");
        sb.append("║                     CHAMA FINANCIAL REPORT                      ║\n");
        sb.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        sb.append("📋 CHAMA INFORMATION\n");
        sb.append("─────────────────────────────────────────────────────────────────\n");
        sb.append("  Name        : ").append(groupName).append("\n");
        sb.append("  Code        : ").append(groupCode).append("\n");
        sb.append("  Leader      : ").append(leaderName).append("\n");
        sb.append("  Created     : ").append(createdAt).append("\n");
        sb.append("  Period      : ").append(startDate).append(" → ").append(endDate).append("\n");
        sb.append("  Frequency   : ").append(frequency).append("\n");
        sb.append("\n");

        sb.append("💰 FINANCIAL SUMMARY\n");
        sb.append("─────────────────────────────────────────────────────────────────\n");
        sb.append("  Total Goal      : Ksh ").append(String.format("%,.0f", totalGoal)).append("\n");
        sb.append("  Total Collected : Ksh ").append(String.format("%,.0f", totalCollected)).append("\n");
        sb.append("  Remaining       : Ksh ").append(String.format("%,.0f", remaining)).append("\n");
        sb.append("  Progress        : ").append(String.format("%.1f", progress)).append("%\n");
        sb.append("  Total Payments  : ").append(totalPayments).append("\n");
        sb.append("  Members         : ").append(memberCount).append("\n");
        sb.append("\n");

        int barLength = 40;
        int filled = (int)((progress / 100) * barLength);
        sb.append("  Progress: [");
        for (int i = 0; i < barLength; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        sb.append("] ").append(String.format("%.1f%%", progress)).append("\n\n");

        sb.append("👥 MEMBERS\n");
        sb.append("─────────────────────────────────────────────────────────────────\n");
        sb.append(String.format("  %-25s %-10s %-12s\n", "Name", "Role", "Join Date"));
        sb.append("  ─────────────────────────────────────────────────────────────\n");
        for (Map<String, Object> member : members) {
            sb.append(String.format("  %-25s %-10s %-12s\n",
                    member.get("name"), member.get("role"), member.get("join_date") != null ? member.get("join_date") : "-"));
        }
        sb.append("\n");

        sb.append("💳 RECENT CONTRIBUTIONS\n");
        sb.append("─────────────────────────────────────────────────────────────────\n");
        if (contributions.isEmpty()) {
            sb.append("  No contributions yet\n");
        } else {
            sb.append(String.format("  %-25s %-12s %-10s %-12s\n", "Member", "Amount", "Method", "Date"));
            sb.append("  ─────────────────────────────────────────────────────────────\n");
            int count = 0;
            for (Map<String, Object> contrib : contributions) {
                if (count++ >= 20) break;
                sb.append(String.format("  %-25s Ksh %-,8.0f %-10s %-12s\n",
                        contrib.get("member_name"), (double) contrib.get("amount"),
                        contrib.get("method") != null ? contrib.get("method") : "CASH", contrib.get("date")));
            }
        }
        sb.append("\n");

        sb.append("📊 INSIGHTS\n");
        sb.append("─────────────────────────────────────────────────────────────────\n");
        if (progress >= 100) {
            sb.append("  🎉 GOAL ACHIEVED! Congratulations to all members!\n");
        } else if (progress >= 75) {
            sb.append("  🌟 Excellent progress! ").append(String.format("%.1f", progress)).append("% of goal reached!\n");
        } else if (progress >= 50) {
            sb.append("  👍 Good progress! Keep the momentum going!\n");
        } else if (progress > 0) {
            sb.append("  💪 Keep pushing! Every contribution brings you closer to your goal.\n");
        } else {
            sb.append("  📢 Start contributing today to reach your Chama goal!\n");
        }
        sb.append("\n");

        sb.append("═══════════════════════════════════════════════════════════════════\n");
        sb.append("  Report generated: ").append(new Date()).append("\n");
        sb.append("  © Supreme Money Coach - Your Path to Financial Freedom\n");
        sb.append("═══════════════════════════════════════════════════════════════════\n");

        return sb.toString();
    }

    private static String getUserFinancialContextForAI(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) as savings, " +
                            "COALESCE(savings_goal, 15000) as goal, " +
                            "occupation, monthly_income, monthly_expenses " +
                            "FROM mysaving2 m RIGHT JOIN users u ON m.user_id = u.id WHERE u.id = ?");
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return String.format(
                        "Savings: Ksh %.0f, Goal: Ksh %.0f, Progress: %.0f%%, " +
                                "Occupation: %s, Income: Ksh %d, Expenses: Ksh %d",
                        rs.getDouble("savings"),
                        rs.getDouble("goal"),
                        rs.getDouble("goal") > 0 ? (rs.getDouble("savings") / rs.getDouble("goal")) * 100 : 0,
                        rs.getString("occupation"),
                        rs.getInt("monthly_income"),
                        rs.getInt("monthly_expenses")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "No financial data available";
    }
    // ============================================================
// PAYMENT METHODS HANDLER
// ============================================================
    static class PaymentMethodsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                Map<String, String> params = getQueryParams(exchange);
                int chamaId = Integer.parseInt(params.getOrDefault("chama_id", "0"));

                if (chamaId == 0) {
                    sendResponse(exchange, 400, "{\"error\":\"chama_id required\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    // Check if user is a member
                    PreparedStatement checkPst = conn.prepareStatement(
                            "SELECT status FROM chama_members WHERE chama_id = ? AND user_id = ?");
                    checkPst.setInt(1, chamaId);
                    checkPst.setInt(2, userId);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (!checkRs.next()) {
                        sendResponse(exchange, 403, "{\"error\":\"Not a member of this Chama\"}");
                        return;
                    }
                    checkRs.close();
                    checkPst.close();

                    // Get payment methods
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT method_type, details, is_active FROM chama_payment_methods WHERE chama_id = ? AND is_active = TRUE");
                    pst.setInt(1, chamaId);
                    ResultSet rs = pst.executeQuery();

                    JSONArray methods = new JSONArray();
                    while (rs.next()) {
                        JSONObject method = new JSONObject();
                        method.put("method_type", rs.getString("method_type"));
                        method.put("details", new JSONObject(rs.getString("details")));
                        method.put("is_active", rs.getBoolean("is_active"));
                        methods.put(method);
                    }
                    rs.close();
                    pst.close();

                    // If no methods configured, return default
                    if (methods.length() == 0) {
                        JSONObject defaultMpesa = new JSONObject();
                        defaultMpesa.put("method_type", "mpesa");
                        JSONObject mpesaDetails = new JSONObject();
                        mpesaDetails.put("paybill", "4572999");
                        mpesaDetails.put("account_number", "CHAMA-" + chamaId);
                        defaultMpesa.put("details", mpesaDetails);
                        defaultMpesa.put("is_active", true);
                        methods.put(defaultMpesa);

                        JSONObject defaultBank = new JSONObject();
                        defaultBank.put("method_type", "bank");
                        JSONObject bankDetails = new JSONObject();
                        bankDetails.put("bank_name", "Equity Bank");
                        bankDetails.put("account_name", "Supreme Money Coach");
                        bankDetails.put("account_number", "1234567890");
                        bankDetails.put("reference", "CHAMA-" + chamaId);
                        defaultBank.put("details", bankDetails);
                        defaultBank.put("is_active", true);
                        methods.put(defaultBank);
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("methods", methods);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    // ============================================================
// INITIATE PAYMENT HANDLER
// ============================================================
    static class InitiatePaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int chamaId = request.getInt("chama_id");
                double amount = request.getDouble("amount");
                String method = request.getString("method");
                String phoneNumber = request.optString("phone_number", "");
                String description = request.optString("description", "");

                if (amount <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Amount must be greater than 0\"}");
                    return;
                }

                // Validate user is a member
                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement checkPst = conn.prepareStatement(
                            "SELECT status FROM chama_members WHERE chama_id = ? AND user_id = ?");
                    checkPst.setInt(1, chamaId);
                    checkPst.setInt(2, userId);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (!checkRs.next()) {
                        sendResponse(exchange, 403, "{\"error\":\"Not a member of this Chama\"}");
                        return;
                    }

                    String status = checkRs.getString("status");
                    if (!"APPROVED".equals(status)) {
                        sendResponse(exchange, 403, "{\"error\":\"Membership not approved. Please wait for leader approval.\"}");
                        return;
                    }
                    checkRs.close();
                    checkPst.close();

                    // Create payment session
                    String transactionId = "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);

                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO payment_sessions (chama_id, member_id, amount, method, status, transaction_id, phone_number, description, created_at) " +
                                    "VALUES (?, ?, ?, ?, 'pending', ?, ?, ?, NOW())",
                            Statement.RETURN_GENERATED_KEYS);
                    pst.setInt(1, chamaId);
                    pst.setInt(2, userId);
                    pst.setDouble(3, amount);
                    pst.setString(4, method);
                    pst.setString(5, transactionId);
                    pst.setString(6, phoneNumber);
                    pst.setString(7, description);
                    pst.executeUpdate();

                    ResultSet rs = pst.getGeneratedKeys();
                    int paymentId = 0;
                    if (rs.next()) {
                        paymentId = rs.getInt(1);
                    }
                    rs.close();
                    pst.close();

                    // In a real implementation, you would call M-Pesa API here
                    // For now, we'll simulate a successful payment
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("payment_id", paymentId);
                    response.put("transaction_id", transactionId);
                    response.put("message", "Payment initiated successfully");
                    sendResponse(exchange, 200, response.toString());

                    // Simulate async processing - in production, this would be a webhook
                    simulatePaymentCompletion(paymentId, chamaId, userId, amount, method, conn);
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        private void simulatePaymentCompletion(int paymentId, int chamaId, int userId, double amount, String method, Connection conn) {
            try {
                // Simulate M-Pesa processing delay
                Thread.sleep(5000);

                // Update payment session to completed
                PreparedStatement pst = conn.prepareStatement(
                        "UPDATE payment_sessions SET status = 'completed', completed_at = NOW() WHERE id = ?");
                pst.setInt(1, paymentId);
                pst.executeUpdate();
                pst.close();

                // Record the contribution
                recordContribution(chamaId, userId, amount, method, userId);

                String userName = getUserName(userId);
                String chamaName = getChamaName(chamaId);

                // Send notification to member
                NotificationService.create(userId,
                        "✅ Your payment of Ksh " + String.format("%,.0f", amount) +
                                " to " + chamaName + " has been confirmed!",
                        NotificationService.SUCCESS);

                // Send notification to leader
                try (PreparedStatement leaderPst = conn.prepareStatement(
                        "SELECT leader_id FROM chama_groups WHERE id = ?")) {
                    leaderPst.setInt(1, chamaId);
                    ResultSet leaderRs = leaderPst.executeQuery();
                    if (leaderRs.next()) {
                        int leaderId = leaderRs.getInt("leader_id");
                        NotificationService.create(leaderId,
                                "💰 New payment received from " + userName +
                                        " for " + chamaName + " (Ksh " + String.format("%,.0f", amount) + ")",
                                NotificationService.INFO);
                    }
                    leaderRs.close();
                }

            } catch (Exception e) {
                System.err.println("Error processing payment: " + e.getMessage());
            }
        }
    }
    // ============================================================
// PAYMENT STATUS HANDLER
// ============================================================
    static class PaymentStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                Map<String, String> params = getQueryParams(exchange);
                int paymentId = Integer.parseInt(params.getOrDefault("payment_id", "0"));

                if (paymentId == 0) {
                    sendResponse(exchange, 400, "{\"error\":\"payment_id required\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT status, transaction_id, amount, method, completed_at FROM payment_sessions WHERE id = ? AND member_id = ?");
                    pst.setInt(1, paymentId);
                    pst.setInt(2, userId);
                    ResultSet rs = pst.executeQuery();

                    if (!rs.next()) {
                        sendResponse(exchange, 404, "{\"error\":\"Payment not found\"}");
                        return;
                    }

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("status", rs.getString("status"));
                    response.put("transaction_id", rs.getString("transaction_id"));
                    response.put("amount", rs.getDouble("amount"));
                    response.put("method", rs.getString("method"));
                    response.put("completed_at", rs.getString("completed_at"));

                    // If completed, get chama name for receipt
                    if ("completed".equals(rs.getString("status"))) {
                        PreparedStatement chamaPst = conn.prepareStatement(
                                "SELECT g.group_name FROM payment_sessions ps JOIN chama_groups g ON ps.chama_id = g.id WHERE ps.id = ?");
                        chamaPst.setInt(1, paymentId);
                        ResultSet chamaRs = chamaPst.executeQuery();
                        if (chamaRs.next()) {
                            response.put("chama_name", chamaRs.getString("group_name"));
                        }
                        chamaRs.close();
                        chamaPst.close();
                    }

                    rs.close();
                    pst.close();

                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    // ============================================================
// PAYMENT HISTORY HANDLER
// ============================================================
    static class PaymentHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                Map<String, String> params = getQueryParams(exchange);
                int limit = Integer.parseInt(params.getOrDefault("limit", "20"));

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT ps.*, g.group_name FROM payment_sessions ps " +
                                    "JOIN chama_groups g ON ps.chama_id = g.id " +
                                    "WHERE ps.member_id = ? ORDER BY ps.created_at DESC LIMIT ?");
                    pst.setInt(1, userId);
                    pst.setInt(2, limit);
                    ResultSet rs = pst.executeQuery();

                    JSONArray payments = new JSONArray();
                    while (rs.next()) {
                        JSONObject payment = new JSONObject();
                        payment.put("id", rs.getInt("id"));
                        payment.put("amount", rs.getDouble("amount"));
                        payment.put("method", rs.getString("method"));
                        payment.put("status", rs.getString("status"));
                        payment.put("transaction_id", rs.getString("transaction_id"));
                        payment.put("chama_name", rs.getString("group_name"));
                        payment.put("created_at", rs.getString("created_at"));
                        payment.put("completed_at", rs.getString("completed_at"));
                        payments.put(payment);
                    }
                    rs.close();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("payments", payments);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    // ============================================================
// EXPENSE CATEGORIES HANDLER
// ============================================================
    static class ExpenseCategoriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT id, category_name as name, icon, color, is_system FROM expense_categories " +
                                    "WHERE is_system = TRUE OR user_id = ? ORDER BY category_name"
                    );
                    pst.setInt(1, userId);
                    ResultSet rs = pst.executeQuery();

                    JSONArray categories = new JSONArray();
                    while (rs.next()) {
                        JSONObject cat = new JSONObject();
                        cat.put("id", rs.getInt("id"));
                        cat.put("name", rs.getString("name"));
                        cat.put("icon", rs.getString("icon") != null ? rs.getString("icon") : "📌");
                        cat.put("color", rs.getString("color") != null ? rs.getString("color") : "#636E72");
                        cat.put("is_system", rs.getBoolean("is_system"));
                        categories.put(cat);
                    }
                    rs.close();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("categories", categories);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
    // ============================================================
// ADD EXPENSE HANDLER
// ============================================================
    static class AddExpenseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int categoryId = request.getInt("category_id");
                double amount = request.getDouble("amount");
                String description = request.optString("description", "");
                String expenseDate = request.getString("expense_date");
                String paymentMethod = request.optString("payment_method", "CASH");
                int chamaId = request.optInt("chama_id", 0);

                if (amount <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Amount must be greater than 0\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    conn.setAutoCommit(false);

                    // Insert expense
                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO expenses (user_id, category_id, chama_id, amount, description, expense_date, payment_method) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    pst.setInt(1, userId);
                    pst.setInt(2, categoryId);
                    pst.setInt(3, chamaId > 0 ? chamaId : 0);
                    pst.setDouble(4, amount);
                    pst.setString(5, description);
                    pst.setDate(6, java.sql.Date.valueOf(expenseDate));
                    pst.setString(7, paymentMethod);
                    pst.executeUpdate();

                    ResultSet rs = pst.getGeneratedKeys();
                    int expenseId = 0;
                    if (rs.next()) {
                        expenseId = rs.getInt(1);
                    }
                    rs.close();
                    pst.close();

                    // Update budget spent amount
                    String monthYear = expenseDate.substring(0, 7) + "-01";
                    PreparedStatement budgetPst = conn.prepareStatement(
                            "UPDATE budgets SET spent = spent + ? " +
                                    "WHERE user_id = ? AND category_id = ? AND month_year = ?"
                    );
                    budgetPst.setDouble(1, amount);
                    budgetPst.setInt(2, userId);
                    budgetPst.setInt(3, categoryId);
                    budgetPst.setDate(4, java.sql.Date.valueOf(monthYear));
                    int updated = budgetPst.executeUpdate();
                    budgetPst.close();

                    // If no budget exists, create one with default amount
                    if (updated == 0) {
                        PreparedStatement createBudget = conn.prepareStatement(
                                "INSERT INTO budgets (user_id, category_id, month_year, amount, spent) " +
                                        "SELECT ?, ?, ?, 10000, ? " +
                                        "FROM expense_categories WHERE id = ?"
                        );
                        createBudget.setInt(1, userId);
                        createBudget.setInt(2, categoryId);
                        createBudget.setDate(3, java.sql.Date.valueOf(monthYear));
                        createBudget.setDouble(4, amount);
                        createBudget.setInt(5, categoryId);
                        createBudget.executeUpdate();
                        createBudget.close();
                    }

                    // Check for budget alert (80% and 100%)
                    PreparedStatement alertPst = conn.prepareStatement(
                            "SELECT id, amount, spent FROM budgets " +
                                    "WHERE user_id = ? AND category_id = ? AND month_year = ?"
                    );
                    alertPst.setInt(1, userId);
                    alertPst.setInt(2, categoryId);
                    alertPst.setDate(3, java.sql.Date.valueOf(monthYear));
                    ResultSet alertRs = alertPst.executeQuery();

                    if (alertRs.next()) {
                        double budgetAmount = alertRs.getDouble("amount");
                        double spent = alertRs.getDouble("spent");
                        double percentage = (spent / budgetAmount) * 100;

                        String categoryName = getCategoryName(conn, categoryId);

                        if (percentage >= 100) {
                            NotificationService.create(userId,
                                    "⚠️ Budget Alert: You've exceeded your " + categoryName + " budget of Ksh " + String.format("%,.0f", budgetAmount),
                                    NotificationService.ALERT
                            );
                        } else if (percentage >= 80) {
                            NotificationService.create(userId,
                                    "⚠️ Budget Alert: You've used " + String.format("%.0f", percentage) + "% of your " + categoryName + " budget",
                                    NotificationService.WARNING
                            );
                        }
                    }
                    alertRs.close();
                    alertPst.close();

                    conn.commit();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Expense added successfully!");
                    response.put("expense_id", expenseId);
                    sendResponse(exchange, 200, response.toString());

                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"Database error: " + e.getMessage() + "\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        private String getCategoryName(Connection conn, int categoryId) throws SQLException {
            PreparedStatement pst = conn.prepareStatement("SELECT category_name FROM expense_categories WHERE id = ?");
            pst.setInt(1, categoryId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getString("category_name");
            }
            return "Category";
        }
    }
    // ============================================================
// GET EXPENSES HANDLER
// ============================================================
    static class GetExpensesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                Map<String, String> params = getQueryParams(exchange);
                String startDate = params.getOrDefault("start_date", "");
                String endDate = params.getOrDefault("end_date", "");
                String limit = params.getOrDefault("limit", "50");

                StringBuilder sql = new StringBuilder(
                        "SELECT e.*, c.category_name as name, c.icon, c.color " +
                                "FROM expenses e JOIN expense_categories c ON e.category_id = c.id " +
                                "WHERE e.user_id = ?"
                );
                List<Object> values = new ArrayList<>();
                values.add(userId);

                if (!startDate.isEmpty()) {
                    sql.append(" AND e.expense_date >= ?");
                    values.add(java.sql.Date.valueOf(startDate));
                }
                if (!endDate.isEmpty()) {
                    sql.append(" AND e.expense_date <= ?");
                    values.add(java.sql.Date.valueOf(endDate));
                }

                sql.append(" ORDER BY e.expense_date DESC, e.id DESC LIMIT ?");
                values.add(Integer.parseInt(limit));

                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(sql.toString())) {

                    for (int i = 0; i < values.size(); i++) {
                        pst.setObject(i + 1, values.get(i));
                    }

                    ResultSet rs = pst.executeQuery();
                    JSONArray expenses = new JSONArray();
                    double total = 0;

                    while (rs.next()) {
                        JSONObject exp = new JSONObject();
                        exp.put("id", rs.getInt("id"));
                        exp.put("category_id", rs.getInt("category_id"));
                        exp.put("category_name", rs.getString("name"));
                        exp.put("icon", rs.getString("icon") != null ? rs.getString("icon") : "📌");
                        exp.put("color", rs.getString("color") != null ? rs.getString("color") : "#636E72");
                        exp.put("amount", rs.getDouble("amount"));
                        exp.put("description", rs.getString("description") != null ? rs.getString("description") : "");
                        exp.put("expense_date", rs.getString("expense_date"));
                        exp.put("payment_method", rs.getString("payment_method"));
                        exp.put("chama_id", rs.getInt("chama_id"));
                        exp.put("created_at", rs.getString("created_at"));
                        expenses.put(exp);
                        total += rs.getDouble("amount");
                    }
                    rs.close();

                    // Get budget summary
                    JSONObject budgetSummary = getBudgetSummary(conn, userId);

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("expenses", expenses);
                    response.put("total", total);
                    response.put("count", expenses.length());
                    response.put("budget_summary", budgetSummary);
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        private JSONObject getBudgetSummary(Connection conn, int userId) throws SQLException {
            JSONObject summary = new JSONObject();

            String currentMonth = java.time.LocalDate.now().withDayOfMonth(1).toString();
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT b.*, c.category_name as name, c.icon, c.color, " +
                            "(b.spent / b.amount * 100) as percentage " +
                            "FROM budgets b JOIN expense_categories c ON b.category_id = c.id " +
                            "WHERE b.user_id = ? AND b.month_year = ?"
            );
            pst.setInt(1, userId);
            pst.setDate(2, java.sql.Date.valueOf(currentMonth));
            ResultSet rs = pst.executeQuery();

            JSONArray budgets = new JSONArray();
            JSONArray alerts = new JSONArray();

            while (rs.next()) {
                JSONObject budget = new JSONObject();
                budget.put("category_id", rs.getInt("category_id"));
                budget.put("category_name", rs.getString("name"));
                budget.put("icon", rs.getString("icon"));
                budget.put("color", rs.getString("color"));
                budget.put("budgeted", rs.getDouble("amount"));
                budget.put("spent", rs.getDouble("spent"));
                budget.put("remaining", rs.getDouble("amount") - rs.getDouble("spent"));
                budget.put("percentage", rs.getDouble("percentage"));

                if (rs.getDouble("percentage") >= 80) {
                    alerts.put(budget);
                }

                budgets.put(budget);
            }
            rs.close();
            pst.close();

            // Get total budget and spending
            PreparedStatement totalPst = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) as total_budget, COALESCE(SUM(spent), 0) as total_spent " +
                            "FROM budgets WHERE user_id = ? AND month_year = ?"
            );
            totalPst.setInt(1, userId);
            totalPst.setDate(2, java.sql.Date.valueOf(currentMonth));
            ResultSet totalRs = totalPst.executeQuery();

            double totalBudget = 0;
            double totalSpent = 0;
            if (totalRs.next()) {
                totalBudget = totalRs.getDouble("total_budget");
                totalSpent = totalRs.getDouble("total_spent");
            }
            totalRs.close();
            totalPst.close();

            summary.put("total_budget", totalBudget);
            summary.put("total_spent", totalSpent);
            summary.put("remaining_total", totalBudget - totalSpent);
            summary.put("budgets", budgets);
            summary.put("alerts", alerts);

            return summary;
        }
    }
    // ============================================================
// BUDGET HANDLER
// ============================================================
    static class BudgetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCors(exchange);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                int userId = getUserIdFromToken(exchange);
                if (userId < 0) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                JSONObject request = readRequestBody(exchange);
                int categoryId = request.getInt("category_id");
                double amount = request.getDouble("amount");
                String monthYear = request.optString("month_year", java.time.LocalDate.now().withDayOfMonth(1).toString());

                if (amount <= 0) {
                    sendResponse(exchange, 400, "{\"error\":\"Amount must be greater than 0\"}");
                    return;
                }

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO budgets (user_id, category_id, month_year, amount) " +
                                    "VALUES (?, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE amount = ?"
                    );
                    pst.setInt(1, userId);
                    pst.setInt(2, categoryId);
                    pst.setDate(3, java.sql.Date.valueOf(monthYear));
                    pst.setDouble(4, amount);
                    pst.setDouble(5, amount);
                    pst.executeUpdate();
                    pst.close();

                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Budget set successfully!");
                    sendResponse(exchange, 200, response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
}