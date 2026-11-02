import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class RegisterForm extends JFrame {

    // Colors
    private static final Color BG_COLOR = new Color(10, 22, 40);
    private static final Color PANEL_COLOR = new Color(20, 35, 55);
    private static final Color BORDER_COLOR = new Color(52, 58, 64);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color LABEL_COLOR = new Color(200, 210, 220);
    private static final Color ACCENT_COLOR = new Color(0, 168, 107);
    private static final Color ERROR_COLOR = new Color(220, 53, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);

    private JTextField fullnameField, usernameField, phoneField, emailField, otpField;
    private JPasswordField passwordField, confirmField;
    private JLabel errorLabel;
    private JButton registerButton, resendOtpButton;
    private int pendingUserId = -1;
    private String pendingEmail;

    public RegisterForm() {
        setTitle("Supreme Money Coach - Register");  // ← CHANGED
        setSize(520, 760);
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

        JLabel bankName = new JLabel("SUPREME MONEY COACH");  // ← CHANGED
        bankName.setFont(new Font("Courier New", Font.BOLD, 18));
        bankName.setForeground(ACCENT_COLOR);
        bankName.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel titleLabel = new JLabel("CREATE ACCOUNT");
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
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Full Name
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        nameLabel.setForeground(LABEL_COLOR);
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        fullnameField = new JTextField(20);
        styleTextField(fullnameField);
        formPanel.add(fullnameField, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        userLabel.setForeground(LABEL_COLOR);
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(20);
        styleTextField(usernameField);
        formPanel.add(usernameField, gbc);

        // Phone Number
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel phoneLabel = new JLabel("Phone Number:");
        phoneLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        phoneLabel.setForeground(LABEL_COLOR);
        formPanel.add(phoneLabel, gbc);

        gbc.gridx = 1;
        phoneField = new JTextField(20);
        styleTextField(phoneField);
        formPanel.add(phoneField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        emailLabel.setForeground(LABEL_COLOR);
        formPanel.add(emailLabel, gbc);

        gbc.gridx = 1;
        emailField = new JTextField(20);
        styleTextField(emailField);
        formPanel.add(emailField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        passLabel.setForeground(LABEL_COLOR);
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        styleTextField(passwordField);
        formPanel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        confirmLabel.setForeground(LABEL_COLOR);
        formPanel.add(confirmLabel, gbc);

        gbc.gridx = 1;
        confirmField = new JPasswordField(20);
        styleTextField(confirmField);
        formPanel.add(confirmField, gbc);

        // Email verification code
        gbc.gridx = 0; gbc.gridy = 6;
        JLabel otpLabel = new JLabel("Email OTP:");
        otpLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        otpLabel.setForeground(LABEL_COLOR);
        formPanel.add(otpLabel, gbc);

        gbc.gridx = 1;
        JPanel otpPanel = new JPanel(new BorderLayout(8, 0));
        otpPanel.setBackground(PANEL_COLOR);
        otpField = new JTextField(10);
        styleTextField(otpField);
        otpField.setEnabled(false);
        resendOtpButton = new JButton("RESEND");
        styleButton(resendOtpButton, ACCENT_COLOR);
        resendOtpButton.setFont(new Font("Courier New", Font.BOLD, 10));
        resendOtpButton.setEnabled(false);
        resendOtpButton.addActionListener(e -> resendOTP());
        otpPanel.add(otpField, BorderLayout.CENTER);
        otpPanel.add(resendOtpButton, BorderLayout.EAST);
        formPanel.add(otpPanel, gbc);

        // Error label
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 2;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Courier New", Font.PLAIN, 11));
        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(errorLabel, gbc);

        // Register Button
        gbc.gridy = 8;
        gbc.insets = new Insets(20, 10, 10, 10);
        registerButton = new JButton("SEND VERIFICATION CODE");
        styleButton(registerButton, ACCENT_COLOR);
        registerButton.addActionListener(e -> register());
        formPanel.add(registerButton, gbc);

        // Login link
        gbc.gridy = 9;
        gbc.insets = new Insets(10, 10, 10, 10);
        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginPanel.setBackground(PANEL_COLOR);

        JLabel haveAccountLabel = new JLabel("Already have an account?");
        haveAccountLabel.setFont(new Font("Courier New", Font.PLAIN, 11));
        haveAccountLabel.setForeground(LABEL_COLOR);

        JButton loginLink = new JButton("LOGIN HERE");
        loginLink.setFont(new Font("Courier New", Font.BOLD, 11));
        loginLink.setBackground(PANEL_COLOR);
        loginLink.setForeground(ACCENT_COLOR);
        loginLink.setBorderPainted(false);
        loginLink.setFocusPainted(false);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginForm());
        });

        loginPanel.add(haveAccountLabel);
        loginPanel.add(loginLink);
        formPanel.add(loginPanel, gbc);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

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

    private void setErrorMessage(String message) {
        errorLabel.setText("⚠️ " + message);
        errorLabel.setForeground(ERROR_COLOR);
        new Timer(5000, e -> errorLabel.setText(" ")).start();
    }

    private void setInfoMessage(String message) {
        errorLabel.setText(message);
        errorLabel.setForeground(WARNING_COLOR);
    }

    private void register() {
        if (pendingUserId > 0) {
            verifyRegistrationOTP();
            return;
        }

        String fullname = fullnameField.getText().trim();
        String username = usernameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        // Validation
        if (fullname.isEmpty() || username.isEmpty() || phone.isEmpty() || email.isEmpty()
                || password.isEmpty()) {
            setErrorMessage("Please fill all fields");
            return;
        }

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            setErrorMessage("Please enter a valid email address");
            return;
        }

        if (!password.equals(confirm)) {
            setErrorMessage("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            setErrorMessage("Password must be at least 6 characters");
            return;
        }

        String hashedPassword = PasswordUtil.hashPassword(password);

        setRegistrationBusy(true);
        registerButton.setText("SENDING OTP...");
        SwingWorker<RegistrationResult, Void> worker = new SwingWorker<RegistrationResult, Void>() {
            @Override
            protected RegistrationResult doInBackground() throws Exception {
                int userId;
                try {
                    userId = createPendingUser(fullname, username, phone, email, hashedPassword);
                } catch (SQLException e) {
                    if (e.getMessage() == null || !e.getMessage().contains("Duplicate entry")) {
                        throw e;
                    }
                    userId = findMatchingPendingUser(fullname, username, phone, email, hashedPassword);
                    if (userId <= 0) {
                        throw e;
                    }
                }
                return new RegistrationResult(userId, OTPService.sendRegistrationOTP(userId, email));
            }

            @Override
            protected void done() {
                setRegistrationBusy(false);
                try {
                    RegistrationResult result = get();
                    pendingUserId = result.userId;
                    pendingEmail = email;
                    setProfileFieldsEnabled(false);
                    resendOtpButton.setEnabled(true);
                    registerButton.setText("VERIFY & CREATE ACCOUNT");
                    registerButton.setEnabled(result.otpSent);
                    otpField.setEnabled(result.otpSent);
                    if (result.otpSent) {
                        otpField.requestFocusInWindow();
                        setInfoMessage("Code sent. Check your email and enter the 6-digit OTP.");
                    } else {
                        registerButton.setText("VERIFY & CREATE ACCOUNT");
                        setErrorMessage("Account pending. Email could not be sent; use RESEND OTP.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setErrorMessage("Registration was interrupted. Please try again.");
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof SQLException && cause.getMessage() != null
                            && cause.getMessage().contains("Duplicate entry")) {
                        setErrorMessage("Username, phone number, or email is already registered.");
                    } else {
                        setErrorMessage("Registration failed: "
                                + (cause == null ? e.getMessage() : cause.getMessage()));
                    }
                    registerButton.setText("SEND VERIFICATION CODE");
                    registerButton.setEnabled(true);
                    resendOtpButton.setEnabled(false);
                    otpField.setEnabled(false);
                }
            }
        };
        worker.execute();
    }

    private int createPendingUser(String fullname, String username, String phone, String email,
                                 String hashedPassword) throws SQLException {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO users (fullname, username, phone_number, email, password, "
                             + "email_verified, is_active) VALUES (?, ?, ?, ?, ?, FALSE, FALSE)",
                     Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, fullname);
            pst.setString(2, username);
            pst.setString(3, phone);
            pst.setString(4, email);
            pst.setString(5, hashedPassword);
            pst.executeUpdate();
            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Could not retrieve the new account ID.");
                }
                return keys.getInt(1);
            }
        }
    }

    private int findMatchingPendingUser(String fullname, String username, String phone, String email,
                                        String hashedPassword) throws SQLException {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id FROM users WHERE fullname = ? AND username = ? AND phone_number = ? "
                             + "AND email = ? AND password = ? AND email_verified = FALSE "
                             + "AND is_active = FALSE LIMIT 1")) {
            pst.setString(1, fullname);
            pst.setString(2, username);
            pst.setString(3, phone);
            pst.setString(4, email);
            pst.setString(5, hashedPassword);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }

    private void resendOTP() {
        if (pendingUserId <= 0) {
            return;
        }
        setRegistrationBusy(true);
        registerButton.setEnabled(false);
        resendOtpButton.setText("SENDING...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return OTPService.sendRegistrationOTP(pendingUserId, pendingEmail);
            }

            @Override
            protected void done() {
                setRegistrationBusy(false);
                resendOtpButton.setText("RESEND");
                registerButton.setEnabled(false);
                try {
                    boolean sent = get();
                    otpField.setEnabled(sent);
                    registerButton.setEnabled(sent);
                    if (sent) {
                        setInfoMessage("A new code has been sent to your email.");
                    } else {
                        setErrorMessage("Email could not be sent. Check your Brevo configuration and retry.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setErrorMessage("Sending the code was interrupted.");
                } catch (java.util.concurrent.ExecutionException e) {
                    setErrorMessage("Could not resend the code: "
                            + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                }
                resendOtpButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    private void verifyRegistrationOTP() {
        String code = otpField.getText().trim();
        if (!code.matches("\\d{6}")) {
            setErrorMessage("Enter the 6-digit code sent to your email.");
            return;
        }
        setRegistrationBusy(true);
        registerButton.setText("VERIFYING...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return OTPService.confirmRegistrationOTP(pendingUserId, code);
            }

            @Override
            protected void done() {
                setRegistrationBusy(false);
                registerButton.setText("VERIFY & CREATE ACCOUNT");
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(RegisterForm.this,
                                "Account verified successfully!\n\nWelcome, "
                                        + fullnameField.getText().trim() + "!\nPlease login to continue.",
                                "Registration Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        SwingUtilities.invokeLater(() -> new LoginForm());
                    } else {
                        setErrorMessage("Invalid or expired code. Request a new code and try again.");
                        resendOtpButton.setEnabled(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setErrorMessage("Code verification was interrupted.");
                } catch (java.util.concurrent.ExecutionException e) {
                    setErrorMessage("Could not verify the code: "
                            + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void setRegistrationBusy(boolean busy) {
        registerButton.setEnabled(!busy);
        resendOtpButton.setEnabled(!busy && pendingUserId > 0);
    }

    private void setProfileFieldsEnabled(boolean enabled) {
        fullnameField.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmField.setEnabled(enabled);
    }

    private static class RegistrationResult {
        private final int userId;
        private final boolean otpSent;

        private RegistrationResult(int userId, boolean otpSent) {
            this.userId = userId;
            this.otpSent = otpSent;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterForm());
    }
}