import java.sql.*;

public class SetupAdmin {
    public static void main(String[] args) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            // Add role column if missing
            try {
                conn.createStatement().execute("ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'USER'");
                System.out.println("✅ Added role column");
            } catch (SQLException e) {
                System.out.println("Role column already exists");
            }

            // Make specific user admin (change 'David' to YOUR username)
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE users SET role = 'ADMIN' WHERE username = ?"
            );
            pst.setString(1, "David");  // ← Change to your username
            int updated = pst.executeUpdate();

            if (updated > 0) {
                System.out.println("✅ Admin privileges granted to user: David");
            } else {
                System.out.println("❌ User not found. Creating new admin...");
                // Create new admin user
                pst = conn.prepareStatement(
                        "INSERT INTO users (fullname, username, email, password, role, is_active) VALUES (?, ?, ?, ?, 'ADMIN', 1)"
                );
                pst.setString(1, "Administrator");
                pst.setString(2, "admin");
                pst.setString(3, "admin@davidsavings.com");
                pst.setString(4, "admin123");
                pst.executeUpdate();
                System.out.println("✅ Created admin user: admin / admin123");
            }

            // Show all users and their roles
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT id, username, role FROM users"
            );
            System.out.println("\n📋 Current Users:");
            while (rs.next()) {
                System.out.println("   ID: " + rs.getInt("id") +
                        " | Username: " + rs.getString("username") +
                        " | Role: " + rs.getString("role"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}