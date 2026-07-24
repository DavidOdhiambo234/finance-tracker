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

    private JTextField fullnameField, usernameField, phoneField;
    private JPasswordField passwordField, confirmField;
    private JLabel errorLabel;

    public RegisterForm() {
        setTitle("Supreme Money Coach - Register");  // ← CHANGED
        setSize(450, 650);
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

        // Password
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        passLabel.setForeground(LABEL_COLOR);
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        styleTextField(passwordField);
        formPanel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        confirmLabel.setForeground(LABEL_COLOR);
        formPanel.add(confirmLabel, gbc);

        gbc.gridx = 1;
        confirmField = new JPasswordField(20);
        styleTextField(confirmField);
        formPanel.add(confirmField, gbc);

        // Error label
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Courier New", Font.PLAIN, 11));
        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(errorLabel, gbc);

        // Register Button
        gbc.gridy = 6;
        gbc.insets = new Insets(20, 10, 10, 10);
        JButton registerBtn = new JButton("CREATE ACCOUNT");
        styleButton(registerBtn, ACCENT_COLOR);
        registerBtn.addActionListener(e -> register());
        formPanel.add(registerBtn, gbc);

        // Login link
        gbc.gridy = 7;
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
    private void register() {
        String fullname = fullnameField.getText().trim();
        String username = usernameField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        // Validation
        if (fullname.isEmpty() || username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            setErrorMessage("Please fill all fields");
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

        // ✅ HASH THE PASSWORD BEFORE SAVING
        String hashedPassword = PasswordUtil.hashPassword(password);
        System.out.println("Plain password: " + password);
        System.out.println("Hashed password: " + hashedPassword);

        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO users (fullname, username, phone_number, password) VALUES (?, ?, ?, ?)")) {
            pst.setString(1, fullname);
            pst.setString(2, username);
            pst.setString(3, phone);
            pst.setString(4, hashedPassword);  // ✅ Store the HASHED password
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "✓ Account created successfully!\n\nWelcome, " + fullname + "!\nPlease login to continue.",
                    "Registration Successful",
                    JOptionPane.INFORMATION_MESSAGE);

            dispose();
            SwingUtilities.invokeLater(() -> new LoginForm());

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                setErrorMessage("Username already exists. Please choose another.");
            } else {
                setErrorMessage("Registration failed: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterForm());
    }
}