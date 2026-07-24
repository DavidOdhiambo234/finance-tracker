import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SecureDatabaseConnection {
    private static final int MAX_POOL_SIZE = 10;
    private static final BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
    private static boolean initialized = false;

    static {
        initializePool();
    }

    private static void initializePool() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String dbUrl = ConfigLoader.getProperty("db.url");
            String pass = ConfigLoader.getProperty("db.password");

            // 🔥 HARDCODE THE USERNAME - NO SPACES!
            String user = "avnadmin";  // Hardcoded, not from config

            System.out.println("🔐 Initializing Secure Database Connection Pool...");
            System.out.println("Database URL: " + dbUrl);
            System.out.println("Username: '" + user + "'");

            if (pass == null || pass.isEmpty()) {
                System.err.println("❌ Password is empty!");
                return;
            }

            for (int i = 0; i < MAX_POOL_SIZE; i++) {
                Connection conn = DriverManager.getConnection(dbUrl, user, pass);
                pool.offer(conn);
            }

            initialized = true;
            System.out.println("✅ Database connection pool initialized!");

        } catch (Exception e) {
            System.err.println("❌ Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static Connection connect() throws SQLException {
        String dbUrl = ConfigLoader.getProperty("db.url");
        String pass = ConfigLoader.getProperty("db.password");
        String user = "avnadmin";  // Hardcoded

        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new SQLException("Database URL is missing!");
        }

        return DriverManager.getConnection(dbUrl, user, pass);
    }

    public static void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(2)) {
                    pool.offer(conn);
                }
            } catch (SQLException e) {
                // Connection is invalid, discard it
            }
        }
    }
}