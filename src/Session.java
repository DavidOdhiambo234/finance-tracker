import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    // Private fields - encapsulation
    public static int userId;
    public static String username;
    public static String fullname;
    private static String role;
    private static String sessionId;
    private static LocalDateTime loginTime;
    private static LocalDateTime lastActivityTime;
    private static boolean isAuthenticated = false;

    // Session timeout in milliseconds (30 minutes)
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000;

    // Store active sessions (for admin monitoring)
    private static final ConcurrentHashMap<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    // Private constructor to prevent instantiation
    private Session() {}

    // =====================================================
    //  CREATE NEW SESSION
    // =====================================================
    public static void createSession(int id, String user, String name, String userRole) {
        userId = id;
        username = user;
        fullname = name;
        role = userRole;
        sessionId = generateSessionId();
        loginTime = LocalDateTime.now();
        lastActivityTime = LocalDateTime.now();
        isAuthenticated = true;

        // Store in active sessions
        SessionInfo info = new SessionInfo(userId, username, fullname, role, loginTime);
        activeSessions.put(sessionId, info);

        System.out.println("✅ Session created for: " + username + " (ID: " + sessionId + ")");
    }

    // =====================================================
    //  GENERATE SECURE SESSION ID
    // =====================================================
    private static String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    // =====================================================
    //  VALIDATE SESSION
    // =====================================================
    public static boolean isValid() {
        if (!isAuthenticated) return false;

        // Check if session has expired
        LocalDateTime now = LocalDateTime.now();
        long lastActivityMillis = java.time.Duration.between(lastActivityTime, now).toMillis();

        if (lastActivityMillis > SESSION_TIMEOUT) {
            System.out.println("⏰ Session expired for: " + username);
            clearSession();
            return false;
        }

        // Update last activity time (keep session alive)
        lastActivityTime = now;
        return true;
    }

    // =====================================================
    //  REFRESH SESSION (Extend timeout)
    // =====================================================
    public static void refreshSession() {
        if (isAuthenticated) {
            lastActivityTime = LocalDateTime.now();
            // Update in active sessions
            SessionInfo info = activeSessions.get(sessionId);
            if (info != null) {
                info.lastActivity = lastActivityTime;
            }
        }
    }

    // =====================================================
    //  CLEAR SESSION (Logout)
    // =====================================================
    public static void clearSession() {
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
        userId = 0;
        username = null;
        fullname = null;
        role = null;
        sessionId = null;
        loginTime = null;
        lastActivityTime = null;
        isAuthenticated = false;

        System.out.println("🔒 Session cleared");
    }

    // =====================================================
    //  GETTER METHODS - USE THESE!
    // =====================================================
    public static int getUserId() {
        return userId;
    }

    public static String getUsername() {
        return username;
    }

    public static String getFullname() {
        return fullname;
    }

    public static String getRole() {
        return role;
    }

    public static String getSessionId() {
        return sessionId;
    }

    public static LocalDateTime getLoginTime() {
        return loginTime;
    }

    public static LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public static boolean isAuthenticated() {
        return isAuthenticated;
    }

    public static boolean isAdmin() {
        return isAuthenticated && "ADMIN".equalsIgnoreCase(role);
    }

    public static boolean isLoggedIn() {
        return isAuthenticated && isValid();
    }

    public static long getSessionDuration() {
        if (loginTime == null) return 0;
        return java.time.Duration.between(loginTime, LocalDateTime.now()).toMinutes();
    }

    // =====================================================
    //  GET ACTIVE SESSIONS (For Admin Dashboard)
    // =====================================================
    public static ConcurrentHashMap<String, SessionInfo> getActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    // =====================================================
    //  SETTER METHODS (for LoginForm)
    // =====================================================
    public static void setUserId(int id) {
        userId = id;
    }

    public static void setUsername(String user) {
        username = user;
    }

    public static void setFullname(String name) {
        fullname = name;
    }

    public static void setRole(String userRole) {
        role = userRole;
    }

    // =====================================================
    //  SESSION INFO INNER CLASS
    // =====================================================
    public static class SessionInfo {
        public final int userId;
        public final String username;
        public final String fullname;
        public final String role;
        public final LocalDateTime loginTime;
        public LocalDateTime lastActivity;

        public SessionInfo(int userId, String username, String fullname, String role, LocalDateTime loginTime) {
            this.userId = userId;
            this.username = username;
            this.fullname = fullname;
            this.role = role;
            this.loginTime = loginTime;
            this.lastActivity = loginTime;
        }

        @Override
        public String toString() {
            return String.format("User: %s (%s) | Login: %s | Last Activity: %s | Role: %s",
                    username, userId, loginTime, lastActivity, role);
        }
    }
}