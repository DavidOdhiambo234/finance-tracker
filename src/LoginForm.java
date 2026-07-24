import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoginForm extends JFrame {

    // Colors matching ATM theme
    private static final Color BG_COLOR = new Color(10, 22, 40);
    private static final Color PANEL_COLOR = new Color(20, 35, 55);
    private static final Color BORDER_COLOR = new Color(52, 58, 64);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color LABEL_COLOR = new Color(200, 210, 220);
    private static final Color ACCENT_COLOR = new Color(0, 168, 107);
    private static final Color ERROR_COLOR = new Color(220, 53, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);

    // ATM theme colors for dialogs
    private static final Color ATM_SCREEN_BG = new Color(20, 35, 55);
    private static final Color ATM_AMBER = new Color(255, 193, 7);
    private static final Color ATM_GREEN = new Color(0, 168, 107);
    private static final Color ATM_GREEN_DIM = new Color(0, 130, 85);
    private static final Color ATM_RED = new Color(220, 53, 69);
    private static final Color ATM_BORDER = new Color(52, 58, 64);
    private static final Color BUTTON_BG = new Color(52, 58, 64);
    private static final Font FONT_SMALL = new Font("Courier New", Font.PLAIN, 11);
    private static final Font FONT_BODY = new Font("Courier New", Font.PLAIN, 12);
    private static final Font FONT_LABEL = new Font("Courier New", Font.BOLD, 13);
    private static final Font FONT_BUTTON = new Font("Courier New", Font.BOLD, 11);

    // =====================================================
    //  SECURITY FEATURES - RATE LIMITING
    // =====================================================
    private static final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JButton loginBtn;
    private JLabel lockIcon;

    public LoginForm() {
        setTitle("Supreme Money Coach - Login");
        setSize(450, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel bankName = new JLabel("SUPREME MONEY COACH");
        bankName.setFont(new Font("Courier New", Font.BOLD, 18));
        bankName.setForeground(ACCENT_COLOR);
        bankName.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel titleLabel = new JLabel("LOGIN TO YOUR ACCOUNT");
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 14));
        titleLabel.setForeground(new Color(255, 193, 7));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        headerPanel.add(bankName);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLabel);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(PANEL_COLOR);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Lock icon (hidden by default)
        lockIcon = new JLabel("🔒");
        lockIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        lockIcon.setForeground(ATM_RED);
        lockIcon.setVisible(false);
        lockIcon.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(lockIcon, gbc);

        // Username
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        userLabel.setForeground(LABEL_COLOR);
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(20);
        styleTextField(usernameField);
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        passLabel.setForeground(LABEL_COLOR);
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        styleTextField(passwordField);
        formPanel.add(passwordField, gbc);

        // Forgot password link
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton forgotBtn = new JButton("Forgot Password?");
        forgotBtn.setFont(new Font("Courier New", Font.PLAIN, 11));
        forgotBtn.setBackground(PANEL_COLOR);
        forgotBtn.setForeground(ACCENT_COLOR);
        forgotBtn.setBorderPainted(false);
        forgotBtn.setFocusPainted(false);
        forgotBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotBtn.addActionListener(e -> showForgotPasswordDialog());

        JPanel forgotPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        forgotPanel.setBackground(PANEL_COLOR);
        forgotPanel.add(forgotBtn);
        formPanel.add(forgotPanel, gbc);

        // Error label
        gbc.gridy = 4;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Courier New", Font.PLAIN, 11));
        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(errorLabel, gbc);

        // Login Button
        gbc.gridy = 5;
        gbc.insets = new Insets(20, 10, 10, 10);
        loginBtn = new JButton("🔐 LOGIN");
        styleButton(loginBtn, ACCENT_COLOR);
        loginBtn.addActionListener(e -> login());
        formPanel.add(loginBtn, gbc);

        // Register link
        gbc.gridy = 6;
        gbc.insets = new Insets(10, 10, 10, 10);
        JPanel registerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        registerPanel.setBackground(PANEL_COLOR);

        JLabel noAccountLabel = new JLabel("Don't have an account?");
        noAccountLabel.setFont(new Font("Courier New", Font.PLAIN, 11));
        noAccountLabel.setForeground(LABEL_COLOR);

        JButton registerBtn = new JButton("CREATE ONE");
        registerBtn.setFont(new Font("Courier New", Font.BOLD, 11));
        registerBtn.setBackground(PANEL_COLOR);
        registerBtn.setForeground(ACCENT_COLOR);
        registerBtn.setBorderPainted(false);
        registerBtn.setFocusPainted(false);
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerBtn.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new RegisterForm());
        });

        registerPanel.add(noAccountLabel);
        registerPanel.add(registerBtn);
        formPanel.add(registerPanel, gbc);

        // Enter key to login
        usernameField.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

    // =====================================================
    //  STYLING METHODS
    // =====================================================
    private void styleTextField(JTextField field) {
        field.setFont(new Font("Courier New", Font.PLAIN, 13));
        field.setBackground(new Color(15, 30, 45));
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(ACCENT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }

    private void styleTextField(JPasswordField field) {
        field.setFont(new Font("Courier New", Font.PLAIN, 13));
        field.setBackground(new Color(15, 30, 45));
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(ACCENT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Courier New", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0, 140, 90));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("Courier New", Font.PLAIN, 14));
        field.setBackground(new Color(15, 30, 45));
        field.setForeground(ATM_GREEN);
        field.setCaretColor(ATM_GREEN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void styleField(JPasswordField field) {
        field.setFont(new Font("Courier New", Font.PLAIN, 14));
        field.setBackground(new Color(15, 30, 45));
        field.setForeground(ATM_GREEN);
        field.setCaretColor(ATM_GREEN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void setErrorMessage(String message) {
        errorLabel.setText("⚠️ " + message);
        errorLabel.setForeground(ERROR_COLOR);
        new Timer(5000, e -> errorLabel.setText(" ")).start();
    }

    // =====================================================
    //  RATE LIMITING METHODS
    // =====================================================
    private boolean isRateLimited(String username) {
        String key = username.toLowerCase().trim();
        LoginAttempt attempt = loginAttempts.get(key);

        if (attempt == null) {
            return false;
        }

        // Check if still blocked
        if (attempt.isBlocked()) {
            long remainingMinutes = attempt.getRemainingBlockMinutes();
            if (remainingMinutes > 0) {
                showLockMessage(remainingMinutes);
                return true;
            } else {
                // Block expired, reset attempts
                loginAttempts.remove(key);
                return false;
            }
        }

        return false;
    }

    private void recordFailedAttempt(String username) {
        String key = username.toLowerCase().trim();
        LoginAttempt attempt = loginAttempts.get(key);

        if (attempt == null) {
            attempt = new LoginAttempt();
            loginAttempts.put(key, attempt);
        }

        attempt.recordFailure();

        if (attempt.getAttempts() >= MAX_ATTEMPTS) {
            attempt.block();
            showLockMessage(BLOCK_DURATION_MINUTES);
        }
    }

    private void showLockMessage(long minutes) {
        lockIcon.setVisible(true);
        setErrorMessage("🔒 Account locked! Try again in " + minutes + " minutes.");
        loginBtn.setEnabled(false);

        // Re-enable after block duration
        new Timer((int)(minutes * 60 * 1000), e -> {
            lockIcon.setVisible(false);
            loginBtn.setEnabled(true);
            errorLabel.setText(" ");
        }).start();
    }

    private void resetLoginAttempts(String username) {
        String key = username.toLowerCase().trim();
        loginAttempts.remove(key);
        lockIcon.setVisible(false);
        loginBtn.setEnabled(true);
    }

    // =====================================================
    //  MAIN LOGIN METHOD
    // =====================================================
    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            setErrorMessage("Please enter both username and password");
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("⏳ LOGGING...");
        errorLabel.setText(" ");

        new Thread(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT id, username, fullname, role, is_admin, is_active, password FROM users WHERE username = ?")) {
                pst.setString(1, username);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    // ✅ Check if password is hashed or plain text
                    boolean passwordMatches = false;

                    // First, try to verify as hash
                    try {
                        passwordMatches = PasswordUtil.verifyPassword(password, storedPassword);
                        System.out.println("Hash verification: " + passwordMatches);
                    } catch (Exception e) {
                        System.out.println("Hash verification failed, checking plain text");
                    }

                    // If hash verification fails, check plain text (for old users)
                    if (!passwordMatches && password.equals(storedPassword)) {
                        passwordMatches = true;
                        System.out.println("Plain text match found!");

                        // 🔥 Update the password to hash (so next time it works)
                        updatePasswordToHash(rs.getInt("id"), password);
                    }

                    if (passwordMatches) {
                        // Check if account is active
                        if (!rs.getBoolean("is_active")) {
                            SwingUtilities.invokeLater(() -> {
                                loginBtn.setEnabled(true);
                                loginBtn.setText("🔐 LOGIN");
                                setErrorMessage("Account is deactivated. Contact admin.");
                            });
                            return;
                        }

                        // Set session variables
                        Session.setUserId(rs.getInt("id"));
                        Session.setUsername(rs.getString("username"));
                        Session.setFullname(rs.getString("fullname"));
                        Session.setRole(rs.getString("role"));
                        boolean isAdmin = rs.getBoolean("is_admin");

                        // Track login activity
                        trackLoginActivity(Session.getUserId());

                        // Close login form and open dashboard
                        SwingUtilities.invokeLater(() -> {
                            dispose();
                            if (isAdmin || "ADMIN".equalsIgnoreCase(Session.getRole())) {
                                new AdminDashboard();
                            } else {
                                new ATMDashboard();
                            }
                        });

                    } else {
                        SwingUtilities.invokeLater(() -> {
                            loginBtn.setEnabled(true);
                            loginBtn.setText("🔐 LOGIN");
                            setErrorMessage("Invalid username or password");
                            passwordField.setText("");
                            passwordField.requestFocus();
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        loginBtn.setEnabled(true);
                        loginBtn.setText("🔐 LOGIN");
                        setErrorMessage("Invalid username or password");
                    });
                }

            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("🔐 LOGIN");
                    setErrorMessage("Database error: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Helper method to update plain text password to hash
    private void updatePasswordToHash(int userId, String plainPassword) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE users SET password = ? WHERE id = ?")) {
            String hashedPassword = PasswordUtil.hashPassword(plainPassword);
            pst.setString(1, hashedPassword);
            pst.setInt(2, userId);
            pst.executeUpdate();
            System.out.println("✅ Password updated to hash for user ID: " + userId);
        } catch (SQLException e) {
            System.err.println("Failed to update password hash: " + e.getMessage());
        }
    }

    private void processLoginResult(ResultSet rs, String username) {
        try {
            if (rs.next()) {
                // Check if account is active
                if (!rs.getBoolean("is_active")) {
                    SwingUtilities.invokeLater(() -> {
                        loginBtn.setEnabled(true);
                        loginBtn.setText("🔐 LOGIN");
                        setErrorMessage("Account is deactivated. Contact admin.");
                    });
                    return;
                }

                // Reset login attempts on successful login
                resetLoginAttempts(username);

                // Create secure session
                int userId = rs.getInt("id");
                String user = rs.getString("username");
                String fullname = rs.getString("fullname");
                String role = rs.getString("role");
                boolean isAdmin = rs.getBoolean("is_admin");

                // Use the enhanced Session class
                Session.createSession(userId, user, fullname, isAdmin ? "ADMIN" : role);

                // Track login activity
                trackLoginActivity(userId);

                // Log successful login
                System.out.println("✅ User logged in: " + user + " (ID: " + userId + ") at " +
                        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));

                // Close the login form and open the appropriate dashboard
                SwingUtilities.invokeLater(() -> {
                    dispose();
                    if (isAdmin || "ADMIN".equalsIgnoreCase(role)) {
                        new AdminDashboard();
                    } else {
                        new ATMDashboard();
                    }
                });

            } else {
                // Failed login - record attempt
                recordFailedAttempt(username);

                SwingUtilities.invokeLater(() -> {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("🔐 LOGIN");
                    setErrorMessage("Invalid username or password");
                    passwordField.setText("");
                    passwordField.requestFocus();
                });
            }
        } catch (SQLException e) {
            SwingUtilities.invokeLater(() -> {
                loginBtn.setEnabled(true);
                loginBtn.setText("🔐 LOGIN");
                setErrorMessage("Error processing login: " + e.getMessage());
            });
        }
    }

    // =====================================================
    //  TRACK LOGIN ACTIVITY
    // =====================================================
    private void trackLoginActivity(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO user_activity (user_id, activity_type, activity_time) VALUES (?, 'LOGIN', UTC_TIMESTAMP())")) {
            pst.setInt(1, userId);
            pst.executeUpdate();
            System.out.println("✅ Login tracked for user ID: " + userId);
        } catch (SQLException e) {
            System.err.println("⚠️ Failed to track login: " + e.getMessage());
            // Try to create table if it doesn't exist
            createUserActivityTable();
        }
    }

    // =====================================================
    //  CREATE USER_ACTIVITY TABLE
    // =====================================================
    private void createUserActivityTable() {
        try (Connection conn = SecureDatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS user_activity (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT," +
                            "user_id INT NOT NULL," +
                            "activity_type VARCHAR(50) NOT NULL," +
                            "activity_time DATETIME DEFAULT NOW()" +
                            ")"
            );
            System.out.println("✅ user_activity table created");
        } catch (SQLException e) {
            System.err.println("❌ Failed to create user_activity table: " + e.getMessage());
        }
    }

    // =====================================================
    //  FORGOT PASSWORD DIALOG
    // =====================================================
    private void showForgotPasswordDialog() {
        JDialog resetDialog = new JDialog(this, "Reset Password", true);
        resetDialog.setSize(450, 450);  // Slightly taller
        resetDialog.setLocationRelativeTo(this);
        resetDialog.setUndecorated(true);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Title
        JLabel titleLabel = new JLabel("◈ RESET PASSWORD ◈");
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 16));
        titleLabel.setForeground(ATM_AMBER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Step label
        JLabel stepLabel = new JLabel("Enter your username to continue");
        stepLabel.setFont(FONT_BODY);
        stepLabel.setForeground(ATM_GREEN);
        stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        stepLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));

        // Username field
        JTextField usernameField = new JTextField(20);
        styleField(usernameField);
        usernameField.setMaximumSize(new Dimension(300, 40));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Error label
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FONT_SMALL);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setForeground(ATM_RED);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);

        JButton nextBtn = new JButton("CONTINUE");
        nextBtn.setFont(FONT_BUTTON);
        nextBtn.setBackground(BUTTON_BG);
        nextBtn.setForeground(ATM_GREEN);
        nextBtn.setBorder(BorderFactory.createLineBorder(ATM_GREEN, 1));
        nextBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextBtn.setPreferredSize(new Dimension(120, 40));

        // ✅ ADD CANCEL/BACK BUTTON
        JButton cancelBtn = new JButton("✖ CANCEL");
        cancelBtn.setFont(FONT_BUTTON);
        cancelBtn.setBackground(BUTTON_BG);
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setBorder(BorderFactory.createLineBorder(ATM_RED, 1));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setPreferredSize(new Dimension(120, 40));
        cancelBtn.addActionListener(e -> resetDialog.dispose());  // Close dialog

        buttonPanel.add(nextBtn);
        buttonPanel.add(cancelBtn);

        // Add components
        panel.add(titleLabel);
        panel.add(stepLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(errorLabel);
        panel.add(buttonPanel);

        // Handle "Continue" button
        nextBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                errorLabel.setText("⚠️ Please enter your username");
                errorLabel.setForeground(ATM_RED);
                return;
            }

            new Thread(() -> {
                int[] result = checkUserSecurityQuestions(username);

                SwingUtilities.invokeLater(() -> {
                    if (result[0] == -1) {
                        errorLabel.setText("❌ User not found");
                        errorLabel.setForeground(ATM_RED);
                    } else if (result[0] == -2) {
                        // ✅ Better message with option to close
                        int option = JOptionPane.showConfirmDialog(
                                resetDialog,
                                "No security questions set for this account.\n\n" +
                                        "Please contact admin to reset your password.\n\n" +
                                        "Would you like to go back to login?",
                                "No Security Questions",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        );
                        if (option == JOptionPane.YES_OPTION) {
                            resetDialog.dispose();  // Close dialog, go back to login
                        } else {
                            errorLabel.setText("❌ Please contact admin");
                            errorLabel.setForeground(ATM_RED);
                        }
                    } else {
                        // User found and has security questions
                        resetDialog.dispose();
                        showSecurityQuestionsDialog(result[0], username);
                    }
                });
            }).start();
        });

        resetDialog.add(panel);
        resetDialog.setVisible(true);
    }

    private int[] checkUserSecurityQuestions(String username) {
        int[] result = new int[2];
        result[0] = -1;
        result[1] = 0;

        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT u.id, " +
                             "sq.question1, sq.answer1 " +
                             "FROM users u " +
                             "LEFT JOIN security_questions sq ON u.id = sq.user_id " +
                             "WHERE u.username = ?")) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                result[0] = rs.getInt("id");
                String q1 = rs.getString("question1");
                if (q1 == null || q1.isEmpty()) {
                    result[1] = 0;
                } else {
                    result[1] = 1;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void showSecurityQuestionsDialog(int userId, String username) {
        // Get the single question from database
        String question1 = "";
        String answer1 = "";

        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT question1, answer1 FROM security_questions WHERE user_id = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                question1 = rs.getString("question1");
                answer1 = rs.getString("answer1");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading security question: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (question1 == null || question1.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No security question set for this account.\nPlease contact admin.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog questionDialog = new JDialog(this, "Verify Identity", true);
        questionDialog.setSize(450, 350);
        questionDialog.setLocationRelativeTo(this);
        questionDialog.setUndecorated(true);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel("◈ VERIFY YOUR IDENTITY ◈");
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 16));
        titleLabel.setForeground(ATM_AMBER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userLabel = new JLabel("User: " + username);
        userLabel.setFont(FONT_BODY);
        userLabel.setForeground(ATM_GREEN);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        // ✅ Only ONE question
        JLabel q1Label = new JLabel(question1);
        q1Label.setFont(FONT_LABEL);
        q1Label.setForeground(ATM_GREEN);
        q1Label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField a1Field = new JTextField(20);
        styleField(a1Field);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FONT_SMALL);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setForeground(ATM_RED);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);

        JButton verifyBtn = new JButton("VERIFY & RESET");
        verifyBtn.setFont(FONT_BUTTON);
        verifyBtn.setBackground(BUTTON_BG);
        verifyBtn.setForeground(ATM_GREEN);
        verifyBtn.setBorder(BorderFactory.createLineBorder(ATM_GREEN, 1));
        verifyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        verifyBtn.setPreferredSize(new Dimension(150, 40));

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(FONT_BUTTON);
        cancelBtn.setBackground(BUTTON_BG);
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setBorder(BorderFactory.createLineBorder(ATM_RED, 1));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setPreferredSize(new Dimension(150, 40));

        final String correctAnswer = answer1 != null ? answer1.toLowerCase() : "";

        verifyBtn.addActionListener(e -> {
            String ans1 = a1Field.getText().trim().toLowerCase();

            if (ans1.isEmpty()) {
                errorLabel.setText("⚠️ Please answer the question");
                errorLabel.setForeground(ATM_RED);
                return;
            }

            if (ans1.equals(correctAnswer)) {
                questionDialog.dispose();
                showNewPasswordDialog(userId, username);
            } else {
                errorLabel.setText("❌ Incorrect answer. Try again.");
                errorLabel.setForeground(ATM_RED);
                Toolkit.getDefaultToolkit().beep();
            }
        });

        cancelBtn.addActionListener(e -> questionDialog.dispose());

        buttonPanel.add(verifyBtn);
        buttonPanel.add(cancelBtn);

        panel.add(titleLabel);
        panel.add(userLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(q1Label);
        panel.add(a1Field);
        panel.add(Box.createVerticalStrut(15));
        panel.add(errorLabel);
        panel.add(buttonPanel);

        questionDialog.add(panel);
        questionDialog.setVisible(true);
    }

    private void showNewPasswordDialog(int userId, String username) {
        JDialog passwordDialog = new JDialog(this, "Reset Password", true);
        passwordDialog.setSize(450, 380);
        passwordDialog.setLocationRelativeTo(this);
        passwordDialog.setUndecorated(true);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_GREEN, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel("◈ SET NEW PASSWORD ◈");
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 16));
        titleLabel.setForeground(ATM_AMBER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userLabel = new JLabel("User: " + username);
        userLabel.setFont(FONT_BODY);
        userLabel.setForeground(ATM_GREEN);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        JLabel passLabel = new JLabel("New Password:");
        passLabel.setFont(FONT_LABEL);
        passLabel.setForeground(ATM_GREEN_DIM);
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPasswordField passField = new JPasswordField(20);
        styleField(passField);
        passField.setMaximumSize(new Dimension(300, 40));
        passField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(FONT_LABEL);
        confirmLabel.setForeground(ATM_GREEN_DIM);
        confirmLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JPasswordField confirmField = new JPasswordField(20);
        styleField(confirmField);
        confirmField.setMaximumSize(new Dimension(300, 40));
        confirmField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FONT_SMALL);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setForeground(ATM_RED);

        JButton resetBtn = new JButton("RESET PASSWORD");
        resetBtn.setFont(FONT_BUTTON);
        resetBtn.setBackground(BUTTON_BG);
        resetBtn.setForeground(ATM_GREEN);
        resetBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetBtn.setBorder(BorderFactory.createLineBorder(ATM_GREEN, 1));
        resetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetBtn.setMaximumSize(new Dimension(200, 40));
        resetBtn.setPreferredSize(new Dimension(200, 40));

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(FONT_BUTTON);
        cancelBtn.setBackground(BUTTON_BG);
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.setBorder(BorderFactory.createLineBorder(ATM_RED, 1));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setMaximumSize(new Dimension(150, 40));
        cancelBtn.setPreferredSize(new Dimension(150, 40));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);
        buttonPanel.add(resetBtn);
        buttonPanel.add(cancelBtn);

        resetBtn.addActionListener(e -> {
            String password = new String(passField.getPassword());
            String confirm = new String(confirmField.getPassword());

            if (password.isEmpty() || confirm.isEmpty()) {
                errorLabel.setText("⚠️ Please enter new password");
                errorLabel.setForeground(ATM_RED);
                return;
            }

            if (password.length() < 6) {
                errorLabel.setText("⚠️ Password must be at least 6 characters");
                errorLabel.setForeground(ATM_RED);
                return;
            }

            if (!password.equals(confirm)) {
                errorLabel.setText("⚠️ Passwords do not match");
                errorLabel.setForeground(ATM_RED);
                return;
            }

            new Thread(() -> {
                boolean success = updateUserPassword(userId, password);

                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(passwordDialog,
                                "✓ Password reset successfully!\n\nYou can now login with your new password.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        passwordDialog.dispose();
                    } else {
                        errorLabel.setText("❌ Database error. Please try again.");
                        errorLabel.setForeground(ATM_RED);
                    }
                });
            }).start();
        });

        cancelBtn.addActionListener(e -> passwordDialog.dispose());

        panel.add(titleLabel);
        panel.add(userLabel);
        panel.add(passLabel);
        panel.add(passField);
        panel.add(confirmLabel);
        panel.add(confirmField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(errorLabel);
        panel.add(buttonPanel);

        passwordDialog.add(panel);
        passwordDialog.setVisible(true);
    }

    private boolean updateUserPassword(int userId, String newPassword) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE users SET password = ? WHERE id = ?")) {
            pst.setString(1, newPassword);
            pst.setInt(2, userId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    //  INNER CLASS FOR RATE LIMITING
    // =====================================================
    private static class LoginAttempt {
        private int attempts = 0;
        private long blockUntil = 0;

        public void recordFailure() {
            attempts++;
        }

        public int getAttempts() {
            return attempts;
        }

        public void block() {
            blockUntil = System.currentTimeMillis() + (BLOCK_DURATION_MINUTES * 60 * 1000);
        }

        public boolean isBlocked() {
            return System.currentTimeMillis() < blockUntil;
        }

        public long getRemainingBlockMinutes() {
            if (!isBlocked()) return 0;
            long remainingMs = blockUntil - System.currentTimeMillis();
            return (remainingMs / 1000 / 60) + 1;
        }
    }

    // =====================================================
    //  MAIN
    // =====================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm());
    }
}