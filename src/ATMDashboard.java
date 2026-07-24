import javax.imageio.ImageIO;
// At the top with other imports
import java.util.Date;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * OPTIMIZED VERSION - Supreme Money Coach ATM Dashboard
 */
public class ATMDashboard extends JFrame {

    // ================= CONSTANTS =================
    private static final Color ATM_BG = new Color(10, 22, 40);
    private static final Color ATM_SCREEN_BG = new Color(20, 35, 55);
    private static final Color ATM_GREEN = new Color(0, 168, 107);
    private static final Color ATM_GREEN_DIM = new Color(0, 130, 85);
    private static final Color ATM_AMBER = new Color(255, 193, 7);
    private static final Color ATM_RED = new Color(220, 53, 69);
    private static final Color ATM_BLUE = new Color(13, 110, 253);
    private static final Color ATM_PURPLE = new Color(111, 66, 193);
    private static final Color ATM_CYAN = new Color(23, 162, 184);
    private static final Color ATM_BORDER = new Color(52, 58, 64);
    private static final Color BUTTON_BG = new Color(52, 58, 64);
    private static final Color BUTTON_HOVER = new Color(0, 130, 85);
    private static final Color SIDEBAR_BG = new Color(15, 25, 45);
    private static final Color SIDEBAR_HOVER = new Color(25, 40, 65);
    // At the top of ATMDashboard.java with other variables
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    // ================= FONTS =================
    private static final Font FONT_LABEL = new Font("Courier New", Font.BOLD, 13);
    private static final Font FONT_VALUE = new Font("Courier New", Font.BOLD, 22);
    private static final Font FONT_SMALL = new Font("Courier New", Font.PLAIN, 11);
    static final Font FONT_BUTTON = new Font("Courier New", Font.BOLD, 11);
    private static final Font FONT_BODY = new Font("Courier New", Font.PLAIN, 12);
    private static final Font FONT_HEAD = new Font("Courier New", Font.BOLD, 14);
    private static final Font FONT_SIDEBAR = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_BOLD = new Font("Courier New", Font.BOLD, 12);
    private static final Font FONT_MONO = new Font("Courier New", Font.BOLD, 12);

    // ================= THREAD POOL =================
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 8)
    );
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    // ================= CACHE =================
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CACHE_EXPIRY = new ConcurrentHashMap<>();
    private static final int CACHE_TTL_MS = 30000;

    // ================= COMPONENTS =================
    private JLabel balanceValueLabel, goalValueLabel, remainingValueLabel;
    private JProgressBar goalBar;
    private JTextArea miniStatementArea;
    private JLabel statusLabel, clockLabel, bankTitleLabel;
    private JLabel balTitleLbl, goalTitleLbl, remTitleLbl, progressTitleLbl, miniStatTitleLbl;
    private JButton withdrawBtn, customBtn, statementBtn, aiKpBtn, chamaKpBtn, chatBtn;
    private JButton profileBtn, refreshBtn, exitBtn, langBtn, chamaMainBtn, submitVideoBtn;
    private JPanel sidebar, contentArea;
    private CardLayout cardLayout;
    private boolean sidebarVisible = true;
    private JButton toggleSidebarBtn;
    private boolean isKiswahili = false;

    // ================= CARD CONSTANTS =================
    private static final String CARD_SPLASH = "SPLASH";
    private static final String CARD_MAIN = "MAIN";
    private static final String CARD_STATEMENT = "STATEMENT";
    private static final String CARD_AI = "AI";
    private static final String CARD_CHAMA = "CHAMA";
    private static final String CARD_CHAT = "CHAT";
    private static final String CARD_PROFILE = "PROFILE";
    private static final String CARD_NOTIFICATIONS = "NOTIFICATIONS";
    private static final String CARD_DEBT = "DEBT";
    private static final String CARD_REQUEST = "REQUEST";
    private static final String CARD_CHAMA_MANAGEMENT = "CHAMA_MANAGEMENT";

    private JTextArea fullStatementArea, aiInsightsArea, chamaResultArea, chatArea;
    private JTextField chatInput, monthlyIncomeField, monthlyExpensesField;
    private JButton chatSendBtn, aiGenerateBtn;
    private JComboBox<String> occupationCombo, goalTypeCombo;
    private JLabel notificationBellBtn, notificationBadge;
    private Map<Integer, Map<String, Object>> chamaMap = new ConcurrentHashMap<>();

    // ================= CONSTANTS =================
    private static final int SAVINGS_GOAL = 15000;
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String GEMINI_API_KEY = ConfigLoader.getProperty("gemini.api.key");
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

    // ================= SOUND =================
    private void playTone(int frequency, int durationMs, float volume) {
        EXECUTOR.submit(() -> {
            try {
                int sampleRate = 44100;
                int samples = sampleRate * durationMs / 1000;
                byte[] buf = new byte[samples];
                for (int i = 0; i < samples; i++) {
                    double angle = 2.0 * Math.PI * i * frequency / sampleRate;
                    double fade = Math.min(1.0, Math.min(i, samples - i) / (sampleRate * 0.01));
                    buf[i] = (byte) (Math.sin(angle) * 127 * volume * fade);
                }
                AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                line.write(buf, 0, buf.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {}
        });
    }

    private void soundBeep() { playTone(880, 80, 0.6f); }
    private void soundSuccess() { playTone(1046, 120, 0.5f); EXECUTOR.submit(() -> { sleep(130); playTone(1318, 150, 0.5f); }); }
    private void soundError() { playTone(220, 200, 0.7f); }
    private void sleep(int ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
    private void soundChama() {
        EXECUTOR.submit(() -> {
            int[] notes = {523, 659, 784, 1046};
            for (int n : notes) { playTone(n, 100, 0.5f); sleep(110); }
        });
    }
    private void soundExit() {
        EXECUTOR.submit(() -> {
            int[] notes = {1046, 880, 698, 587, 440, 349, 220, 110};
            for (int n : notes) { playTone(n, 100, 0.5f); sleep(90); }
            playTone(80, 400, 0.6f);
        });
    }

    // ================= HELPER METHODS (FIXED) =================
    private void styleTextField(JTextField field) {
        field.setFont(FONT_SMALL);
        field.setBackground(new Color(15, 30, 45));  // Dark background
        field.setForeground(ATM_GREEN);              // Green text - VISIBLE!
        field.setCaretColor(ATM_GREEN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(new Color(15, 30, 45));
        combo.setForeground(ATM_GREEN);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    setBackground(new Color(0, 130, 85));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(new Color(15, 30, 45));
                    setForeground(ATM_GREEN);
                }
                return this;
            }
        });
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    }
    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(FONT_SMALL);
        combo.setBackground(new Color(15, 30, 45));  // Dark background
        combo.setForeground(ATM_GREEN);              // Green text - VISIBLE!
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(new Color(15, 30, 45));
                setForeground(ATM_GREEN);
                return this;
            }
        });
    }

    private void styleFormField(JTextField field) {
        field.setFont(new Font("Courier New", Font.PLAIN, 13));
        field.setBackground(new Color(15, 30, 45));
        field.setForeground(ATM_GREEN);
        field.setCaretColor(ATM_GREEN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
    }

    // ================= GEMINI API =================
    private  String callGeminiAPI(String prompt) {
        String cacheKey = prompt.hashCode() + "_" + isKiswahili;
        if (isCacheValid(cacheKey)) {
            return (String) CACHE.get(cacheKey);
        }

        try {
            URL url = URI.create(GEMINI_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);

            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", prompt));
            contentObj.put("parts", parts);
            contents.put(contentObj);
            requestBody.put("contents", contents);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                return getOfflineResponse(prompt);
            }

            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder rb = new StringBuilder();
                while (scanner.hasNextLine()) rb.append(scanner.nextLine());
                String response = new JSONObject(rb.toString())
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .replace("**", "").replace("##", "")
                        .replace("*", "-").replace("#", "");

                cacheResult(cacheKey, response);
                return response;
            }
        } catch (Exception ex) {
            return getOfflineResponse(prompt);
        }
    }

    private boolean isCacheValid(String key) {
        Long expiry = CACHE_EXPIRY.get(key);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    private void cacheResult(String key, Object value) {
        CACHE.put(key, value);
        CACHE_EXPIRY.put(key, System.currentTimeMillis() + CACHE_TTL_MS);
    }

    private String getOfflineResponse(String prompt) {
        boolean isSwahili = prompt.contains("KISWAHILI") || prompt.toLowerCase().contains("kiswahili");
        if (prompt.toLowerCase().contains("question:")) {
            String question = prompt.substring(prompt.toLowerCase().indexOf("question:") + 9).trim();
            return isSwahili ? getSwahiliOfflineResponse(question) : getEnglishOfflineResponse(question);
        }
        return isSwahili ?
                "📡 Samahani, huduma ya AI ina wateja wengi kwa sasa. Tafadhali jaribu tena baada ya dakika chache." :
                "📡 The AI service is currently experiencing high demand. Please try again in a few minutes.";
    }

    private String getEnglishOfflineResponse(String question) {
        question = question.toLowerCase();
        if (question.contains("save") || question.contains("saving")) {
            return "💡 SMART SAVING TIPS:\n\n1. Save at least 10% of every income\n2. Use the 50/30/20 rule\n3. Set up automatic transfers\n4. Track every expense\n5. Join a Chama for motivation";
        }
        if (question.contains("debt") || question.contains("owe")) {
            return "💡 DEBT MANAGEMENT TIPS:\n\n1. List all debts with interest rates\n2. Use the avalanche method\n3. Pay more than minimum\n4. Track money owed to you\n5. Set reminders for due dates";
        }
        return "💡 QUICK FINANCIAL TIPS:\n\n• Save before you spend\n• Track every shilling\n• Use the AI Coach\n• Share goals with family\n• Review progress weekly";
    }

    private String getSwahiliOfflineResponse(String question) {
        question = question.toLowerCase();
        if (question.contains("aka") || question.contains("weka") || question.contains("hifadhi")) {
            return "💡 VIDOKEZO VYA KUWEKA AKIBA:\n\n1. Weka angalau 10% ya mapato\n2. Tumia kanuni ya 50/30/20\n3. Weka uhamisho wa kiotomatiki\n4. Rekodi kila matumizi\n5. Jiunge na Chama";
        }
        return "💡 VIDOKEZO VYA KIFEDHA:\n\n• Weka akiba kabla ya matumizi\n• Rekodi kila shilingi\n• Tumia AI Coach\n• Shiriki malengo na familia\n• Kagua maendeleo kila wiki";
    }

    // ================= CONSTRUCTOR =================
    // ================= CONSTRUCTOR =================
    public ATMDashboard() {

        if (Session.getUsername() == null || Session.getUsername().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Please login first!", "Session Error", JOptionPane.ERROR_MESSAGE);
                dispose();
                new LoginForm();
            });
            return;
        }

        initUI();
        setVisible(true);
        playCardInsertAnimation();
    }

    // ================= INIT UI =================
    private void initUI() {
        setTitle("Supreme Money Coach - AI Financial Platform");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { exitWithAnimation(); }
        });

        // Get screen size for responsive layout
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1280, screenSize.width - 20);
        int height = Math.min(800, screenSize.height - 40);

        setSize(width, height);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setResizable(true);

        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerSize(0);
        splitPane.setBorder(null);

        sidebar = buildSidebar();
        int sidebarWidth = isSmallScreen() ? 180 : 220;
        sidebar.setPreferredSize(new Dimension(sidebarWidth, 0));

        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(ATM_SCREEN_BG);

        // Add all card panels
        contentArea.add(buildSplashScreen(), CARD_SPLASH);
        contentArea.add(buildMainScreen(), CARD_MAIN);
        contentArea.add(buildStatementScreen(), CARD_STATEMENT);
        contentArea.add(buildAIScreen(), CARD_AI);
        contentArea.add(buildChamaScreen(), CARD_CHAMA);
        contentArea.add(buildChatScreen(), CARD_CHAT);
        contentArea.add(buildProfileScreen(), CARD_PROFILE);
        contentArea.add(buildNotificationsScreen(), CARD_NOTIFICATIONS);
        contentArea.add(buildDebtScreen(), CARD_DEBT);
        contentArea.add(buildRequestScreen(), CARD_REQUEST);
        contentArea.add(buildChamaManagementScreen(), CARD_CHAMA_MANAGEMENT);

        splitPane.setLeftComponent(sidebar);
        splitPane.setRightComponent(contentArea);
        add(splitPane, BorderLayout.CENTER);
        add(buildKeypad(), BorderLayout.SOUTH);
    }

    // ================= SMALL SCREEN DETECTION =================
    private boolean isSmallScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return screenSize.height < 800 || screenSize.width < 1200;
    }
    private void showCreditScoreDialog() {
        // Show loading dialog
        JDialog loadingDialog = new JDialog(this, "Calculating...", true);
        loadingDialog.setSize(300, 150);
        loadingDialog.setLocationRelativeTo(this);
        JLabel loadingLabel = new JLabel("📊 Calculating your credit score...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loadingLabel.setForeground(ATM_GREEN);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel loadPanel = new JPanel(new BorderLayout(10, 10));
        loadPanel.setBackground(ATM_SCREEN_BG);
        loadPanel.add(loadingLabel, BorderLayout.CENTER);
        loadPanel.add(progressBar, BorderLayout.SOUTH);
        loadingDialog.add(loadPanel);

        executor.submit(() -> {
            // Calculate credit score
            CreditScore score = CreditScoreCalculator.calculate(Session.getUserId());

            SwingUtilities.invokeLater(() -> {
                loadingDialog.dispose();
                showCreditScoreResult(score);
            });
        });

        loadingDialog.setVisible(true);
    }

    private void showCreditScoreResult(CreditScore score) {
        JDialog dialog = new JDialog(this, "📊 Your Credit Score", true);
        dialog.setSize(650, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ATM_SCREEN_BG);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(20, 25, 25, 25)));

        // ========== HEADER ==========
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ATM_SCREEN_BG);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel titleLabel = new JLabel("📊 YOUR CREDIT SCORE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(ATM_AMBER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel dateLabel = new JLabel("Calculated: " + new Date(), SwingConstants.CENTER);
        dateLabel.setFont(FONT_SMALL);
        dateLabel.setForeground(ATM_GREEN_DIM);
        headerPanel.add(dateLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // ========== SCORE DISPLAY ==========
        JPanel scorePanel = new JPanel(new GridBagLayout());
        scorePanel.setBackground(new Color(15, 30, 50));
        scorePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        Color scoreColor = getScoreColor(score.getTotalScore());
        scorePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(scoreColor, 3),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel scoreLabel = new JLabel(String.valueOf(score.getTotalScore()));
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        scoreLabel.setForeground(scoreColor);
        scorePanel.add(scoreLabel, gbc);

        gbc.gridy = 1;
        JLabel ratingLabel = new JLabel(score.getRating());
        ratingLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        ratingLabel.setForeground(scoreColor);
        scorePanel.add(ratingLabel, gbc);

        gbc.gridy = 2;
        JLabel loanLabel = new JLabel("💰 Loan Eligibility: Ksh " + String.format("%,.0f", score.getLoanLimit()));
        loanLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loanLabel.setForeground(ATM_GREEN);
        scorePanel.add(loanLabel, gbc);

        mainPanel.add(scorePanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // ========== AI INSIGHTS ==========
        JLabel aiTitle = new JLabel("🤖 AI FINANCIAL INSIGHTS");
        aiTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        aiTitle.setForeground(ATM_PURPLE);
        aiTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(aiTitle);
        mainPanel.add(Box.createVerticalStrut(5));

        JPanel aiPanel = new JPanel(new BorderLayout());
        aiPanel.setBackground(new Color(20, 35, 55));
        aiPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_PURPLE, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        aiPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JTextArea aiArea = new JTextArea();
        aiArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        aiArea.setBackground(new Color(20, 35, 55));
        aiArea.setForeground(ATM_GREEN);
        aiArea.setEditable(false);
        aiArea.setLineWrap(true);
        aiArea.setWrapStyleWord(true);
        aiArea.setText("🤖 AI is analyzing your financial behavior...\n\nThis may take a few seconds.");

        // Load AI insights in background
        executor.submit(() -> {
            String aiInsight = getAIAnalysis(score);
            SwingUtilities.invokeLater(() -> {
                aiArea.setText(aiInsight);
            });
        });

        aiPanel.add(new JScrollPane(aiArea), BorderLayout.CENTER);
        mainPanel.add(aiPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // ========== DETAILED BREAKDOWN ==========
        JLabel breakdownTitle = new JLabel("📋 HOW YOUR SCORE WAS CALCULATED");
        breakdownTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        breakdownTitle.setForeground(ATM_AMBER);
        breakdownTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(breakdownTitle);
        mainPanel.add(Box.createVerticalStrut(10));

        // Create breakdown rows
        JPanel breakdownPanel = new JPanel();
        breakdownPanel.setLayout(new BoxLayout(breakdownPanel, BoxLayout.Y_AXIS));
        breakdownPanel.setBackground(ATM_SCREEN_BG);

        breakdownPanel.add(createBreakdownRow(
                "💰 Savings History",
                "Based on how much you save and consistency",
                score.getSavingsScore(), 300,
                "You have saved Ksh " + String.format("%,.0f", getTotalSavings(Session.getUserId())) + ". " +
                        "Need Ksh 2,000+ to start earning points. " +
                        "4+ weeks of saving gives consistency points."
        ));
        breakdownPanel.add(Box.createVerticalStrut(5));

        breakdownPanel.add(createBreakdownRow(
                "📊 Income vs Expenses",
                "Do you save money or spend everything?",
                score.getExpenseScore(), 250,
                "You save at least 10% of your income. " +
                        "Higher savings rate = more points!"
        ));
        breakdownPanel.add(Box.createVerticalStrut(5));

        breakdownPanel.add(createBreakdownRow(
                "👥 Chama Participation",
                "Are you part of a community savings group?",
                score.getChamaScore(), 150,
                "Chama shows you're responsible with group money. " +
                        "2+ contributions needed to start earning points."
        ));
        breakdownPanel.add(Box.createVerticalStrut(5));

        breakdownPanel.add(createBreakdownRow(
                "💳 Debt Repayment",
                "Do you pay back what you owe?",
                score.getDebtScore(), 150,
                "Paying debts on time shows responsibility. " +
                        "No debts = neutral (not good, not bad)."
        ));
        breakdownPanel.add(Box.createVerticalStrut(5));

        breakdownPanel.add(createBreakdownRow(
                "📱 App Activity",
                "How often do you use the app?",
                score.getActivityScore(), 150,
                "Active users get more points. " +
                        "5+ activities per month starts earning points."
        ));

        // Wrap breakdown in scroll pane
        JScrollPane breakdownScroll = new JScrollPane(breakdownPanel);
        breakdownScroll.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        breakdownScroll.getViewport().setBackground(ATM_SCREEN_BG);
        breakdownScroll.setPreferredSize(new Dimension(550, 250));
        breakdownScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        mainPanel.add(breakdownScroll);
        mainPanel.add(Box.createVerticalStrut(15));

        // ========== CLOSE BUTTON ==========
        JButton closeBtn = new JButton("✖ CLOSE");
        closeBtn.setFont(FONT_BUTTON);
        closeBtn.setBackground(BUTTON_BG);
        closeBtn.setForeground(ATM_RED);
        closeBtn.setBorder(BorderFactory.createLineBorder(ATM_RED, 1));
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.setMaximumSize(new Dimension(150, 40));
        closeBtn.addActionListener(e -> dialog.dispose());

        mainPanel.add(closeBtn);

        // Wrap everything in a scroll pane
        JScrollPane mainScroll = new JScrollPane(mainPanel);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(ATM_SCREEN_BG);
        mainScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        dialog.add(mainScroll);
        dialog.setVisible(true);
    }
    private String getAIAnalysis(CreditScore score) {
        String prompt = String.format(
                "You are a financial advisor AI. Analyze this user's credit data:\n\n" +
                        "Credit Score: %d/1000 (%s)\n" +
                        "Savings Score: %.0f/300\n" +
                        "Expense Score: %.0f/250\n" +
                        "Chama Score: %.0f/150\n" +
                        "Debt Score: %.0f/150\n" +
                        "Activity Score: %.0f/150\n" +
                        "Loan Limit: Ksh %.0f\n\n" +
                        "Provide a brief analysis with these 3 sections (keep it short):\n" +
                        "1. ASSESSMENT: 2-3 sentences honestly evaluating their financial behavior\n" +
                        "2. RECOMMENDATIONS: 3 specific, actionable tips to improve their score\n" +
                        "3. MOTIVATION: One encouraging sentence\n\n" +
                        "Use clear, simple language. Be honest but encouraging.",
                score.getTotalScore(), score.getRating(),
                score.getSavingsScore(), score.getExpenseScore(),
                score.getChamaScore(), score.getDebtScore(),
                score.getActivityScore(),
                score.getLoanLimit()
        );

        try {
            String aiResponse = callGeminiAPI(prompt);
            return "🤖 AI Financial Analysis:\n\n" + aiResponse;
        } catch (Exception e) {
            // Fallback to offline analysis
            return getOfflineAIAnalysis(score);
        }
    }
    private String getOfflineAIAnalysis(CreditScore score) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 YOUR FINANCIAL SUMMARY\n\n");

        // Savings
        sb.append("💰 SAVINGS: ");
        if (score.getSavingsScore() < 100) {
            sb.append("You're building your savings habit. Start small and stay consistent. ");
        } else if (score.getSavingsScore() < 200) {
            sb.append("Good progress! You're saving regularly. Keep increasing your amounts. ");
        } else {
            sb.append("Excellent savings discipline! You're on track to reach your goals. ");
        }
        sb.append("\n\n");

        // Chama
        sb.append("👥 CHAMA: ");
        if (score.getChamaScore() < 50) {
            sb.append("Join a Chama to build community savings and boost your score! ");
        } else if (score.getChamaScore() < 100) {
            sb.append("You're in a Chama! Stay consistent with contributions. ");
        } else {
            sb.append("Excellent Chama participation! You're building strong financial relationships. ");
        }
        sb.append("\n\n");

        // Debt
        sb.append("💳 DEBT: ");
        if (score.getDebtScore() < 50) {
            sb.append("Track your debts to show responsibility. ");
        } else if (score.getDebtScore() < 100) {
            sb.append("You're paying your debts. Try to clear them faster. ");
        } else {
            sb.append("Great debt management! You're reliable with repayments. ");
        }
        sb.append("\n\n");

        // Recommendations
        sb.append("📈 RECOMMENDATIONS:\n");
        sb.append("1. Save at least 10% of every income\n");
        sb.append("2. Join or stay active in a Chama\n");
        sb.append("3. Track all your expenses daily\n");
        sb.append("4. Pay debts early when possible\n");
        sb.append("5. Use the app regularly for better insights\n\n");

        sb.append("💡 Your score is " + score.getTotalScore() + "/1000 (" + score.getRating() + "). ");
        sb.append("With consistent effort, you can reach the next level in 2-3 months!");

        return sb.toString();
    }

    private JPanel createBreakdownRow(String title, String subtitle, double score, double maxScore, String explanation) {
        JPanel row = new JPanel(new BorderLayout(10, 5));
        row.setBackground(new Color(20, 35, 55));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ATM_BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        // Left side: Title and subtitle
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(20, 35, 55));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(ATM_GREEN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        subLabel.setForeground(ATM_GREEN_DIM);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subLabel);

        row.add(leftPanel, BorderLayout.CENTER);

        // Right side: Score
        int percentage = (int)((score / maxScore) * 100);
        String percentageText = percentage + "%";

        JLabel scoreLabel = new JLabel(percentageText);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        scoreLabel.setForeground(getScoreColor(percentage * 10));
        scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        scoreLabel.setPreferredSize(new Dimension(60, 30));
        row.add(scoreLabel, BorderLayout.EAST);

        // Tooltip with explanation
        row.setToolTipText(explanation);

        return row;
    }
    private double getTotalSavings(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) as total_savings " +
                            "FROM mysaving2 WHERE user_id = ? AND amount > 0");
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total_savings");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    private String getAISavingsInsight(int userId, double savingsScore) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            // Get detailed savings data
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total_deposits, " +
                            "COALESCE(SUM(amount), 0) as total_savings, " +
                            "COUNT(DISTINCT weekNO) as weeks_active, " +
                            "AVG(amount) as avg_deposit " +
                            "FROM mysaving2 WHERE user_id = ? AND amount > 0");
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int deposits = rs.getInt("total_deposits");
                double total = rs.getDouble("total_savings");
                int weeks = rs.getInt("weeks_active");
                double avg = rs.getDouble("avg_deposit");

                // Build prompt for AI
                String prompt = String.format(
                        "Analyze this user's savings behavior:\n" +
                                "Total saved: Ksh %.2f\n" +
                                "Number of deposits: %d\n" +
                                "Weeks saving: %d\n" +
                                "Average deposit: Ksh %.2f\n" +
                                "Current savings score: %.0f%%\n\n" +
                                "Provide a brief, honest assessment (2-3 sentences) about their savings habits.",
                        total, deposits, weeks, avg, (savingsScore/300)*100
                );

                return callGeminiAPI(prompt);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Keep saving consistently to build your score.";
    }

    private void addBreakdownRow(JPanel panel, String label, double score, double maxScore) {
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(FONT_SMALL);
        labelComponent.setForeground(ATM_GREEN);
        panel.add(labelComponent);

        int percentage = (int)((score / maxScore) * 100);
        JLabel valueLabel = new JLabel(percentage + "%");
        valueLabel.setFont(FONT_SMALL);
        valueLabel.setForeground(getScoreColor(percentage * 10));
        panel.add(valueLabel);
    }

    private Color getScoreColor(int score) {
        if (score >= 800) return new Color(0, 168, 107);  // Green
        if (score >= 600) return new Color(255, 193, 7);   // Amber
        if (score >= 400) return new Color(255, 140, 0);   // Orange
        return new Color(220, 53, 69);                     // Red
    }

    // ================= TOP BAR =================
    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(5, 5, 5));
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        topBar.setPreferredSize(new Dimension(0, 55));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(new Color(5, 5, 5));

        JLabel logoLabel = new JLabel("◈");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logoLabel.setForeground(ATM_AMBER);

        bankTitleLabel = new JLabel("SUPREME MONEY COACH");
        bankTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        bankTitleLabel.setForeground(ATM_GREEN);

        leftPanel.add(logoLabel);
        leftPanel.add(bankTitleLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(new Color(5, 5, 5));

        clockLabel = new JLabel("", SwingConstants.RIGHT);
        clockLabel.setFont(FONT_SMALL);
        clockLabel.setForeground(ATM_GREEN_DIM);

        notificationBellBtn = new JLabel("🔔");
        notificationBellBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        notificationBellBtn.setForeground(ATM_AMBER);
        notificationBellBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        notificationBellBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                soundBeep();
                CardLayout cl = (CardLayout) contentArea.getLayout();
                cl.show(contentArea, CARD_NOTIFICATIONS);
                NotificationService.markAllRead(Session.userId);
                updateNotificationBadge();
            }
        });

        notificationBadge = new JLabel("0");
        notificationBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
        notificationBadge.setForeground(Color.WHITE);
        notificationBadge.setOpaque(true);
        notificationBadge.setBackground(ATM_RED);
        notificationBadge.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        notificationBadge.setVisible(false);

        JLabel userLabel = new JLabel("👤 " + Session.username.toUpperCase());
        userLabel.setFont(FONT_SMALL);
        userLabel.setForeground(ATM_GREEN_DIM);

        rightPanel.add(notificationBellBtn);
        rightPanel.add(notificationBadge);
        rightPanel.add(clockLabel);
        rightPanel.add(userLabel);

        topBar.add(leftPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);
        return topBar;
    }

    // ================= SIDEBAR =================
    // ================= FIXED SIDEBAR BUILD METHOD =================
    private JPanel buildSidebar() {
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BorderLayout());
        sidebarPanel.setBackground(SIDEBAR_BG);
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ATM_BORDER));

        // ========== TOP: Toggle Button ==========
        toggleSidebarBtn = new JButton("☰ MENU");
        toggleSidebarBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toggleSidebarBtn.setBackground(new Color(25, 35, 55));
        toggleSidebarBtn.setForeground(ATM_AMBER);
        toggleSidebarBtn.setFocusPainted(false);
        toggleSidebarBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        toggleSidebarBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleSidebarBtn.addActionListener(e -> toggleSidebar());
        sidebarPanel.add(toggleSidebarBtn, BorderLayout.NORTH);

        // ========== CENTER: Scrollable Menu ==========
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(SIDEBAR_BG);

        // Detect small screen
        boolean isSmall = isSmallScreen();
        int fontSize = isSmall ? 11 : 12;
        int padding = isSmall ? 8 : 12;

        // Create all menu buttons with appropriate sizing
        JButton aiBtn = createSidebarButton("◆ AI", CARD_AI, fontSize, padding);
        JButton chamaBtn = createSidebarButton("◈ CHAMA", "CHAMA_MENU", fontSize, padding);
        JButton chatBtn = createSidebarButton("◉ CHAT", CARD_CHAT, fontSize, padding);
        JButton profileBtn = createSidebarButton("○ PROFILE", CARD_PROFILE, fontSize, padding);
        JButton debtBtn = createSidebarButton("◊ DEBT", CARD_DEBT, fontSize, padding);
        JButton requestBtn = createSidebarButton("▣ REQUEST", CARD_REQUEST, fontSize, padding);
        JButton videosBtn = createSidebarButton("▶ VIDEOS", "VIDEOS", fontSize, padding);
        JButton expenseTrackerBtn = createSidebarButton("📊 EXPENSES", "EXPENSES", fontSize, padding);
        JButton exportSpendingBtn = createSidebarButton("📄 EXPORT SPENDING", "EXPORT_SPENDING", fontSize, padding);
        JButton refreshBtn = createSidebarButton("↻ REFRESH", "REFRESH", fontSize, padding);
        JButton exitBtn = createSidebarButton("✕ EXIT", "EXIT", fontSize, padding);

        // ✅ FIX: Create credit score button and add to menuPanel (NOT sidebarPanel)
        JButton creditScoreBtn = createSidebarButton("📊 CREDIT SCORE", "CREDIT_SCORE", fontSize, padding);
        creditScoreBtn.addActionListener(e -> showCreditScoreDialog());

        // ✅ Add all buttons to menuPanel (including creditScoreBtn)
        menuPanel.add(aiBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(chamaBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(chatBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(profileBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(debtBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(requestBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(videosBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(expenseTrackerBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(exportSpendingBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(refreshBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(exitBtn);
        menuPanel.add(Box.createVerticalStrut(1));
        menuPanel.add(creditScoreBtn);  // ✅ Add credit score button here!

        // Wrap menu in scroll pane
        JScrollPane menuScroll = new JScrollPane(menuPanel);
        menuScroll.setBorder(null);
        menuScroll.getViewport().setBackground(SIDEBAR_BG);
        menuScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        menuScroll.getVerticalScrollBar().setUnitIncrement(10);
        menuScroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));

        sidebarPanel.add(menuScroll, BorderLayout.CENTER);

        // ========== BOTTOM: Language Button ==========
        langBtn = new JButton("◙ KISWAHILI");
        langBtn.setFont(new Font("Segoe UI", Font.PLAIN, isSmall ? 10 : 11));
        langBtn.setBackground(SIDEBAR_BG);
        langBtn.setForeground(ATM_CYAN);
        langBtn.setFocusPainted(false);
        langBtn.setBorder(BorderFactory.createEmptyBorder(padding, 15, padding, 15));
        langBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        langBtn.addActionListener(e -> toggleLanguage());
        sidebarPanel.add(langBtn, BorderLayout.SOUTH);

        return sidebarPanel;
    }
    private void trackActivity(String activityType) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO user_activity (user_id, activity_type, activity_time) VALUES (?, ?, CONVERT_TZ(NOW(), '+00:00', '+03:00'))")) {
            pst.setInt(1, Session.getUserId());
            pst.setString(2, activityType);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to track activity: " + e.getMessage());
        }
    }

    // ================= ENHANCED SIDEBAR BUTTON =================
    private JButton createSidebarButton(String text, String targetCard, int fontSize, int padding) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        btn.setBackground(SIDEBAR_BG);
        btn.setForeground(ATM_GREEN_DIM);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(padding, 15, padding, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(SIDEBAR_HOVER);
                btn.setForeground(ATM_AMBER);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(SIDEBAR_BG);
                btn.setForeground(ATM_GREEN_DIM);
            }
        });

        btn.addActionListener(e -> {
            soundBeep();
            switch (targetCard) {
                case "REFRESH": refreshAll(); break;
                case "EXIT": exitWithAnimation(); break;
                case "CHAMA_MENU": showSmartChamaDialog(); break;
                case "VIDEOS": showVideoPlayer(); break;
                case "EXPENSES": showExpenseDashboard(); break;
                case "EXPORT_SPENDING": exportSpendingReport(); break;
                default:
                    CardLayout cl = (CardLayout) contentArea.getLayout();
                    cl.show(contentArea, targetCard);
            }
        });

        return btn;
    }

    // ================= EXPENSE DASHBOARD METHOD =================
    private void showExpenseDashboard() {
        JDialog dashboard = new JDialog(this, "Expense Tracker", true);
        dashboard.setSize(900, 700);
        dashboard.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(ATM_SCREEN_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(ATM_SCREEN_BG);
        tabs.setForeground(ATM_GREEN);
        tabs.setFont(FONT_BODY);

        tabs.addTab("📊 Summary", createExpenseSummaryPanel());
        tabs.addTab("📈 By Category", createCategoryBreakdownPanel());
        tabs.addTab("📋 History", createExpenseHistoryPanel());
        tabs.addTab("🤖 AI Insights", createExpenseInsightsPanel());
        trackActivity("VIEW_EXPENSES");

        mainPanel.add(tabs, BorderLayout.CENTER);
        dashboard.add(mainPanel);
        dashboard.setVisible(true);
    }

    // ================= EXPENSE SUMMARY PANEL =================
    private JPanel createExpenseSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextArea summaryArea = new JTextArea();
        summaryArea.setFont(FONT_MONO);
        summaryArea.setBackground(new Color(15, 30, 45));
        summaryArea.setForeground(ATM_GREEN);
        summaryArea.setEditable(false);

        EXECUTOR.submit(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("╔═══════════════════════════════════════════════════════════╗\n");
            sb.append("║                   EXPENSE SUMMARY                          ║\n");
            sb.append("╚═══════════════════════════════════════════════════════════╝\n\n");

            try (Connection conn = SecureDatabaseConnection.connect()) {
                // Total income, expenses, net
                PreparedStatement pst = conn.prepareStatement(
                        "SELECT " +
                                "COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) as income, " +
                                "COALESCE(SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END), 0) as expenses, " +
                                "COUNT(CASE WHEN amount < 0 THEN 1 END) as expense_count " +
                                "FROM mysaving2 WHERE user_id = ?");
                pst.setInt(1, Session.getUserId());
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    double income = rs.getDouble("income");
                    double expenses = rs.getDouble("expenses");
                    int count = rs.getInt("expense_count");
                    double net = income - expenses;
                    double savingsRate = income > 0 ? ((income - expenses) / income) * 100 : 0;

                    sb.append(String.format("  💰 Total Income     : Ksh %,10.2f\n", income));
                    sb.append(String.format("  💸 Total Expenses   : Ksh %,10.2f\n", expenses));
                    sb.append(String.format("  📊 Net Savings      : Ksh %,10.2f\n", net));
                    sb.append(String.format("  📈 Savings Rate     : %10.1f%%\n", Math.max(savingsRate, 0)));
                    sb.append(String.format("  📝 Total Expenses   : %10d\n", count));
                    sb.append("\n  ──────────────────────────────────────────────\n");
                }

                // Top expense categories
                pst = conn.prepareStatement(
                        "SELECT category, COUNT(*) as count, SUM(ABS(amount)) as total " +
                                "FROM mysaving2 " +
                                "WHERE user_id = ? AND amount < 0 " +
                                "GROUP BY category " +
                                "ORDER BY total DESC LIMIT 5");
                pst.setInt(1, Session.getUserId());
                rs = pst.executeQuery();

                sb.append("\n  🏷️  TOP EXPENSE CATEGORIES\n");
                sb.append("  ──────────────────────────────────────────────\n");
                int rank = 1;
                while (rs.next()) {
                    sb.append(String.format("  %d. %-20s Ksh %,10.2f (%d items)\n",
                            rank++, rs.getString("category"), rs.getDouble("total"), rs.getInt("count")));
                }

                // Recent expenses with reasons
                pst = conn.prepareStatement(
                        "SELECT amount, category, reason, dateOfPayment " +
                                "FROM mysaving2 " +
                                "WHERE user_id = ? AND amount < 0 " +
                                "ORDER BY dateOfPayment DESC LIMIT 10");
                pst.setInt(1, Session.getUserId());
                rs = pst.executeQuery();

                sb.append("\n  🕐  RECENT EXPENSES\n");
                sb.append("  ──────────────────────────────────────────────\n");
                while (rs.next()) {
                    String reason = rs.getString("reason");
                    if (reason == null || reason.isEmpty()) reason = "No reason";
                    sb.append(String.format("  %s  %s  - Ksh %,7.2f  [%s]\n",
                            rs.getDate("dateOfPayment"),
                            rs.getString("category"),
                            Math.abs(rs.getDouble("amount")),
                            reason.length() > 20 ? reason.substring(0, 20) + "..." : reason));
                }

            } catch (SQLException e) {
                sb.append("\n  ❌ Error loading expense data\n");
                e.printStackTrace();
            }

            final String text = sb.toString();
            SwingUtilities.invokeLater(() -> summaryArea.setText(text));
        });

        panel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        return panel;
    }

    // ================= CATEGORY BREAKDOWN PANEL =================
    private JPanel createCategoryBreakdownPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextArea breakdownArea = new JTextArea();
        breakdownArea.setFont(FONT_MONO);
        breakdownArea.setBackground(new Color(15, 30, 45));
        breakdownArea.setForeground(ATM_GREEN);
        breakdownArea.setEditable(false);

        EXECUTOR.submit(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("╔═══════════════════════════════════════════════════════════╗\n");
            sb.append("║               SPENDING BY CATEGORY                        ║\n");
            sb.append("╚═══════════════════════════════════════════════════════════╝\n\n");

            try (Connection conn = SecureDatabaseConnection.connect()) {
                PreparedStatement pst = conn.prepareStatement(
                        "SELECT category, COUNT(*) as count, SUM(ABS(amount)) as total, " +
                                "AVG(ABS(amount)) as avg_amount, MIN(ABS(amount)) as min_amount, " +
                                "MAX(ABS(amount)) as max_amount " +
                                "FROM mysaving2 " +
                                "WHERE user_id = ? AND amount < 0 " +
                                "GROUP BY category " +
                                "ORDER BY total DESC");
                pst.setInt(1, Session.getUserId());
                ResultSet rs = pst.executeQuery();

                double grandTotal = 0;
                List<Map<String, Object>> categories = new ArrayList<>();

                while (rs.next()) {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("name", rs.getString("category"));
                    cat.put("count", rs.getInt("count"));
                    cat.put("total", rs.getDouble("total"));
                    cat.put("avg", rs.getDouble("avg_amount"));
                    cat.put("min", rs.getDouble("min_amount"));
                    cat.put("max", rs.getDouble("max_amount"));
                    categories.add(cat);
                    grandTotal += rs.getDouble("total");
                }

                for (Map<String, Object> cat : categories) {
                    String name = (String) cat.get("name");
                    double total = (double) cat.get("total");
                    int count = (int) cat.get("count");
                    double avg = (double) cat.get("avg");
                    double min = (double) cat.get("min");
                    double max = (double) cat.get("max");
                    double percentage = grandTotal > 0 ? (total / grandTotal) * 100 : 0;

                    int barLength = 30;
                    int filled = (int) ((percentage / 100) * barLength);
                    StringBuilder bar = new StringBuilder("[");
                    for (int i = 0; i < barLength; i++) {
                        bar.append(i < filled ? "█" : "░");
                    }
                    bar.append("]");

                    sb.append(String.format("  %-20s %6.1f%% %s\n", name, percentage, bar.toString()));
                    sb.append(String.format("     Total: Ksh %,.2f | Count: %d | Avg: Ksh %,.2f\n",
                            total, count, avg));
                    sb.append(String.format("     Range: Ksh %,.2f - Ksh %,.2f\n", min, max));
                    sb.append("  ──────────────────────────────────────────────\n");
                }

                sb.append(String.format("\n  📊 Total Spending: Ksh %,.2f\n", grandTotal));
                sb.append(String.format("  📝 Total Categories: %d\n", categories.size()));

            } catch (SQLException e) {
                sb.append("  ❌ Error loading category breakdown\n");
                e.printStackTrace();
            }

            final String text = sb.toString();
            SwingUtilities.invokeLater(() -> breakdownArea.setText(text));
        });

        panel.add(new JScrollPane(breakdownArea), BorderLayout.CENTER);
        return panel;
    }

    // ================= EXPENSE HISTORY PANEL =================
    private JPanel createExpenseHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Filter controls
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(ATM_SCREEN_BG);

        JLabel filterLabel = new JLabel("Filter by:");
        filterLabel.setFont(FONT_LABEL);
        filterLabel.setForeground(ATM_GREEN_DIM);
        filterPanel.add(filterLabel);

        JComboBox<String> categoryFilter = new JComboBox<>(getUserCategories());
        styleComboBox(categoryFilter);
        filterPanel.add(categoryFilter);

        JButton filterBtn = new JButton("Apply");
        filterBtn.setFont(FONT_BUTTON);
        filterBtn.setBackground(ATM_BLUE);
        filterBtn.setForeground(Color.WHITE);
        filterBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        filterPanel.add(filterBtn);

        JButton resetBtn = new JButton("Reset");
        resetBtn.setFont(FONT_BUTTON);
        resetBtn.setBackground(BUTTON_BG);
        resetBtn.setForeground(ATM_RED);
        resetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        filterPanel.add(resetBtn);

        // Table
        String[] columns = {"Date", "Amount", "Category", "Reason"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildExpenseTable(model);

        // Load data
        loadExpenseHistory(model, null);

        filterBtn.addActionListener(e -> {
            String category = (String) categoryFilter.getSelectedItem();
            loadExpenseHistory(model, category);
        });

        resetBtn.addActionListener(e -> {
            categoryFilter.setSelectedIndex(0);
            loadExpenseHistory(model, null);
        });

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ================= EXPENSE INSIGHTS PANEL =================
    private JPanel createExpenseInsightsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextArea insightsArea = new JTextArea();
        insightsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        insightsArea.setBackground(new Color(15, 30, 45));
        insightsArea.setForeground(new Color(220, 230, 250));
        insightsArea.setEditable(false);
        insightsArea.setLineWrap(true);
        insightsArea.setWrapStyleWord(true);

        JButton generateBtn = new JButton("🤖 GENERATE SPENDING INSIGHTS");
        generateBtn.setFont(FONT_BUTTON);
        generateBtn.setBackground(ATM_PURPLE);
        generateBtn.setForeground(Color.WHITE);
        generateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        generateBtn.addActionListener(e -> generateSpendingInsights(insightsArea));

        panel.add(new JScrollPane(insightsArea), BorderLayout.CENTER);
        panel.add(generateBtn, BorderLayout.SOUTH);

        return panel;
    }

    // ================= GENERATE SPENDING INSIGHTS =================
    private void generateSpendingInsights(JTextArea area) {
        area.setText("🤖 Analyzing your spending patterns...\n\nPlease wait...");

        EXECUTOR.submit(() -> {
            try {
                StringBuilder data = new StringBuilder();
                double totalIncome = 0, totalExpenses = 0;
                Map<String, Double> categorySpending = new HashMap<>();

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT amount, category FROM mysaving2 WHERE user_id = ?");
                    pst.setInt(1, Session.getUserId());
                    ResultSet rs = pst.executeQuery();

                    while (rs.next()) {
                        double amount = rs.getDouble("amount");
                        String category = rs.getString("category");
                        if (amount > 0) {
                            totalIncome += amount;
                        } else {
                            double expense = Math.abs(amount);
                            totalExpenses += expense;
                            categorySpending.merge(category, expense, Double::sum);
                        }
                    }
                }

                double savingsRate = totalIncome > 0 ? ((totalIncome - totalExpenses) / totalIncome) * 100 : 0;
                String topCategory = categorySpending.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");

                String prompt = String.format(
                        "You are a financial advisor AI. Analyze this spending data:\n\n" +
                                "User: %s\n" +
                                "Total Income: Ksh %.2f\n" +
                                "Total Expenses: Ksh %.2f\n" +
                                "Savings Rate: %.1f%%\n" +
                                "Top Spending Category: %s\n" +
                                "Categories: %s\n\n" +
                                "Provide practical advice to improve savings.",
                        Session.getUsername(),
                        totalIncome, totalExpenses, savingsRate,
                        topCategory,
                        categorySpending.keySet().toString()
                );

                String insights = callGeminiAPI(prompt);

                StringBuilder display = new StringBuilder();
                display.append("\n  🤖  AI SPENDING INSIGHTS\n");
                display.append("  " + "─".repeat(50) + "\n\n");
                display.append("  📊  STATISTICAL SUMMARY\n");
                display.append("  " + "─".repeat(50) + "\n");
                display.append(String.format("     •  Income       : Ksh %,.2f\n", totalIncome));
                display.append(String.format("     •  Expenses     : Ksh %,.2f\n", totalExpenses));
                display.append(String.format("     •  Savings Rate : %.1f%%\n", Math.max(savingsRate, 0)));
                display.append(String.format("     •  Top Category : %s\n", topCategory));
                display.append("\n  💡  AI RECOMMENDATIONS\n");
                display.append("  " + "─".repeat(50) + "\n");
                display.append("  " + insights.replace("\n", "\n  ") + "\n\n");

                final String finalDisplay = display.toString();
                SwingUtilities.invokeLater(() -> {
                    area.setText(finalDisplay);
                    area.setCaretPosition(0);
                    soundSuccess();
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    area.setText("❌ Error generating insights:\n" + e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    // ================= LOAD EXPENSE HISTORY =================
    private void loadExpenseHistory(DefaultTableModel model, String categoryFilter) {
        model.setRowCount(0);
        EXECUTOR.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect()) {
                String sql = "SELECT dateOfPayment, amount, category, reason " +
                        "FROM mysaving2 WHERE user_id = ? AND amount < 0";
                if (categoryFilter != null && !categoryFilter.isEmpty() && !categoryFilter.equals("All")) {
                    sql += " AND category = ?";
                }
                sql += " ORDER BY dateOfPayment DESC LIMIT 100";

                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, Session.getUserId());
                if (categoryFilter != null && !categoryFilter.isEmpty() && !categoryFilter.equals("All")) {
                    pst.setString(2, categoryFilter);
                }

                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    final Date date = rs.getDate("dateOfPayment");
                    final double amount = Math.abs(rs.getDouble("amount"));
                    final String category = rs.getString("category");
                    final String reason = rs.getString("reason");
                    SwingUtilities.invokeLater(() -> {
                        model.addRow(new Object[]{
                                date,
                                "Ksh " + String.format("%,.2f", amount),
                                category != null ? category : "General",
                                reason != null ? reason : "No reason"
                        });
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // ================= BUILD EXPENSE TABLE =================
    private JTable buildExpenseTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? new Color(33, 38, 45) : new Color(22, 27, 34));
                    c.setForeground(new Color(201, 209, 217));
                } else {
                    c.setBackground(new Color(88, 166, 255, 55));
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        };

        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(48, 54, 61));
        table.setSelectionBackground(new Color(88, 166, 255, 55));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(22, 27, 34));
        table.getTableHeader().setForeground(new Color(110, 118, 129));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(48, 54, 61)));
        table.getTableHeader().setReorderingAllowed(false);

        // Amount column renderer
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isSel,
                                                           boolean hf, int row, int col) {
                JLabel l = new JLabel(String.valueOf(v));
                l.setFont(FONT_MONO);
                l.setOpaque(true);
                l.setForeground(ATM_RED);
                l.setBackground(isSel ? new Color(31, 111, 235, 60) :
                        (row % 2 == 0 ? new Color(33, 38, 45) : new Color(22, 27, 34)));
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
                return l;
            }
        });

        return table;
    }

    // ================= GET USER CATEGORIES =================
    private String[] getUserCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT DISTINCT category FROM mysaving2 WHERE user_id = ? AND category IS NOT NULL")) {
            pst.setInt(1, Session.getUserId());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (categories.size() <= 1) {
            categories.addAll(Arrays.asList("Food & Dining", "Transportation", "Shopping",
                    "Entertainment", "Bills & Utilities", "Health", "Education",
                    "Savings", "Investment", "General", "Other"));
        }

        return categories.toArray(new String[0]);
    }

    // ================= EXPORT SPENDING REPORT =================
    // ================= FIXED EXPORT SPENDING REPORT =================
    private void exportSpendingReport() {
        String[] categories = getUserCategories();
        String selectedCategory = (String) JOptionPane.showInputDialog(this,
                "Select category to filter (or All Categories):",
                "Export Spending Report",
                JOptionPane.QUESTION_MESSAGE,
                null,
                categories,
                categories[0]);

        if (selectedCategory == null) return;

        String filter = selectedCategory.equals("All Categories") ? null : selectedCategory;

        // Show loading dialog
        JDialog loadingDialog = new JDialog(this, "Generating Report...", true);
        loadingDialog.setSize(350, 150);
        loadingDialog.setLocationRelativeTo(this);
        loadingDialog.setUndecorated(true);

        JPanel loadPanel = new JPanel(new BorderLayout(10, 10));
        loadPanel.setBackground(ATM_SCREEN_BG);
        loadPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JLabel loadLabel = new JLabel("📄 Generating spending report...", SwingConstants.CENTER);
        loadLabel.setFont(new Font("Courier New", Font.BOLD, 14));
        loadLabel.setForeground(ATM_GREEN);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBackground(new Color(5, 20, 5));
        progressBar.setForeground(ATM_GREEN);

        loadPanel.add(loadLabel, BorderLayout.CENTER);
        loadPanel.add(progressBar, BorderLayout.SOUTH);
        loadingDialog.add(loadPanel);
        trackActivity("PDF_EXPORT");
        // Track PDF report in database
        trackPDFReport("SPENDING_REPORT");

        EXECUTOR.submit(() -> {
            try {
                String timestamp = LocalDateTime.now().format(DT_FORMATTER)
                        .replace(":", "-")
                        .replace(" ", "_");
                String filename = getReportsDirectory() + File.separator +
                        "Spending_Report_" + timestamp + ".pdf";

                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(filename));
                document.open();

                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.Font headingFont = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 10);

                document.add(new Paragraph("SUPREME MONEY COACH", titleFont));
                document.add(new Paragraph("SPENDING REPORT", titleFont));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DT_FORMATTER), normalFont));
                document.add(new Paragraph("User: " + Session.getUsername().toUpperCase(), normalFont));
                document.add(new Paragraph("Filter: " + (filter != null ? filter : "All Categories"), normalFont));
                document.add(new Paragraph(" "));

                // Summary
                double totalIncome = 0, totalExpenses = 0;
                Map<String, Double> categorySums = new LinkedHashMap<>();

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    PreparedStatement pst = conn.prepareStatement(
                            "SELECT " +
                                    "COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) as income, " +
                                    "COALESCE(SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END), 0) as expenses " +
                                    "FROM mysaving2 WHERE user_id = ?");
                    pst.setInt(1, Session.getUserId());
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        totalIncome = rs.getDouble("income");
                        totalExpenses = rs.getDouble("expenses");
                    }

                    String sql = "SELECT category, COUNT(*) as count, SUM(ABS(amount)) as total " +
                            "FROM mysaving2 WHERE user_id = ? AND amount < 0";
                    if (filter != null) {
                        sql += " AND category = ?";
                    }
                    sql += " GROUP BY category ORDER BY total DESC";

                    pst = conn.prepareStatement(sql);
                    pst.setInt(1, Session.getUserId());
                    if (filter != null) {
                        pst.setString(2, filter);
                    }
                    rs = pst.executeQuery();

                    while (rs.next()) {
                        categorySums.put(rs.getString("category"), rs.getDouble("total"));
                    }
                }

                // Summary table
                PdfPTable summaryTable = new PdfPTable(2);
                summaryTable.setWidthPercentage(60);
                summaryTable.setWidths(new float[]{40f, 60f});

                addTableRow(summaryTable, "Total Income:", "Ksh " + String.format("%,.2f", totalIncome));
                addTableRow(summaryTable, "Total Expenses:", "Ksh " + String.format("%,.2f", totalExpenses));
                addTableRow(summaryTable, "Net Savings:", "Ksh " + String.format("%,.2f", totalIncome - totalExpenses));

                document.add(summaryTable);
                document.add(new Paragraph(" "));

                // Category breakdown
                document.add(new Paragraph("CATEGORY BREAKDOWN", headingFont));
                document.add(new Paragraph(" "));

                PdfPTable categoryTable = new PdfPTable(3);
                categoryTable.setWidthPercentage(80);
                categoryTable.setWidths(new float[]{35f, 30f, 35f});

                addTableHeader(categoryTable, "Category", "Amount", "Percentage");

                double grandTotal = categorySums.values().stream().mapToDouble(Double::doubleValue).sum();
                for (Map.Entry<String, Double> entry : categorySums.entrySet()) {
                    double percentage = grandTotal > 0 ? (entry.getValue() / grandTotal) * 100 : 0;
                    addTableRow(categoryTable,
                            entry.getKey(),
                            "Ksh " + String.format("%,.2f", entry.getValue()),
                            String.format("%.1f%%", percentage));
                }

                document.add(categoryTable);
                document.add(new Paragraph(" "));

                // Recent transactions
                document.add(new Paragraph("RECENT TRANSACTIONS", headingFont));
                document.add(new Paragraph(" "));

                PdfPTable transactionTable = new PdfPTable(4);
                transactionTable.setWidthPercentage(95);
                transactionTable.setWidths(new float[]{20f, 25f, 25f, 30f});

                addTableHeader(transactionTable, "Date", "Category", "Amount", "Reason");

                try (Connection conn = SecureDatabaseConnection.connect()) {
                    String sql = "SELECT dateOfPayment, amount, category, reason " +
                            "FROM mysaving2 WHERE user_id = ? AND amount < 0";
                    if (filter != null) {
                        sql += " AND category = ?";
                    }
                    sql += " ORDER BY dateOfPayment DESC LIMIT 50";

                    PreparedStatement pst = conn.prepareStatement(sql);
                    pst.setInt(1, Session.getUserId());
                    if (filter != null) {
                        pst.setString(2, filter);
                    }
                    ResultSet rs = pst.executeQuery();

                    while (rs.next()) {
                        String reason = rs.getString("reason");
                        addTableRow(transactionTable,
                                rs.getDate("dateOfPayment").toString(),
                                rs.getString("category"),
                                "- Ksh " + String.format("%,.2f", Math.abs(rs.getDouble("amount"))),
                                reason != null ? reason : "No reason"
                        );
                    }
                }

                document.add(transactionTable);
                document.add(new Paragraph(" "));

                document.add(new Paragraph("Report generated by Supreme Money Coach", normalFont));
                document.add(new Paragraph("© " + java.time.Year.now().getValue() + " Supreme Money Coach", normalFont));

                document.close();

                soundSuccess();

                // Close loading dialog
                SwingUtilities.invokeLater(() -> loadingDialog.dispose());

                // ========== FIXED: Properly sized success dialog ==========
                SwingUtilities.invokeLater(() -> {
                    // Create a custom dialog with proper sizing
                    JDialog successDialog = new JDialog(this, "✅ Export Complete", true);
                    successDialog.setSize(550, 300);
                    successDialog.setLocationRelativeTo(this);
                    successDialog.setUndecorated(true);
                    successDialog.setBackground(ATM_SCREEN_BG);

                    JPanel successPanel = new JPanel(new BorderLayout(15, 15));
                    successPanel.setBackground(ATM_SCREEN_BG);
                    successPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ATM_GREEN, 3),
                            BorderFactory.createEmptyBorder(25, 30, 25, 30)));

                    // Icon
                    JLabel iconLabel = new JLabel("✅", SwingConstants.CENTER);
                    iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
                    iconLabel.setForeground(ATM_GREEN);

                    // Message
                    JPanel messagePanel = new JPanel(new GridLayout(3, 1, 5, 8));
                    messagePanel.setBackground(ATM_SCREEN_BG);

                    JLabel titleLabel = new JLabel("SPENDING REPORT EXPORTED SUCCESSFULLY!", SwingConstants.CENTER);
                    titleLabel.setFont(new Font("Courier New", Font.BOLD, 16));
                    titleLabel.setForeground(ATM_GREEN);

                    // Format the filename to be more readable
                    String displayFilename = filename;
                    // Show just the filename, not the full path
                    String[] pathParts = filename.split(File.separator);
                    String shortName = pathParts[pathParts.length - 1];

                    JLabel fileLabel = new JLabel("📄 " + shortName, SwingConstants.CENTER);
                    fileLabel.setFont(new Font("Courier New", Font.PLAIN, 12));
                    fileLabel.setForeground(ATM_AMBER);

                    JLabel locationLabel = new JLabel("📁 Saved in: " + getReportsDirectory(), SwingConstants.CENTER);
                    locationLabel.setFont(new Font("Courier New", Font.PLAIN, 10));
                    locationLabel.setForeground(ATM_GREEN_DIM);

                    messagePanel.add(titleLabel);
                    messagePanel.add(fileLabel);
                    messagePanel.add(locationLabel);

                    // Buttons
                    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
                    buttonPanel.setBackground(ATM_SCREEN_BG);

                    JButton openBtn = new JButton("📂 OPEN FOLDER");
                    openBtn.setFont(new Font("Courier New", Font.BOLD, 12));
                    openBtn.setBackground(ATM_BLUE);
                    openBtn.setForeground(Color.WHITE);
                    openBtn.setFocusPainted(false);
                    openBtn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.WHITE, 1),
                            BorderFactory.createEmptyBorder(8, 20, 8, 20)));
                    openBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    openBtn.addActionListener(ev -> {
                        try {
                            Desktop.getDesktop().open(new File(getReportsDirectory()));
                        } catch (IOException ex) {
                            ATMDialog.error(this, "Cannot open folder: " + ex.getMessage());
                        }
                        successDialog.dispose();
                    });

                    JButton closeBtn = new JButton("✓ OK");
                    closeBtn.setFont(new Font("Courier New", Font.BOLD, 12));
                    closeBtn.setBackground(ATM_GREEN);
                    closeBtn.setForeground(Color.WHITE);
                    closeBtn.setFocusPainted(false);
                    closeBtn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.WHITE, 1),
                            BorderFactory.createEmptyBorder(8, 30, 8, 30)));
                    closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    closeBtn.addActionListener(ev -> successDialog.dispose());

                    buttonPanel.add(openBtn);
                    buttonPanel.add(closeBtn);

                    successPanel.add(iconLabel, BorderLayout.NORTH);
                    successPanel.add(messagePanel, BorderLayout.CENTER);
                    successPanel.add(buttonPanel, BorderLayout.SOUTH);

                    successDialog.setContentPane(successPanel);
                    successDialog.setVisible(true);
                });

                setStatus("► Spending report exported: " + filename);

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> loadingDialog.dispose());
                soundError();
                SwingUtilities.invokeLater(() -> {
                    // ========== FIXED: Error dialog with proper sizing ==========
                    JDialog errorDialog = new JDialog(this, "❌ Export Failed", true);
                    errorDialog.setSize(450, 250);
                    errorDialog.setLocationRelativeTo(this);
                    errorDialog.setUndecorated(true);
                    errorDialog.setBackground(ATM_SCREEN_BG);

                    JPanel errorPanel = new JPanel(new BorderLayout(15, 15));
                    errorPanel.setBackground(ATM_SCREEN_BG);
                    errorPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ATM_RED, 3),
                            BorderFactory.createEmptyBorder(25, 30, 25, 30)));

                    JLabel iconLabel = new JLabel("❌", SwingConstants.CENTER);
                    iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
                    iconLabel.setForeground(ATM_RED);

                    JTextArea errorArea = new JTextArea(
                            "Failed to export spending report.\n\n" +
                                    "Error: " + ex.getMessage() + "\n\n" +
                                    "Please check:\n" +
                                    "• Database connection\n" +
                                    "• File permissions\n" +
                                    "• Try again later"
                    );
                    errorArea.setFont(new Font("Courier New", Font.PLAIN, 12));
                    errorArea.setForeground(ATM_RED);
                    errorArea.setBackground(ATM_SCREEN_BG);
                    errorArea.setEditable(false);
                    errorArea.setLineWrap(true);
                    errorArea.setWrapStyleWord(true);

                    JButton okBtn = new JButton("✓ OK");
                    okBtn.setFont(new Font("Courier New", Font.BOLD, 12));
                    okBtn.setBackground(BUTTON_BG);
                    okBtn.setForeground(ATM_RED);
                    okBtn.setFocusPainted(false);
                    okBtn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ATM_RED, 1),
                            BorderFactory.createEmptyBorder(8, 30, 8, 30)));
                    okBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    okBtn.addActionListener(ev -> errorDialog.dispose());

                    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                    buttonPanel.setBackground(ATM_SCREEN_BG);
                    buttonPanel.add(okBtn);

                    errorPanel.add(iconLabel, BorderLayout.NORTH);
                    errorPanel.add(errorArea, BorderLayout.CENTER);
                    errorPanel.add(buttonPanel, BorderLayout.SOUTH);

                    errorDialog.setContentPane(errorPanel);
                    errorDialog.setVisible(true);
                });
                ex.printStackTrace();
            }
        });

        loadingDialog.setVisible(true);
    }

    // ================= TABLE HELPER METHODS FOR PDF =================
    private void addTableHeader(PdfPTable table, String... headers) {
        com.itextpdf.text.Font font = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
        for (String header : headers) {
            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                    new com.itextpdf.text.Paragraph(header, font));
            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String... values) {
        com.itextpdf.text.Font font = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
        for (String value : values) {
            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                    new com.itextpdf.text.Paragraph(value, font));
            cell.setPadding(4);
            table.addCell(cell);
        }
    }



    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        boolean isSmall = isSmallScreen();
        int fullWidth = isSmall ? 180 : 220;
        int iconWidth = isSmall ? 50 : 60;

        Component[] comps = sidebar.getComponents();

        if (sidebarVisible) {
            sidebar.setPreferredSize(new Dimension(fullWidth, 0));
            toggleSidebarBtn.setText("☰ MENU");

            // Full text versions
            String[] fullTexts = {"☰ MENU", "◆ AI", "◈ CHAMA", "◉ CHAT", "○ PROFILE",
                    "◊ DEBT", "▣ REQUEST", "▶ VIDEOS", "📊 EXPENSES",
                    "📄 EXPORT SPENDING", "↻ REFRESH", "✕ EXIT", "◙ KISWAHILI"};
            int idx = 0;
            for (Component comp : comps) {
                if (comp instanceof JButton) {
                    if (idx < fullTexts.length) {
                        ((JButton) comp).setText(fullTexts[idx++]);
                    }
                }
            }
        } else {
            sidebar.setPreferredSize(new Dimension(iconWidth, 0));
            toggleSidebarBtn.setText("☰");

            // Icon only versions
            String[] iconTexts = {"☰", "◆", "◈", "◉", "○", "◊", "▣", "▶", "📊", "📄", "↻", "✕", "◙"};
            int idx = 0;
            for (Component comp : comps) {
                if (comp instanceof JButton) {
                    if (idx < iconTexts.length) {
                        ((JButton) comp).setText(iconTexts[idx++]);
                    }
                }
            }
        }

        sidebar.revalidate();
        sidebar.repaint();
    }

    // ================= SPLASH SCREEN =================
    private JLabel splashStatusLabel;

    private void playCardInsertAnimation() {
        soundBeep();
        CardLayout cl = (CardLayout) contentArea.getLayout();
        cl.show(contentArea, CARD_SPLASH);
        String[] frames = {"  ► INSERTING CARD...", "  ► READING CARD...", "  ► VERIFYING USER...", "  ► WELCOME, " + Session.username.toUpperCase() + "!"};
        Timer timer = new Timer(600, null);
        int[] index = {0};
        timer.addActionListener(e -> {
            if (index[0] < frames.length) {
                splashStatusLabel.setText(frames[index[0]]);
                index[0]++;
            } else {
                timer.stop();
                SwingUtilities.invokeLater(() -> {
                    cl.show(contentArea, CARD_MAIN);
                    refreshAll();
                    startClock();
                });
            }
        });
        timer.start();
    }

    private JPanel buildSplashScreen() {
        JPanel screen = new JPanel();
        screen.setLayout(new BoxLayout(screen, BoxLayout.Y_AXIS));
        screen.setBackground(new Color(10, 22, 40));
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_BORDER, 1)));
        screen.add(Box.createVerticalGlue());

        JLabel logoLabel = new JLabel("◈");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 72));
        logoLabel.setForeground(ATM_AMBER);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        screen.add(logoLabel);
        screen.add(Box.createVerticalStrut(10));

        JLabel bankName = new JLabel("SUPREME MONEY COACH");
        bankName.setFont(new Font("Segoe UI", Font.BOLD, 24));
        bankName.setForeground(ATM_GREEN);
        bankName.setAlignmentX(Component.CENTER_ALIGNMENT);
        screen.add(bankName);
        screen.add(Box.createVerticalStrut(5));

        JLabel tagline = new JLabel("•  Your Financial Freedom Starts Here  •");
        tagline.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        tagline.setForeground(ATM_GREEN_DIM);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        screen.add(tagline);
        screen.add(Box.createVerticalStrut(40));

        splashStatusLabel = new JLabel("  ●  INSERTING CARD...", SwingConstants.CENTER);
        splashStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        splashStatusLabel.setForeground(ATM_GREEN);
        splashStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        screen.add(splashStatusLabel);
        screen.add(Box.createVerticalStrut(20));

        JProgressBar loadBar = new JProgressBar(0, 100);
        loadBar.setForeground(ATM_GREEN);
        loadBar.setBackground(new Color(5, 20, 5));
        loadBar.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        loadBar.setStringPainted(true);
        loadBar.setFont(new Font("Segoe UI", Font.BOLD, 10));
        loadBar.setMaximumSize(new Dimension(400, 20));
        loadBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        screen.add(loadBar);
        screen.add(Box.createVerticalGlue());

        Timer loadTimer = new Timer(30, null);
        int[] progress = {0};
        loadTimer.addActionListener(e -> {
            if (progress[0] < 100) {
                progress[0] += 2;
                loadBar.setValue(progress[0]);
                loadBar.setString(progress[0] + "%");
                if (progress[0] < 20) splashStatusLabel.setText("  ●  INSERTING CARD...");
                else if (progress[0] < 40) splashStatusLabel.setText("  ●  READING CARD...");
                else if (progress[0] < 60) splashStatusLabel.setText("  ●  VERIFYING USER...");
                else if (progress[0] < 80) splashStatusLabel.setText("  ●  CONNECTING TO SERVER...");
                else if (progress[0] < 95) splashStatusLabel.setText("  ●  LOADING YOUR DATA...");
                else {
                    splashStatusLabel.setText("  ●  WELCOME, " + Session.getUsername().toUpperCase() + "!");
                    splashStatusLabel.setForeground(ATM_AMBER);
                }
            } else {
                loadTimer.stop();
                SwingUtilities.invokeLater(() -> {
                    CardLayout cl = (CardLayout) contentArea.getLayout();
                    cl.show(contentArea, CARD_MAIN);
                    refreshAll();
                    startClock();
                });
            }
        });
        loadTimer.start();
        return screen;
    }

    // ================= MAIN SCREEN =================
    private JPanel buildMainScreen() {
        JPanel screen = new JPanel();
        screen.setLayout(new BoxLayout(screen, BoxLayout.Y_AXIS));
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 15, 4, 15, ATM_BG),
                BorderFactory.createLineBorder(ATM_BORDER, 1)));

        // ========== HERO SECTION ==========
        screen.add(buildHeroSection());
        screen.add(Box.createVerticalStrut(15));

        // ========== BALANCE CARD ==========
        screen.add(buildBalanceCard());
        screen.add(Box.createVerticalStrut(10));

        // ========== GOAL PANEL ==========
        screen.add(buildGoalPanel());
        screen.add(Box.createVerticalStrut(10));

        // ========== RECENT TRANSACTIONS ==========
        screen.add(buildMiniStatement());
        screen.add(Box.createVerticalStrut(8));

        // ========== ACTION BUTTONS ROW ==========
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        actionRow.setBackground(ATM_SCREEN_BG);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));


        screen.add(actionRow);
        screen.add(Box.createVerticalStrut(5));

        // ========== STATUS LABEL ==========
        statusLabel = new JLabel("  ► SYSTEM READY");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(ATM_GREEN);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 0));
        screen.add(statusLabel);

        // ========== WRAP IN SCROLL PANE ==========
        JScrollPane scrollPane = new JScrollPane(screen);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(ATM_SCREEN_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Return the scroll pane as a JPanel
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ATM_SCREEN_BG);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    // ================= STYLE ACTION BUTTON =================
    private void styleActionButton(JButton btn, Color color) {
        boolean isSmall = isSmallScreen();
        btn.setFont(new Font("Segoe UI", Font.BOLD, isSmall ? 10 : 11));
        btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                btn.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                btn.setForeground(color);
            }
        });
    }


    // Use it in your UI building methods
    private int getPadding() {
        return isSmallScreen() ? 8 : 15;
    }

    private int getFontSize() {
        return isSmallScreen() ? 10 : 13;
    }

    private JPanel buildHeroSection() {
        JPanel heroPanel = new JPanel();
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));
        heroPanel.setBackground(new Color(15, 30, 50));
        heroPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        heroPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        row1.setBackground(new Color(15, 30, 50));
        JLabel welcomeLabel = new JLabel("🔥 " + Session.username.toUpperCase() + " 🔥");
        welcomeLabel.setFont(new Font("Courier New", Font.BOLD, 14));
        welcomeLabel.setForeground(ATM_AMBER);
        JLabel quoteLabel = new JLabel("\"Small savings = Big wealth\"");
        quoteLabel.setFont(new Font("Courier New", Font.ITALIC, 11));
        quoteLabel.setForeground(ATM_GREEN);
        row1.add(welcomeLabel);
        row1.add(quoteLabel);
        heroPanel.add(row1);
        heroPanel.add(Box.createVerticalStrut(5));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 0));
        row2.setBackground(new Color(15, 30, 50));
        String[] labels = {"🤖 AI COACH", "💰 SMART SAVE", "📈 GROW WEALTH"};
        Color[] colors = {ATM_CYAN, ATM_GREEN, ATM_AMBER};
        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Courier New", Font.BOLD, 10));
            label.setForeground(colors[i]);
            row2.add(label);
        }
        heroPanel.add(row2);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        row3.setBackground(new Color(10, 25, 45));
        row3.setBorder(BorderFactory.createLineBorder(ATM_CYAN, 1));
        JLabel tipIcon = new JLabel("💡");
        tipIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
        JLabel tipText = new JLabel(getDailyTip());
        tipText.setFont(new Font("Courier New", Font.PLAIN, 9));
        tipText.setForeground(ATM_GREEN);
        row3.add(tipIcon);
        row3.add(tipText);
        heroPanel.add(row3);
        return heroPanel;
    }

    private String getDailyTip() {
        String[] tips = {
                "💰 Save at least 10% of every income - pay yourself first!",
                "📊 Use the 50/30/20 rule: 50% needs, 30% wants, 20% savings",
                "🎯 Set specific savings goals - you're 42% more likely to achieve them",
                "👥 Join a Chama to save with others - accountability boosts success",
                "📱 Use M-Pesa savings features to automate your savings",
                "💡 Track every expense - small leaks sink big ships!",
                "🏦 Start an emergency fund before investing (3-6 months of expenses)",
                "📈 Invest in MMFs for low-risk returns on your savings",
                "🤖 Ask your AI Coach for personalized financial advice",
                "🔥 Consistency beats intensity - save small amounts regularly"
        };
        return tips[LocalDate.now().getDayOfYear() % tips.length];
    }

    // ================= BALANCE CARD =================
    private JPanel buildBalanceCard() {
        JPanel card = new JPanel(new GridLayout(3, 2, 10, 6));
        card.setBackground(new Color(5, 20, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 15, 0, 15, ATM_SCREEN_BG),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ATM_BORDER, 1),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15))));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        balTitleLbl = new JLabel("BALANCE");
        balTitleLbl.setFont(FONT_LABEL);
        balTitleLbl.setForeground(ATM_GREEN_DIM);
        balanceValueLabel = new JLabel("Ksh 0");
        balanceValueLabel.setFont(FONT_VALUE);
        balanceValueLabel.setForeground(ATM_GREEN);

        goalTitleLbl = new JLabel("GOAL");
        goalTitleLbl.setFont(FONT_LABEL);
        goalTitleLbl.setForeground(ATM_GREEN_DIM);
        goalValueLabel = new JLabel("Ksh 15000");
        goalValueLabel.setFont(FONT_VALUE);
        goalValueLabel.setForeground(ATM_AMBER);

        JButton setGoalBtn = new JButton("✎ SET GOAL");
        setGoalBtn.setFont(FONT_SMALL);
        setGoalBtn.setBackground(BUTTON_BG);
        setGoalBtn.setForeground(ATM_BLUE);
        setGoalBtn.setFocusPainted(false);
        setGoalBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BLUE, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        setGoalBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setGoalBtn.addActionListener(e -> showSetGoalDialog());

        remTitleLbl = new JLabel("REMAINING");
        remTitleLbl.setFont(FONT_LABEL);
        remTitleLbl.setForeground(ATM_GREEN_DIM);
        remainingValueLabel = new JLabel("Ksh 15000");
        remainingValueLabel.setFont(FONT_VALUE);
        remainingValueLabel.setForeground(ATM_RED);

        card.add(balTitleLbl);
        card.add(balanceValueLabel);
        card.add(goalTitleLbl);

        JPanel goalPanel = new JPanel(new BorderLayout(5, 0));
        goalPanel.setBackground(new Color(5, 20, 5));
        goalPanel.add(goalValueLabel, BorderLayout.WEST);
        goalPanel.add(setGoalBtn, BorderLayout.EAST);
        card.add(goalPanel);
        card.add(remTitleLbl);
        card.add(remainingValueLabel);
        return card;
    }

    // ================= GOAL PANEL =================
    private JPanel buildGoalPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 4));
        p.setBackground(ATM_SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        progressTitleLbl = new JLabel("PROGRESS:");
        progressTitleLbl.setFont(FONT_LABEL);
        progressTitleLbl.setForeground(ATM_GREEN_DIM);

        goalBar = new JProgressBar(0, SAVINGS_GOAL);
        goalBar.setStringPainted(true);
        goalBar.setFont(FONT_SMALL);
        goalBar.setForeground(ATM_GREEN);
        goalBar.setBackground(new Color(5, 20, 5));
        goalBar.setBorderPainted(true);
        goalBar.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        p.add(progressTitleLbl, BorderLayout.WEST);
        p.add(goalBar, BorderLayout.CENTER);
        return p;
    }

    // ================= MINI STATEMENT =================
    private JPanel buildMiniStatement() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(ATM_SCREEN_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        miniStatTitleLbl = new JLabel("RECENT TXNS (LAST 3)");
        miniStatTitleLbl.setFont(FONT_LABEL);
        miniStatTitleLbl.setForeground(ATM_AMBER);
        miniStatTitleLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        miniStatementArea = new JTextArea(5, 40);
        miniStatementArea.setFont(FONT_SMALL);
        miniStatementArea.setBackground(new Color(5, 20, 5));
        miniStatementArea.setForeground(ATM_GREEN);
        miniStatementArea.setEditable(false);
        miniStatementArea.setMargin(new Insets(6, 10, 6, 10));
        miniStatementArea.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        p.add(miniStatTitleLbl, BorderLayout.NORTH);
        p.add(new JScrollPane(miniStatementArea), BorderLayout.CENTER);
        return p;
    }

    // ================= KEYPAD =================
    private JPanel buildKeypad() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ATM_BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        JLabel depositLabel = new JLabel("QUICK DEPOSIT ►");
        depositLabel.setFont(FONT_LABEL);
        depositLabel.setForeground(ATM_GREEN_DIM);
        depositLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JPanel depositRow = new JPanel(new GridLayout(1, 4, 8, 0));
        depositRow.setBackground(ATM_BG);
        int[] presets = {500, 1000, 2000, 5000};
        for (int amount : presets) {
            JButton btn = atmButton("+ " + amount);
            btn.addActionListener(e -> { soundBeep(); quickDeposit(amount); });
            depositRow.add(btn);
        }

        JPanel actionRow = new JPanel(new GridLayout(1, 3, 8, 0));
        actionRow.setBackground(ATM_BG);
        withdrawBtn = atmButton("WITHDRAW");
        customBtn = atmButton("DEPOSIT");
        statementBtn = atmButton("STATEMENT");
        withdrawBtn.setForeground(ATM_RED);
        statementBtn.setForeground(ATM_AMBER);
        withdrawBtn.addActionListener(e -> doWithdraw());
        customBtn.addActionListener(e -> customDeposit());
        statementBtn.addActionListener(e -> { soundBeep(); showFullStatement(); });
        actionRow.add(withdrawBtn);
        actionRow.add(customBtn);
        actionRow.add(statementBtn);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottomRow.setBackground(ATM_BG);
        JButton exitBottomBtn = atmButton("EXIT");
        exitBottomBtn.setForeground(ATM_RED);
        exitBottomBtn.setFont(new Font("Courier New", Font.BOLD, 12));
        exitBottomBtn.addActionListener(e -> exitWithAnimation());
        bottomRow.add(exitBottomBtn);

        JPanel lower = new JPanel(new GridLayout(3, 1, 0, 8));
        lower.setBackground(ATM_BG);
        lower.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        lower.add(depositRow);
        lower.add(actionRow);
        lower.add(bottomRow);

        wrapper.add(depositLabel, BorderLayout.NORTH);
        wrapper.add(lower, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton atmButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Courier New", Font.BOLD, 13));
        btn.setBackground(BUTTON_BG);
        btn.setForeground(ATM_GREEN);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(60, 65, 70)); btn.setForeground(ATM_GREEN); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BUTTON_BG); btn.setForeground(ATM_GREEN); }
        });
        return btn;
    }

    // ================= DEPOSIT & WITHDRAW =================
    private void quickDeposit(int amount) {
        JDialog depositDialog = new JDialog(this, "Record Deposit", true);
        depositDialog.setSize(450, 350);
        depositDialog.setLocationRelativeTo(this);
        depositDialog.setUndecorated(true);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_GREEN, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Amount (Ksh):"), gbc);
        JLabel amountLabel = new JLabel("Ksh " + amount);
        amountLabel.setFont(FONT_VALUE);
        amountLabel.setForeground(ATM_GREEN);
        gbc.gridx = 1;
        panel.add(amountLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Source:"), gbc);
        JComboBox<String> sourceCombo = new JComboBox<>(new String[]{"Salary", "Business", "Gift", "Investment", "Savings", "Other"});
        styleComboBox(sourceCombo);
        gbc.gridx = 1;
        panel.add(sourceCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Note:"), gbc);
        JTextField noteField = new JTextField(20);
        styleTextField(noteField);
        gbc.gridx = 1;
        panel.add(noteField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);

        JButton confirmBtn = new JButton("✓ DEPOSIT");
        confirmBtn.setFont(FONT_BUTTON);
        confirmBtn.setBackground(ATM_GREEN);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton cancelBtn = new JButton("✗ CANCEL");
        cancelBtn.setFont(FONT_BUTTON);
        cancelBtn.setBackground(BUTTON_BG);
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        confirmBtn.addActionListener(e -> {
            String source = (String) sourceCombo.getSelectedItem();
            String note = noteField.getText().trim();
            depositDialog.dispose();
            processDeposit(amount, source, note);
        });
        cancelBtn.addActionListener(e -> depositDialog.dispose());
        trackActivity("DEPOSIT");

        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, gbc);
        depositDialog.add(panel);
        depositDialog.setVisible(true);
    }

    private void processDeposit(int amount, String source, String note) {
        EXECUTOR.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement maxPst = conn.prepareStatement(
                         "SELECT COALESCE(MAX(weekNO), 0) + 1 FROM mysaving2 WHERE user_id=?");
                 PreparedStatement insPst = conn.prepareStatement(
                         "INSERT INTO mysaving2(user_id, weekNO, dateOfPayment, amount, day, category, reason) VALUES(?,?,?,?,?,?,?)")) {

                maxPst.setInt(1, Session.getUserId());
                ResultSet rs = maxPst.executeQuery();
                int nextWeek = rs.next() ? rs.getInt(1) : 1;

                insPst.setInt(1, Session.getUserId());
                insPst.setInt(2, nextWeek);
                insPst.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                insPst.setInt(4, amount);
                insPst.setString(5, "DEPOSIT");
                insPst.setString(6, source != null ? source : "Income");
                insPst.setString(7, note != null && !note.isEmpty() ? note : "Deposit");
                insPst.executeUpdate();

                NotificationService.create(Session.getUserId(),
                        "💰 Deposit: Ksh " + amount + " from " + source,
                        NotificationService.SUCCESS);

                soundSuccess();
                setStatus("► DEPOSITED Ksh " + amount + " (" + source + ")");
                CACHE.remove("balance_" + Session.userId);
                CACHE_EXPIRY.remove("balance_" + Session.userId);
                refreshAll();
            } catch (SQLException ex) {
                soundError();
                setStatus("► ERROR: " + ex.getMessage());
            }
        });
    }

    private void customDeposit() {
        String input = ATMDialog.input(this, isKiswahili ? "Weka Kiasi:" : "Enter Amount To Deposit:");
        if (input == null) return;
        try { quickDeposit(Integer.parseInt(input.trim())); }
        catch (NumberFormatException ex) { soundError(); setStatus("► INVALID AMOUNT"); }
    }

    private void doWithdraw() {
        JDialog withdrawDialog = new JDialog(this, "Record Withdrawal", true);
        withdrawDialog.setSize(550, 520);
        withdrawDialog.setLocationRelativeTo(this);
        withdrawDialog.setUndecorated(true);
        withdrawDialog.setBackground(ATM_SCREEN_BG);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(ATM_SCREEN_BG);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(20, 25, 25, 25)));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ATM_SCREEN_BG);
        JLabel titleLabel = new JLabel("💸 WITHDRAW MONEY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(ATM_AMBER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ATM_SCREEN_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel amountLabel = new JLabel("💰 Amount (Ksh):");
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        amountLabel.setForeground(ATM_GREEN);
        formPanel.add(amountLabel, gbc);

        gbc.gridx = 1;
        JTextField amountField = new JTextField(15);
        amountField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        amountField.setBackground(new Color(15, 30, 45));
        amountField.setForeground(ATM_GREEN);
        amountField.setCaretColor(ATM_GREEN);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        formPanel.add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel categoryLabel = new JLabel("📂 Category:");
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        categoryLabel.setForeground(ATM_GREEN);
        formPanel.add(categoryLabel, gbc);

        gbc.gridx = 1;
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
                "Food & Dining", "Transportation", "Shopping", "Entertainment",
                "Bills & Utilities", "Health", "Education", "Savings",
                "Investment", "Emergency", "Rent", "Insurance", "Gifts", "Travel", "Other"
        });
        styleComboBox(categoryCombo);
        formPanel.add(categoryCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel reasonLabel = new JLabel("📝 Reason for Withdrawal:");
        reasonLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        reasonLabel.setForeground(ATM_GREEN);
        formPanel.add(reasonLabel, gbc);

        gbc.gridx = 1;
        JTextArea reasonArea = new JTextArea(3, 20);
        reasonArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reasonArea.setBackground(new Color(15, 30, 45));
        reasonArea.setForeground(ATM_GREEN);
        reasonArea.setCaretColor(ATM_GREEN);
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        reasonScroll.setBorder(null);
        reasonScroll.setPreferredSize(new Dimension(250, 80));
        formPanel.add(reasonScroll, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        JLabel quickLabel = new JLabel("Quick Select Reasons:");
        quickLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        quickLabel.setForeground(ATM_GREEN_DIM);
        formPanel.add(quickLabel, gbc);

        gbc.gridy = 4;
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        quickPanel.setBackground(ATM_SCREEN_BG);
        String[] quickReasons = {"Food 🍔", "Transport 🚗", "Shopping 🛍️", "Bills 📄",
                "Entertainment 🎮", "Health 🏥", "Education 📚", "Emergency 🚨"};
        for (String reason : quickReasons) {
            JButton btn = new JButton(reason);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            btn.setBackground(new Color(40, 50, 70));
            btn.setForeground(ATM_CYAN);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createLineBorder(ATM_CYAN, 1));
            btn.addActionListener(e -> {
                String text = reason.replaceAll(" [🍔🚗🛍️📄🎮🏥📚🚨]", "");
                if (reasonArea.getText().isEmpty()) reasonArea.setText(text);
                else reasonArea.setText(reasonArea.getText() + ", " + text);
            });
            quickPanel.add(btn);
        }
        formPanel.add(quickPanel, gbc);

        gbc.gridy = 5;
        JPanel balancePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        balancePanel.setBackground(new Color(15, 30, 45));
        balancePanel.setBorder(BorderFactory.createLineBorder(ATM_GREEN, 1));
        try {
            int balance = getCurrentBalance();
            JLabel balanceLabel = new JLabel("💰 Current Balance: Ksh " + String.format("%,d", balance));
            balanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            balanceLabel.setForeground(ATM_GREEN);
            balancePanel.add(balanceLabel);
        } catch (SQLException e) {
            balancePanel.add(new JLabel("💰 Balance: --"));
        }
        formPanel.add(balancePanel, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(20, 10, 10, 10);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        buttonPanel.setBackground(ATM_SCREEN_BG);

        JButton confirmBtn = new JButton("✓ WITHDRAW");
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        confirmBtn.setBackground(ATM_RED);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmBtn.setPreferredSize(new Dimension(160, 45));

        JButton cancelBtn = new JButton("✗ CANCEL");
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelBtn.setBackground(new Color(60, 65, 70));
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setPreferredSize(new Dimension(140, 45));
        cancelBtn.setBorder(BorderFactory.createLineBorder(ATM_RED, 1));

        confirmBtn.addActionListener(e -> {
            String amountStr = amountField.getText().trim();
            String category = (String) categoryCombo.getSelectedItem();
            String reason = reasonArea.getText().trim();

            if (amountStr.isEmpty()) {
                ATMDialog.error(this, "⚠️ Please enter the amount you want to withdraw.");
                amountField.requestFocus();
                return;
            }

            try {
                int amount = Integer.parseInt(amountStr.replace(",", ""));
                if (amount <= 0) {
                    ATMDialog.error(this, "⚠️ Amount must be greater than 0.");
                    return;
                }

                int balance = getCurrentBalance();
                if (amount > balance) {
                    soundError();
                    ATMDialog.error(this, "❌ Insufficient funds!\n\nBalance: Ksh " +
                            String.format("%,d", balance) + "\nRequested: Ksh " +
                            String.format("%,d", amount));
                    return;
                }

                withdrawDialog.dispose();
                processWithdrawal(amount, category, reason);

            } catch (NumberFormatException ex) {
                soundError();
                ATMDialog.error(this, "⚠️ Please enter a valid number.");
                amountField.requestFocus();
            } catch (SQLException ex) {
                soundError();
                ATMDialog.error(this, "❌ Error checking balance:\n" + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> withdrawDialog.dispose());

        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);
        formPanel.add(buttonPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        withdrawDialog.add(mainPanel);
        withdrawDialog.setVisible(true);
    }

    private int getCurrentBalance() throws SQLException {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM mysaving2 WHERE user_id=?")) {
            pst.setInt(1, Session.userId);
            ResultSet rs = pst.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void processWithdrawal(int amount, String category, String reason) {
        EXECUTOR.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement maxPst = conn.prepareStatement(
                         "SELECT COALESCE(MAX(weekNO), 0) + 1 FROM mysaving2 WHERE user_id=?");
                 PreparedStatement insPst = conn.prepareStatement(
                         "INSERT INTO mysaving2(user_id, weekNO, dateOfPayment, amount, day, category, reason) VALUES(?,?,?,?,?,?,?)")) {

                maxPst.setInt(1, Session.getUserId());
                ResultSet rs = maxPst.executeQuery();
                int nextWeek = rs.next() ? rs.getInt(1) : 1;

                insPst.setInt(1, Session.getUserId());
                insPst.setInt(2, nextWeek);
                insPst.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                insPst.setInt(4, -amount);
                insPst.setString(5, "WITHDRAWAL");
                insPst.setString(6, category != null ? category : "General");
                insPst.setString(7, reason != null && !reason.isEmpty() ? reason : "No reason provided");
                insPst.executeUpdate();

                NotificationService.create(Session.getUserId(),
                        "💸 Withdrawal: Ksh " + amount + " for " + category,
                        NotificationService.WARNING);

                soundSuccess();
                setStatus("► WITHDREW Ksh " + amount + " (" + category + ")");
                CACHE.remove("balance_" + Session.userId);
                CACHE_EXPIRY.remove("balance_" + Session.userId);
                refreshAll();
            } catch (SQLException ex) {
                soundError();
                setStatus("► ERROR: " + ex.getMessage());
            }
        });
    }

    // ================= REFRESH METHODS =================
    private void refreshAll() {
        System.out.println("Refreshing all data...");
        updateBalanceCard();
        updateMiniStatement();
        refreshChamaData();  // This already exists
        refreshMyChamasPanel();  // Add this for extra safety
    }

    private void updateBalanceCard() {
        EXECUTOR.submit(() -> {
            String cacheKey = "balance_" + Session.userId;
            if (isCacheValid(cacheKey)) {
                SwingUtilities.invokeLater(() -> updateBalanceUI((int[]) CACHE.get(cacheKey)));
                return;
            }

            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT COALESCE(SUM(amount), 0) as total, " +
                                 "(SELECT COALESCE(savings_goal, 15000) FROM users WHERE id = ?) as goal " +
                                 "FROM mysaving2 WHERE user_id = ?")) {
                pst.setInt(1, Session.userId);
                pst.setInt(2, Session.userId);
                ResultSet rs = pst.executeQuery();
                int total = 0, userGoal = 15000;
                if (rs.next()) {
                    total = rs.getInt("total");
                    userGoal = rs.getInt("goal");
                    if (userGoal <= 0) userGoal = 15000;
                }
                int[] result = {total, userGoal};
                cacheResult(cacheKey, result);
                SwingUtilities.invokeLater(() -> updateBalanceUI(result));
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
    }

    private void updateBalanceUI(int[] data) {
        int total = data[0], goal = data[1];
        int rem = goal - total;
        double pct = ((double) total / goal) * 100;
        balanceValueLabel.setText("Ksh " + total);
        goalValueLabel.setText("Ksh " + goal);
        remainingValueLabel.setText("Ksh " + Math.max(rem, 0));
        goalBar.setMaximum(goal);
        goalBar.setValue(Math.min(total, goal));
        goalBar.setString(String.format("%.1f%%", Math.min(pct, 100)));
        if (pct >= 100) {
            remainingValueLabel.setForeground(ATM_GREEN);
            remainingValueLabel.setText("GOAL REACHED!");
        } else if (pct >= 75) {
            remainingValueLabel.setForeground(ATM_AMBER);
        } else {
            remainingValueLabel.setForeground(ATM_RED);
        }
    }

    private void updateMiniStatement() {
        EXECUTOR.submit(() -> {
            String cacheKey = "miniStatement_" + Session.userId;
            if (isCacheValid(cacheKey)) {
                SwingUtilities.invokeLater(() -> miniStatementArea.setText((String) CACHE.get(cacheKey)));
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  %-6s %-12s %-12s%n", "ID", "DATE", "AMOUNT"));
            sb.append("  ────────────────────────────────\n");
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT id, dateOfPayment, amount FROM mysaving2 WHERE user_id=? ORDER BY id DESC LIMIT 3")) {
                pst.setInt(1, Session.userId);
                ResultSet rs = pst.executeQuery();
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    int amt = rs.getInt("amount");
                    sb.append(String.format("  %-6d %-12s %s%d%n",
                            rs.getInt("id"), rs.getDate("dateOfPayment"),
                            amt < 0 ? "-Ksh " : "+Ksh ", Math.abs(amt)));
                }
                if (!any) sb.append("  NO TRANSACTIONS YET\n");
            } catch (SQLException ex) { sb.append("  ERROR LOADING\n"); }
            String result = sb.toString();
            cacheResult(cacheKey, result);
            SwingUtilities.invokeLater(() -> miniStatementArea.setText(result));
        });
    }

    private void refreshChamaData() {
        // Clear any cached Chama data
        chamaMap.clear();

        // Force reload from database
        List<Map<String, Object>> userChamas = ChamaGroup.getUserChamas(Session.userId);
        if (!userChamas.isEmpty()) {
            for (Map<String, Object> chama : userChamas) {
                int id = (Integer) chama.get("id");
                chamaMap.put(id, chama);
            }
        }

        System.out.println("🔄 Chama data refreshed. Found " + userChamas.size() + " Chamas.");
    }

    // ================= STATEMENT SCREEN =================
    private JPanel buildStatementScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_BORDER, 1)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(5, 20, 5));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel(" SUPREME MONEY COACH - STATEMENT");
        title.setFont(new Font("Courier New", Font.BOLD, 18));
        title.setForeground(ATM_AMBER);

        JButton backBtn = atmButton("◄ BACK");
        backBtn.setFont(new Font("Courier New", Font.BOLD, 12));
        backBtn.addActionListener(e -> {
            soundBeep();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_MAIN);
        });

        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);

        fullStatementArea = new JTextArea();
        fullStatementArea.setFont(new Font("Courier New", Font.PLAIN, 13));
        fullStatementArea.setBackground(new Color(5, 20, 5));
        fullStatementArea.setForeground(new Color(220, 230, 250));
        fullStatementArea.setEditable(false);
        fullStatementArea.setMargin(new Insets(15, 15, 15, 15));

        JScrollPane scroll = new JScrollPane(fullStatementArea);
        scroll.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        scroll.getViewport().setBackground(new Color(5, 20, 5));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        footer.setBackground(new Color(5, 20, 5));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ATM_BORDER));

        JButton downloadBtn = atmButton("⬇ DOWNLOAD RECEIPT (PDF)");
        downloadBtn.setFont(new Font("Courier New", Font.BOLD, 12));
        downloadBtn.setForeground(ATM_AMBER);
        downloadBtn.addActionListener(e -> { soundBeep(); downloadReceipt(); });
        footer.add(downloadBtn);

        screen.add(header, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(footer, BorderLayout.SOUTH);
        return screen;
    }

    private void showFullStatement() {
        StringBuilder sb = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DT_FORMATTER);

        sb.append("╔════════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                         SUPREME MONEY COACH - STATEMENT                        ║\n");
        sb.append("╠════════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Account  : %-69s ║\n", Session.username.toUpperCase()));
        sb.append(String.format("║  Printed  : %-69s ║\n", timestamp));
        sb.append("╚════════════════════════════════════════════════════════════════════════════════╝\n\n");

        sb.append("┌────────┬────────────┬────────────────┬──────────────┬──────────────┐\n");
        sb.append("│   ID   │    DATE    │     AMOUNT     │     TYPE     │     DAY      │\n");
        sb.append("├────────┼────────────┼────────────────┼──────────────┼──────────────┤\n");

        int dep = 0, wdr = 0, tx = 0;
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, dateOfPayment, amount, day FROM mysaving2 WHERE user_id=? ORDER BY id ASC")) {
            pst.setInt(1, Session.userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                int amt = rs.getInt("amount");
                String amountStr = (amt < 0 ? "- Ksh " : "+ Ksh ") + String.format("%,d", Math.abs(amt));
                String type = amt < 0 ? "WITHDRAWAL" : "DEPOSIT";
                sb.append(String.format("│  %3d   │  %s  │  %11s  │   %-9s│   %-8s │\n",
                        rs.getInt("id"), rs.getDate("dateOfPayment"), amountStr, type, rs.getString("day")));
                if (amt >= 0) dep += amt;
                else wdr += Math.abs(amt);
                tx++;
            }
        } catch (SQLException ex) {
            sb.append("│  ERROR LOADING TRANSACTIONS                                             │\n");
        }

        sb.append("└────────┴────────────┴────────────────┴──────────────┴──────────────┘\n\n");

        int net = dep - wdr;
        int userGoal = (int) getCurrentGoalValue();
        int remaining = userGoal - net;
        double progress = ((double) net / userGoal) * 100;

        sb.append("╔════════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                              FINANCIAL SUMMARY                                  ║\n");
        sb.append("╠════════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Total Transactions  : %-54d ║\n", tx));
        sb.append(String.format("║  Total Deposits       : Ksh %-49s ║\n", String.format("%,d", dep)));
        sb.append(String.format("║  Total Withdrawals    : Ksh %-49s ║\n", String.format("%,d", wdr)));
        sb.append("╠════════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  NET BALANCE          : Ksh %-49s ║\n", String.format("%,d", net)));
        sb.append(String.format("║  SAVINGS GOAL         : Ksh %-49s ║\n", String.format("%,d", userGoal)));
        sb.append(String.format("║  REMAINING            : Ksh %-49s ║\n", String.format("%,d", Math.max(remaining, 0))));

        int barLength = 40;
        int filled = (int)((progress / 100) * barLength);
        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            progressBar.append(i < filled ? "█" : "░");
        }
        progressBar.append("]");
        sb.append(String.format("║  PROGRESS             : %-5s %-38s ║\n", String.format("%.1f%%", progress), progressBar.toString()));
        sb.append("╚════════════════════════════════════════════════════════════════════════════════╝\n\n");

        sb.append("  Thank you for banking with Supreme Money Coach\n");
        sb.append("  Your Path to Financial Freedom\n");
        sb.append("  ───────────────────────────────────────────────────────────────────────────────\n");
        sb.append("  Statement generated: ").append(timestamp).append("\n");
        trackActivity("VIEW_STATEMENT");

        fullStatementArea.setText(sb.toString());
        fullStatementArea.setCaretPosition(0);
        CardLayout cl = (CardLayout) contentArea.getLayout();
        cl.show(contentArea, CARD_STATEMENT);
    }

    private void downloadReceipt() {
        String timestamp = LocalDateTime.now().format(DT_FORMATTER).replace(":", "-").replace(" ", "_");
        String filename = getReportsDirectory() + File.separator + "Receipt_" + timestamp + ".pdf";

        EXECUTOR.submit(() -> {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(filename));
                document.open();

                com.itextpdf.text.Font fTitle = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.Font fHead = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.Font fNorm = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 10);
                com.itextpdf.text.Font fGreen = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL,
                        new com.itextpdf.text.BaseColor(0, 150, 50));
                com.itextpdf.text.Font fRed = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL,
                        new com.itextpdf.text.BaseColor(200, 50, 50));

                document.add(new Paragraph("SUPREME MONEY COACH", fTitle));
                document.add(new Paragraph("TRANSACTION RECEIPT", fTitle));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Account Holder : " + Session.username.toUpperCase(), fNorm));
                document.add(new Paragraph("Generated On   : " + LocalDateTime.now().format(DT_FORMATTER), fNorm));
                document.add(new Paragraph("Reference No   : RCP-" + System.currentTimeMillis(), fNorm));
                document.add(new Paragraph(" "));
                trackActivity("PDF_EXPORT");

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{10f, 20f, 20f, 20f, 30f});

                for (String h : new String[]{"ID", "DATE", "AMOUNT", "TYPE", "DAY"}) {
                    com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new Paragraph(h, fHead));
                    cell.setBackgroundColor(new com.itextpdf.text.BaseColor(220, 220, 220));
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                int dep = 0, wdr = 0, tx = 0;
                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "SELECT id, dateOfPayment, amount, day FROM mysaving2 WHERE user_id=? ORDER BY id ASC")) {
                    pst.setInt(1, Session.userId);
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        int amt = rs.getInt("amount");
                        com.itextpdf.text.Font rf = amt < 0 ? fRed : fGreen;
                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new Paragraph(String.valueOf(rs.getInt("id")), rf)));
                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new Paragraph(String.valueOf(rs.getDate("dateOfPayment")), rf)));
                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new Paragraph((amt < 0 ? "- Ksh " : "+ Ksh ") + String.format("%,d", Math.abs(amt)), rf)));
                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new Paragraph(amt < 0 ? "WITHDRAWAL" : "DEPOSIT", rf)));
                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new Paragraph(rs.getString("day"), rf)));
                        if (amt >= 0) dep += amt; else wdr += Math.abs(amt);
                        tx++;
                    }
                }

                int net = dep - wdr;
                int userGoal = (int) getCurrentGoalValue();

                document.add(table);
                document.add(new Paragraph(" "));
                document.add(new Paragraph("SUMMARY", fHead));
                document.add(new Paragraph(String.format("%-30s %d", "Total Transactions :", tx), fNorm));
                document.add(new Paragraph(String.format("%-30s Ksh %d", "Total Deposits     :", dep), fGreen));
                document.add(new Paragraph(String.format("%-30s Ksh %d", "Total Withdrawals  :", wdr), fRed));
                document.add(new Paragraph(String.format("%-30s Ksh %d", "Net Balance        :", net), fNorm));
                document.add(new Paragraph(String.format("%-30s Ksh %d", "Savings Goal       :", userGoal), fNorm));
                document.add(new Paragraph(String.format("%-30s %.1f%%", "Goal Progress      :",
                        Math.min(((double) net / userGoal) * 100, 100)), fNorm));
                document.close();
                // Track PDF report in database
                trackPDFReport("RECEIPT");
                trackActivity("PDF_EXPORT");


                soundSuccess();
                SwingUtilities.invokeLater(() -> {
                    ATMDialog.success(this, "Receipt saved as:\n" + filename, "Export Complete");
                    setStatus("► RECEIPT SAVED: " + filename);
                });
            } catch (Exception ex) {
                soundError();
                SwingUtilities.invokeLater(() -> ATMDialog.error(this, "Failed:\n" + ex.getMessage()));
            }
        });
    }
    private void trackPDFReport(String reportType) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO pdf_reports (user_id, report_type, filename, created_at) VALUES (?, ?, ?, NOW())")) {
            String filename = "Report_" + System.currentTimeMillis() + ".pdf";
            pst.setInt(1, Session.getUserId());
            pst.setString(2, reportType);
            pst.setString(3, filename);
            pst.executeUpdate();
            System.out.println("✅ PDF Report tracked: " + reportType);
        } catch (SQLException e) {
            System.err.println("⚠️ Failed to track PDF report: " + e.getMessage());
        }
    }

    // ================= AI SCREEN =================
    private JPanel buildAIScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_BORDER, 1)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ATM_SCREEN_BG);
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("  ◈ AI FINANCIAL INSIGHTS  (Powered by Gemini)");
        title.setFont(new Font("Courier New", Font.BOLD, 16));
        title.setForeground(ATM_AMBER);

        JButton backBtn = atmButton("◄ BACK");
        backBtn.addActionListener(e -> {
            soundBeep();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_MAIN);
        });

        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);

        aiInsightsArea = new JTextArea();
        aiInsightsArea.setFont(new Font("Courier New", Font.PLAIN, 14));
        aiInsightsArea.setBackground(new Color(15, 30, 45));
        aiInsightsArea.setForeground(new Color(220, 230, 250));
        aiInsightsArea.setEditable(false);
        aiInsightsArea.setLineWrap(true);
        aiInsightsArea.setWrapStyleWord(true);
        aiInsightsArea.setMargin(new Insets(15, 15, 15, 15));
        aiInsightsArea.setText(
                "  ╔════════════════════════════════════════════════════════════════╗\n" +
                        "  ║           SUPREME MONEY COACH AI ADVISOR                      ║\n" +
                        "  ║           Powered by Google Gemini                             ║\n" +
                        "  ║           Press GENERATE INSIGHTS below to get                ║\n" +
                        "  ║           personalized financial advice                       ║\n" +
                        "  ╚════════════════════════════════════════════════════════════════╝\n");

        JScrollPane scroll = new JScrollPane(aiInsightsArea);
        scroll.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        scroll.getViewport().setBackground(new Color(15, 30, 45));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel footer = new JPanel(new GridLayout(1, 2, 10, 0));
        footer.setBackground(ATM_SCREEN_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));

        aiGenerateBtn = atmButton("◉ GENERATE INSIGHTS");
        aiGenerateBtn.setForeground(ATM_GREEN);
        aiGenerateBtn.setFont(new Font("Courier New", Font.BOLD, 13));
        aiGenerateBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_GREEN, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        aiGenerateBtn.addActionListener(e -> { soundBeep(); generateAIInsights(); });

        JButton exportBtn = atmButton("⬇ EXPORT INSIGHTS PDF");
        exportBtn.setForeground(ATM_AMBER);
        exportBtn.setFont(new Font("Courier New", Font.BOLD, 13));
        exportBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        exportBtn.addActionListener(e -> { soundBeep(); exportInsightsPDF(); });

        footer.add(aiGenerateBtn);
        footer.add(exportBtn);

        screen.add(header, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(footer, BorderLayout.SOUTH);
        return screen;
    }

    private void generateAIInsights() {
        aiInsightsArea.setText("\n\n  ◉ CONNECTING TO GEMINI AI...\n\n  Analyzing your financial data, please wait...\n");
        aiGenerateBtn.setEnabled(false);

        EXECUTOR.submit(() -> {
            try {
                StringBuilder data = new StringBuilder();
                int dep = 0, wdr = 0, wk = 0, tx = 0, hi = 0, lo = Integer.MAX_VALUE;
                data.append("Weekly savings records:\n");
                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "SELECT weekNO, dateOfPayment, amount, day FROM mysaving2 WHERE user_id=? ORDER BY id ASC")) {
                    pst.setInt(1, Session.userId);
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        int amt = rs.getInt("amount");
                        data.append("  Week ").append(rs.getInt("weekNO"))
                                .append(" | ").append(rs.getDate("dateOfPayment"))
                                .append(" | Ksh ").append(amt)
                                .append(" | ").append(rs.getString("day")).append("\n");
                        if (amt >= 0) { dep += amt; wk++; if(amt>hi) hi=amt; if(amt<lo) lo=amt; }
                        else wdr += Math.abs(amt);
                        tx++;
                    }
                }
                if (lo == Integer.MAX_VALUE) lo = 0;
                int net = dep - wdr, rem = Math.max(SAVINGS_GOAL - net, 0);
                double pct = Math.min(((double) net / SAVINGS_GOAL) * 100, 100);
                double avg = wk > 0 ? (double) dep / wk : 0;
                int eta = avg > 0 ? (int) Math.ceil(rem / avg) : -1;
                int userGoal = getUserGoal();

                String langInstr = isKiswahili ? "IMPORTANT: Respond entirely in KISWAHILI." : "Respond in ENGLISH.";
                String prompt = "You are an ATM financial advisor AI for Supreme Money Coach in Kenya. " +
                        langInstr + " Plain text, no markdown, no asterisks.\n\n" +
                        "Sections:\n1. SAVINGS PACE ANALYSIS\n2. GOAL FORECAST\n" +
                        "3. SPENDING WARNINGS\n4. TOP 3 RECOMMENDATIONS\n5. MOTIVATIONAL MESSAGE\n\n" +
                        "DATA:\nAccount: " + Session.username + "\nGoal: Ksh " + userGoal +
                        "\nNet balance: Ksh " + net + "\nDeposits: Ksh " + dep +
                        "\nWithdrawals: Ksh " + wdr + "\nWeeks saving: " + wk +
                        "\nAvg/week: Ksh " + String.format("%.0f", avg) +
                        "\nProgress: " + String.format("%.1f", pct) + "%" +
                        "\nRemaining: Ksh " + rem +
                        "\nWeeks to goal: " + (eta > 0 ? eta : "N/A") +
                        "\nDate: " + LocalDateTime.now().format(DT_FORMATTER) + "\n\n" + data;

                String aiText = callGeminiAPI(prompt);
                String timestamp = LocalDateTime.now().format(DT_FORMATTER);

                StringBuilder display = new StringBuilder();
                display.append("╔════════════════════════════════════════════════════════════════════════════════╗\n");
                display.append("║                         SUPREME MONEY COACH AI ADVISOR                         ║\n");
                display.append("║                              Powered by Google Gemini                           ║\n");
                display.append("╠════════════════════════════════════════════════════════════════════════════════╣\n");
                display.append("║  Language: ").append(isKiswahili ? "KISWAHILI" : "ENGLISH");
                display.append(String.format("%72s", "Analysis: " + timestamp)).append("║\n");
                display.append("╚════════════════════════════════════════════════════════════════════════════════╝\n\n");

                for (String line : aiText.split("\n")) {
                    if (line.trim().startsWith("1.") || line.trim().startsWith("2.") ||
                            line.trim().startsWith("3.") || line.trim().startsWith("4.") ||
                            line.trim().startsWith("5.")) {
                        display.append("\n  ").append(line).append("\n");
                        display.append("  ─────────────────────────────────────────────────────────────────────────\n");
                    } else {
                        display.append("  ").append(line).append("\n");
                    }
                }

                display.append("\n");
                display.append("╔════════════════════════════════════════════════════════════════════════════════╗\n");
                display.append("║                              FINANCIAL SUMMARY                                 ║\n");
                display.append("╠════════════════════════════════════════════════════════════════════════════════╣\n");
                display.append(String.format("║  Balance   : Ksh %,d%69s║\n", net, " "));
                display.append(String.format("║  Progress  : %.1f%%%70s║\n", pct, " "));
                display.append(String.format("║  Remaining : Ksh %,d%69s║\n", rem, " "));
                if (eta > 0) display.append(String.format("║  ETA       : ~%d weeks%68s║\n", eta, " "));
                display.append(String.format("║  Generated : %s%52s║\n", timestamp, " "));
                display.append("╚════════════════════════════════════════════════════════════════════════════════╝\n");

                String fd = display.toString();
                // ✅ Track activity
                trackActivity("AI_INSIGHT");

                SwingUtilities.invokeLater(() -> {
                    soundSuccess();
                    aiInsightsArea.setText(fd);
                    aiInsightsArea.setCaretPosition(0);
                    aiGenerateBtn.setEnabled(true);
                    setStatus("► AI INSIGHTS GENERATED  [" + LocalDateTime.now().format(DT_FORMATTER) + "]");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    soundError();
                    aiInsightsArea.setText("\n\n  ⚠️ ERROR: " + ex.getMessage() + "\n\n  Please try again.");
                    aiGenerateBtn.setEnabled(true);
                });
            }
        });
    }

    private int getUserGoal() {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT COALESCE(savings_goal, 15000) FROM users WHERE id = ?")) {
            pst.setInt(1, Session.userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 15000;
    }

    private void exportInsightsPDF() {
        String content = aiInsightsArea.getText();
        if (content.contains("Press GENERATE")) {
            ATMDialog.warning(this, "Please generate insights first.");
            return;
        }

        String timestamp = LocalDateTime.now().format(DT_FORMATTER).replace(":", "-").replace(" ", "_");
        String filename = getReportsDirectory() + File.separator + "AIInsights_" + timestamp + ".pdf";

        EXECUTOR.submit(() -> {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(filename));
                document.open();

                com.itextpdf.text.Font fTitle = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.COURIER, 16, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.Font fNorm = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.COURIER, 10);

                document.add(new Paragraph("SUPREME MONEY COACH", fTitle));
                document.add(new Paragraph("AI FINANCIAL INSIGHTS REPORT", fTitle));
                document.add(new Paragraph("Account  : " + Session.getUsername().toUpperCase(), fNorm));
                document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DT_FORMATTER), fNorm));
                document.add(new Paragraph(" "));

                for (String line : content.split("\n")) {
                    String t = line.trim();
                    if (!t.isEmpty() && !t.startsWith("╔") && !t.startsWith("║") &&
                            !t.startsWith("╚") && !t.startsWith("══") && !t.startsWith("──")) {
                        document.add(new Paragraph(t, fNorm));
                    }
                }
                document.close();
                // Track PDF report in database
                trackPDFReport("AI_INSIGHTS_REPORT");
                trackActivity("PDF_EXPORT");

                soundSuccess();
                SwingUtilities.invokeLater(() -> {
                    ATMDialog.success(this, "Saved as:\n" + filename, "Export Complete");
                    setStatus("► AI PDF SAVED: " + filename);
                });
            } catch (Exception ex) {
                soundError();
                SwingUtilities.invokeLater(() -> ATMDialog.error(this, "Export failed:\n" + ex.getMessage()));
            }
        });
    }

    // ================= CHAMA SCREEN =================
    private JPanel buildChamaScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_PURPLE, 1)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 5, 25));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel("  ◈ GROUP GOAL  —  CHAMA / MERRY-GO-ROUND");
        title.setFont(new Font("Courier New", Font.BOLD, 13));
        title.setForeground(ATM_PURPLE);
        JButton backBtn = atmButton("◄ BACK");
        backBtn.addActionListener(e -> { soundBeep(); CardLayout cl = (CardLayout) contentArea.getLayout(); cl.show(contentArea, CARD_MAIN); });
        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);

        chamaResultArea = new JTextArea();
        chamaResultArea.setFont(FONT_SMALL);
        chamaResultArea.setBackground(new Color(15, 5, 25));
        chamaResultArea.setForeground(ATM_PURPLE);
        chamaResultArea.setEditable(false);
        chamaResultArea.setLineWrap(true);
        chamaResultArea.setWrapStyleWord(true);
        chamaResultArea.setMargin(new Insets(12, 15, 12, 15));
        chamaResultArea.setText("  ┌────────────────────────────────────────────┐\n" +
                "  │   CHAMA GROUP GOAL CALCULATOR              │\n" +
                "  │   Press CALCULATE GROUP GOAL below         │\n" +
                "  └────────────────────────────────────────────┘\n");

        JScrollPane scroll = new JScrollPane(chamaResultArea);
        scroll.setBorder(BorderFactory.createLineBorder(ATM_PURPLE));
        scroll.getViewport().setBackground(new Color(15, 5, 25));

        JPanel footer = new JPanel(new GridLayout(1, 2, 10, 0));
        footer.setBackground(new Color(15, 5, 25));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ATM_PURPLE),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));

        JButton calcBtn = atmButton("◉ CALCULATE GROUP GOAL");
        calcBtn.setForeground(ATM_PURPLE);
        calcBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_PURPLE, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        calcBtn.addActionListener(e -> { soundBeep(); calculateChama(); });

        JButton exportBtn = atmButton("⬇ EXPORT CHAMA PDF");
        exportBtn.setForeground(ATM_AMBER);
        exportBtn.addActionListener(e -> { soundBeep(); exportChamaPDF(); });

        footer.add(calcBtn);
        footer.add(exportBtn);
        screen.add(header, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(footer, BorderLayout.SOUTH);
        return screen;
    }

    private void openChamaPlanner() {
        CardLayout cl = (CardLayout) contentArea.getLayout();
        cl.show(contentArea, CARD_CHAMA);
        calculateChama();
    }

    private void calculateChama() {
        JPanel namePanel = new JPanel(new BorderLayout(10, 10));
        namePanel.setBackground(ATM_SCREEN_BG);
        JLabel nameLabel = new JLabel("Enter Group Name (e.g. Umoja Chama):");
        nameLabel.setFont(FONT_LABEL);
        nameLabel.setForeground(ATM_GREEN_DIM);
        JTextField nameField = new JTextField(20);
        styleTextField(nameField);
        namePanel.add(nameLabel, BorderLayout.NORTH);
        namePanel.add(nameField, BorderLayout.CENTER);
        int nameResult = JOptionPane.showConfirmDialog(this, namePanel, "Group Name", JOptionPane.OK_CANCEL_OPTION);
        if (nameResult != JOptionPane.OK_OPTION) return;
        String groupName = nameField.getText().trim();
        if (groupName.isEmpty()) return;

        JPanel goalPanel = new JPanel(new BorderLayout(10, 10));
        goalPanel.setBackground(ATM_SCREEN_BG);
        JLabel goalLabel = new JLabel("Enter Total Group Goal Amount (Ksh):");
        goalLabel.setFont(FONT_LABEL);
        goalLabel.setForeground(ATM_GREEN_DIM);
        JTextField goalField = new JTextField(20);
        styleTextField(goalField);
        goalPanel.add(goalLabel, BorderLayout.NORTH);
        goalPanel.add(goalField, BorderLayout.CENTER);
        int goalResult = JOptionPane.showConfirmDialog(this, goalPanel, "Group Goal", JOptionPane.OK_CANCEL_OPTION);
        if (goalResult != JOptionPane.OK_OPTION) return;
        String targetStr = goalField.getText().trim();
        if (targetStr.isEmpty()) return;

        JPanel membersPanel = new JPanel(new BorderLayout(10, 10));
        membersPanel.setBackground(ATM_SCREEN_BG);
        JLabel membersLabel = new JLabel("How many members?");
        membersLabel.setFont(FONT_LABEL);
        membersLabel.setForeground(ATM_GREEN_DIM);
        JTextField membersField = new JTextField(10);
        styleTextField(membersField);
        membersField.setText("10");
        membersPanel.add(membersLabel, BorderLayout.NORTH);
        membersPanel.add(membersField, BorderLayout.CENTER);
        int membersResult = JOptionPane.showConfirmDialog(this, membersPanel, "Number of Members", JOptionPane.OK_CANCEL_OPTION);
        if (membersResult != JOptionPane.OK_OPTION) return;
        String membersStr = membersField.getText().trim();
        if (membersStr.isEmpty()) membersStr = "10";

        JPanel weeksPanel = new JPanel(new BorderLayout(10, 10));
        weeksPanel.setBackground(ATM_SCREEN_BG);
        JLabel weeksLabel = new JLabel("How many weeks? (52 = 1 year, 26 = 6 months):");
        weeksLabel.setFont(FONT_LABEL);
        weeksLabel.setForeground(ATM_GREEN_DIM);
        JTextField weeksField = new JTextField(10);
        styleTextField(weeksField);
        weeksField.setText("52");
        weeksPanel.add(weeksLabel, BorderLayout.NORTH);
        weeksPanel.add(weeksField, BorderLayout.CENTER);
        int weeksResult = JOptionPane.showConfirmDialog(this, weeksPanel, "Timeline", JOptionPane.OK_CANCEL_OPTION);
        if (weeksResult != JOptionPane.OK_OPTION) return;
        String weeksStr = weeksField.getText().trim();
        if (weeksStr.isEmpty()) return;

        try {
            int target = Integer.parseInt(targetStr.trim());
            int members = Integer.parseInt(membersStr.trim());
            int weeks = Integer.parseInt(weeksStr.trim());
            if (target <= 0 || members <= 0 || weeks <= 0) {
                soundError();
                ATMDialog.error(this, "All values must be greater than 0.");
                return;
            }
            double weeklyPerMember = (double) target / members / weeks;
            double monthlyPerMember = weeklyPerMember * 4.33;
            double dailyPerMember = weeklyPerMember / 7;
            int totalWeeklyAllMembers = (int) Math.ceil(weeklyPerMember * members);
            int totalMonthlyAllMembers = (int) (totalWeeklyAllMembers * 4.33);
            double totalMonthsToGoal = weeks / 4.33;
            String timestamp = LocalDateTime.now().format(DT_FORMATTER);

            StringBuilder sb = new StringBuilder();
            sb.append("  ╔════════════════════════════════════════════════╗\n");
            sb.append("  ║         CHAMA GROUP GOAL CALCULATOR            ║\n");
            sb.append("  ║  Calculated : ").append(String.format("%-33s", timestamp)).append("║\n");
            sb.append("  ╚════════════════════════════════════════════════╝\n\n");
            sb.append("  GROUP DETAILS\n");
            sb.append("  ──────────────────────────────────────────────\n");
            sb.append(String.format("  %-20s %s%n", "Group Name    :", groupName.toUpperCase()));
            sb.append(String.format("  %-20s Ksh %,d%n", "Group Target  :", target));
            sb.append(String.format("  %-20s %d members%n", "Total Members :", members));
            sb.append(String.format("  %-20s %d weeks (%.1f months)%n", "Timeline      :", weeks, totalMonthsToGoal));
            sb.append("\n  PER MEMBER CONTRIBUTION\n");
            sb.append("  ──────────────────────────────────────────────\n");
            sb.append(String.format("  %-20s Ksh %,.0f%n", "Daily   :", dailyPerMember));
            sb.append(String.format("  %-20s Ksh %,.0f%n", "Weekly  :", weeklyPerMember));
            sb.append(String.format("  %-20s Ksh %,.0f%n", "Monthly :", monthlyPerMember));
            sb.append("\n  GROUP TOTALS\n");
            sb.append("  ──────────────────────────────────────────────\n");
            sb.append(String.format("  %-20s Ksh %,d%n", "Total Weekly  :", totalWeeklyAllMembers));
            sb.append(String.format("  %-20s Ksh %,d%n", "Total Monthly :", totalMonthlyAllMembers));
            sb.append(String.format("  %-20s Ksh %,d%n", "Final Amount  :", target));
            sb.append("\n  VERIFICATION\n");
            sb.append("  ──────────────────────────────────────────────\n");
            sb.append(String.format("  %-20s Ksh %,d%n", "Over %d weeks:", weeks, (int)(totalWeeklyAllMembers * weeks)));
            sb.append("  ADVISOR NOTES\n");
            sb.append("  ──────────────────────────────────────────────\n");
            if (weeklyPerMember < 200) sb.append("  ✓ Excellent! Very manageable for each member.\n");
            else if (weeklyPerMember < 500) sb.append("  ✓ Good target. Consider M-Pesa automation.\n");
            else if (weeklyPerMember < 1000) sb.append("  ⚠ Ambitious. Ensure all members have stable income.\n");
            else sb.append("  ⚠ HIGH contribution. Consider extending timeline.\n");
            sb.append("\n  ► Tip: Use M-Pesa Savings or a Chama bank account.\n");
            sb.append("  ► Tip: Set up automated weekly reminders.\n");
            sb.append("  ► Tip: Hold monthly meetings to review progress.\n");
            sb.append("\n  ════════════════════════════════════════════════\n");
            sb.append("  Supreme Money Coach - Chama Planner\n");
            sb.append("  Generated: ").append(timestamp).append("\n");

            chamaResultArea.setText(sb.toString());
            chamaResultArea.setCaretPosition(0);
            soundChama();
            setStatus("► CHAMA PLAN CALCULATED  [" + LocalDateTime.now().format(DT_FORMATTER) + "]");
        } catch (NumberFormatException ex) {
            soundError();
            ATMDialog.error(this, "Please enter valid numbers only.");
        }
    }
    private void exportChamaPDF() {
        String content = chamaResultArea.getText();
        if (content.contains("Press CALCULATE")) {
            ATMDialog.warning(this, "Please calculate the group goal first.");
            return;
        }

        // Save to Desktop
        String userHome = System.getProperty("user.home");
        String desktopPath = userHome + File.separator + "Desktop";

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = desktopPath + File.separator + "ChamaGoal_" + timestamp + ".pdf";

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();

            com.itextpdf.text.Font fTitle = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.COURIER, 16, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font fNorm = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.COURIER, 10);

            document.add(new Paragraph("SUPREME MONEY COACH", fTitle));
            document.add(new Paragraph("CHAMA GROUP GOAL PLAN", fTitle));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Generated By : " + Session.getUsername().toUpperCase(), fNorm));
            document.add(new Paragraph("Generated On : " + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), fNorm));
            document.add(new Paragraph(" "));

            for (String line : content.split("\n")) {
                String t = line.trim().replace("╔","").replace("╗","").replace("╚","")
                        .replace("╝","").replace("║","").replace("═","");
                if (!t.isEmpty() && !t.startsWith("──")) {
                    document.add(new Paragraph(t, fNorm));
                } else {
                    document.add(new Paragraph(" "));
                }
            }
            document.close();

            soundSuccess();
            // ✅ FIXED: Just show the message directly using JOptionPane
            JOptionPane.showMessageDialog(this,
                    "✅ Chama plan saved to Desktop:\n" + filename,
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            setStatus("► CHAMA PDF SAVED: " + filename);
            // ✅ Track activity
            trackActivity("PDF_EXPORT");
            // Track PDF report in database
            trackPDFReport("CHAMA_REPORT");

        } catch (Exception ex) {
            soundError();
            JOptionPane.showMessageDialog(this,
                    "❌ Export failed:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= CHAT SCREEN =================
    // ================= CHAT SCREEN WITH LARGER FONTS =================
    private JPanel buildChatScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_BORDER, 1)));

        // ========== HEADER ==========
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 35, 55));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel title = new JLabel("  ◈ AI CHAT ASSISTANT");
        title.setFont(new Font("Courier New", Font.BOLD, 18));  // Increased from 13
        title.setForeground(ATM_AMBER);

        JButton backBtn = atmButton("◄ BACK");
        backBtn.setFont(new Font("Courier New", Font.BOLD, 13));  // Increased
        backBtn.addActionListener(e -> {
            soundBeep();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_MAIN);
        });

        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);

        // ========== CHAT AREA WITH LARGER FONTS ==========
        chatArea = new JTextArea();
        chatArea.setFont(new Font("Courier New", Font.PLAIN, 15));  // Increased from 11
        chatArea.setBackground(new Color(15, 30, 45));
        chatArea.setForeground(new Color(200, 220, 240));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(15, 18, 15, 18));
        chatArea.setText(
                "  ◈ SUPREME MONEY COACH AI CHAT\n" +
                        "  " + "─".repeat(50) + "\n\n" +
                        "  💬  Ask me anything about savings or finance!\n\n" +
                        "  •  How can I save faster?\n" +
                        "  •  Best ways to reduce expenses\n" +
                        "  •  Investment options for beginners\n" +
                        "  •  How to build an emergency fund\n" +
                        "  •  Tips for paying off debt\n"
        );

        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        scroll.getViewport().setBackground(new Color(15, 30, 45));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // ========== INPUT PANEL WITH LARGER FONTS ==========
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(ATM_SCREEN_BG);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ATM_BORDER),
                BorderFactory.createEmptyBorder(10, 15, 12, 15)
        ));

        chatInput = new JTextField();
        chatInput.setFont(new Font("Courier New", Font.PLAIN, 15));  // Increased from 11
        chatInput.setBackground(new Color(15, 30, 45));
        chatInput.setForeground(new Color(200, 220, 240));
        chatInput.setCaretColor(ATM_GREEN);
        chatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        chatInput.addActionListener(e -> sendChatMessage());

        chatSendBtn = atmButton("SEND ►");
        chatSendBtn.setFont(new Font("Courier New", Font.BOLD, 14));  // Increased
        chatSendBtn.setForeground(ATM_GREEN);
        chatSendBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_GREEN, 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        chatSendBtn.addActionListener(e -> { soundBeep(); sendChatMessage(); });

        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(chatSendBtn, BorderLayout.EAST);

        screen.add(header, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(inputPanel, BorderLayout.SOUTH);
        return screen;
    }

    // ================= UPDATED CHAT MESSAGE SENDING =================
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;

        // Format user message with larger font
        chatArea.append("\n  [YOU]: " + message + "\n");
        chatArea.append("  [AI] : 🤔 Thinking...\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
        chatInput.setText("");
        chatSendBtn.setEnabled(false);

        String langInstr = isKiswahili ? "Jibu kwa Kiswahili rahisi na kwa ufupi." : "Reply in English, concisely and helpfully.";

        EXECUTOR.submit(() -> {
            String prompt = "You are Supreme Money Coach AI assistant in Kenya. " + langInstr +
                    " No markdown. User: " + Session.username + ". Question: " + message;
            String response = callGeminiAPI(prompt);
            // ✅ Track activity
            trackActivity("CHAT");

            SwingUtilities.invokeLater(() -> {
                String current = chatArea.getText();
                // Remove the "Thinking..." line
                int lastIndex = current.lastIndexOf("  [AI] : 🤔 Thinking...\n");
                if (lastIndex >= 0) {
                    chatArea.setText(current.substring(0, lastIndex));
                }
                // Add the response with better formatting
                chatArea.append("  [AI] : " + response + "\n");
                chatArea.append("  " + "─".repeat(40) + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
                chatSendBtn.setEnabled(true);
                soundSuccess();
            });
        });
    }



    // ================= NOTIFICATIONS SCREEN =================
    private JPanel buildNotificationsScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_AMBER, 1)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 15, 5));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel("  NOTIFICATIONS");
        title.setFont(new Font("Courier New", Font.BOLD, 18));
        title.setForeground(ATM_AMBER);

        JButton backBtn = atmButton("◄ BACK");
        backBtn.setFont(new Font("Courier New", Font.BOLD, 12));
        backBtn.addActionListener(e -> {
            soundBeep();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_MAIN);
        });
        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);

        JTextArea notifArea = new JTextArea();
        notifArea.setFont(new Font("Courier New", Font.PLAIN, 13));
        notifArea.setBackground(new Color(15, 10, 5));
        notifArea.setForeground(new Color(220, 230, 250));
        notifArea.setEditable(false);
        notifArea.setLineWrap(true);
        notifArea.setWrapStyleWord(true);
        notifArea.setMargin(new Insets(15, 15, 15, 15));

        JScrollPane scroll = new JScrollPane(notifArea);
        scroll.setBorder(BorderFactory.createLineBorder(ATM_AMBER, 1));
        scroll.getViewport().setBackground(new Color(15, 10, 5));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel footer = new JPanel(new GridLayout(1, 2, 15, 0));
        footer.setBackground(new Color(20, 15, 5));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ATM_AMBER),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JButton refreshBtn = atmButton("🔄 REFRESH");
        refreshBtn.setFont(new Font("Courier New", Font.BOLD, 12));
        refreshBtn.setForeground(ATM_CYAN);

        JButton clearBtn = atmButton("🗑 CLEAR ALL");
        clearBtn.setFont(new Font("Courier New", Font.BOLD, 12));
        clearBtn.setForeground(ATM_RED);

        refreshBtn.addActionListener(e -> {
            soundBeep();
            loadNotifications(notifArea);
        });

        clearBtn.addActionListener(e -> {
            soundBeep();
            if (!ATMDialog.confirm(this, "Clear all notifications?")) return;
            EXECUTOR.submit(() -> {
                NotificationService.clearAll(Session.userId);
                SwingUtilities.invokeLater(() -> {
                    loadNotifications(notifArea);
                    updateNotificationBadge();
                    setStatus("► All notifications cleared");
                });
            });
        });

        footer.add(refreshBtn);
        footer.add(clearBtn);

        screen.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                loadNotifications(notifArea);
                EXECUTOR.submit(() -> {
                    NotificationService.markAllRead(Session.userId);
                    SwingUtilities.invokeLater(() -> updateNotificationBadge());
                });
            }
        });

        screen.add(header, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(footer, BorderLayout.SOUTH);
        return screen;
    }


    private void loadNotifications(JTextArea area) {
        EXECUTOR.submit(() -> {
            var list = NotificationService.getAll(Session.userId);
            SwingUtilities.invokeLater(() -> {
                if (list.isEmpty()) {
                    area.setFont(new Font("Courier New", Font.PLAIN, 14));
                    area.setForeground(ATM_GREEN);
                    area.setText("\n\n" +
                            "  ╔══════════════════════════════════════════════════════════════════════╗\n" +
                            "  ║                            📭 NO NOTIFICATIONS                        ║\n" +
                            "  ╠══════════════════════════════════════════════════════════════════════╣\n" +
                            "  ║                                                                      ║\n" +
                            "  ║  Notifications appear when you:                                     ║\n" +
                            "  ║                                                                      ║\n" +
                            "  ║  💰 Make a deposit or withdrawal                                    ║\n" +
                            "  ║  🎯 Reach a savings milestone                                       ║\n" +
                            "  ║  ⚠️ Get a debt reminder                                             ║\n" +
                            "  ║  📝 Receive admin responses                                         ║\n" +
                            "  ║  👥 Chama activities                                                ║\n" +
                            "  ║                                                                      ║\n" +
                            "  ╚══════════════════════════════════════════════════════════════════════╝\n");
                    return;
                }

                StringBuilder sb = new StringBuilder();

                // Count unread
                int unreadCount = 0;
                for (String[] n : list) {
                    if (!Boolean.parseBoolean(n[3])) unreadCount++;
                }

                // ========== HEADER ==========
                sb.append("╔══════════════════════════════════════════════════════════════════════╗\n");
                sb.append("║                         📬 YOUR NOTIFICATIONS                        ║\n");
                sb.append("╠══════════════════════════════════════════════════════════════════════╣\n");
                sb.append(String.format("║  📊 You have %d unread notification%s%44s║\n",
                        unreadCount, unreadCount != 1 ? "s" : "", " "));
                sb.append("╚══════════════════════════════════════════════════════════════════════╝\n\n");

                // ========== NOTIFICATIONS ==========
                int count = 0;
                for (String[] n : list) {
                    count++;
                    boolean isRead = Boolean.parseBoolean(n[3]);

                    // ========== FIXED: No truncation - show full message ==========
                    String message = n[1]; // Full message, NOT truncated

                    // Icon based on type
                    String icon = "📌";
                    String type = n[2];
                    if (type.equals(NotificationService.SUCCESS)) icon = "✅";
                    else if (type.equals(NotificationService.WARNING)) icon = "⚠️";
                    else if (type.equals(NotificationService.ALERT)) icon = "🔴";
                    else if (type.equals(NotificationService.INFO)) icon = "ℹ️";

                    // Unread indicator
                    String marker = isRead ? "   " : " 🔴 NEW";

                    // ========== FIXED: Better formatting with full message ==========
                    sb.append("┌──────────────────────────────────────────────────────────────────┐\n");
                    sb.append(String.format("│ %s %s %-68s │\n", marker, icon, message));
                    sb.append("├──────────────────────────────────────────────────────────────────┤\n");
                    sb.append(String.format("│  📅 %-72s │\n", n[4]));
                    sb.append("└──────────────────────────────────────────────────────────────────┘\n");

                    if (count < list.size()) {
                        sb.append("\n");
                    }
                }

                area.setFont(new Font("Courier New", Font.PLAIN, 12));
                area.setForeground(new Color(220, 230, 250));
                area.setText(sb.toString());
                area.setCaretPosition(0);
            });
        });
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    // ================= DEBT SCREEN =================
    private JPanel buildDebtScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_BORDER, 1)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ATM_SCREEN_BG);
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel("  ◈ DEBT MANAGER  ◈");
        title.setFont(FONT_HEAD);
        title.setForeground(ATM_AMBER);

        JButton backBtn = atmButton("◄ BACK");
        backBtn.addActionListener(e -> {
            soundBeep();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_MAIN);
        });
        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);
        screen.add(header, BorderLayout.NORTH);

        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setBackground(ATM_SCREEN_BG);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel lblTotalIOwe = new JLabel("Ksh 0");
        lblTotalIOwe.setFont(FONT_VALUE);
        lblTotalIOwe.setForeground(ATM_RED);

        JLabel lblTotalTheyOwe = new JLabel("Ksh 0");
        lblTotalTheyOwe.setFont(FONT_VALUE);
        lblTotalTheyOwe.setForeground(ATM_GREEN);

        JLabel lblNetPosition = new JLabel("Ksh 0");
        lblNetPosition.setFont(FONT_VALUE);
        lblNetPosition.setForeground(ATM_AMBER);

        JLabel lblActiveDebts = new JLabel("0");
        lblActiveDebts.setFont(FONT_VALUE);
        lblActiveDebts.setForeground(ATM_BLUE);

        summaryPanel.add(createDebtStatCard("💰 I OWE", lblTotalIOwe, ATM_RED));
        summaryPanel.add(createDebtStatCard("💵 THEY OWE ME", lblTotalTheyOwe, ATM_GREEN));
        summaryPanel.add(createDebtStatCard("📊 NET POSITION", lblNetPosition, ATM_AMBER));
        summaryPanel.add(createDebtStatCard("📋 ACTIVE DEBTS", lblActiveDebts, ATM_BLUE));

        screen.add(summaryPanel, BorderLayout.NORTH);

        String[] columns = {"ID", "Person", "Amount", "Type", "Due Date", "Status", "Paid", "Actions"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable debtTable = buildDebtTable(model);
        JScrollPane scroll = new JScrollPane(debtTable);
        scroll.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        scroll.getViewport().setBackground(ATM_SCREEN_BG);
        screen.add(scroll, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        actionPanel.setBackground(ATM_SCREEN_BG);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        JButton addIOweBtn = createDebtActionButton("+ I OWE", ATM_RED);
        JButton addTheyOweBtn = createDebtActionButton("+ THEY OWE ME", ATM_GREEN);
        JButton makePaymentBtn = createDebtActionButton("💰 MAKE PAYMENT", ATM_AMBER);
        JButton deleteDebtBtn = createDebtActionButton("🗑 DELETE", ATM_RED);
        JButton refreshBtn = createDebtActionButton("🔄 REFRESH", ATM_BLUE);
        JButton reminderBtn = createDebtActionButton("📱 SEND REMINDER", ATM_PURPLE);

        addIOweBtn.addActionListener(e -> showAddDebtDialog("I_OWE", model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts));
        addTheyOweBtn.addActionListener(e -> showAddDebtDialog("THEY_OWE", model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts));
        makePaymentBtn.addActionListener(e -> makeDebtPayment(debtTable, model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts));
        deleteDebtBtn.addActionListener(e -> deleteDebt(debtTable, model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts));
        refreshBtn.addActionListener(e -> loadAllDebts(model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts));
        reminderBtn.addActionListener(e -> sendDebtReminder(debtTable));

        actionPanel.add(addIOweBtn);
        actionPanel.add(addTheyOweBtn);
        actionPanel.add(makePaymentBtn);
        actionPanel.add(deleteDebtBtn);
        actionPanel.add(refreshBtn);
        actionPanel.add(reminderBtn);

        screen.add(actionPanel, BorderLayout.SOUTH);

        screen.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                loadAllDebts(model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts);
            }
        });

        return screen;
    }

    private JPanel createDebtStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 5));
        card.setBackground(new Color(5, 20, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_SMALL);
        titleLabel.setForeground(ATM_GREEN_DIM);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        valueLabel.setFont(FONT_VALUE);
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JButton createDebtActionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(BUTTON_BG);
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(color); btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BUTTON_BG); btn.setForeground(color); }
        });
        return btn;
    }

    private JTable buildDebtTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(new Color(5, 20, 5));
        table.setForeground(ATM_GREEN);
        table.setFont(FONT_SMALL);
        table.setRowHeight(35);
        table.setGridColor(ATM_BORDER);
        table.setSelectionBackground(new Color(0, 130, 85));
        table.setSelectionForeground(Color.WHITE);

        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);
        table.getColumnModel().getColumn(7).setPreferredWidth(80);

        table.getColumnModel().getColumn(2).setCellRenderer(new DebtAmountRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new DebtStatusRenderer());

        return table;
    }

    class DebtAmountRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String type = (String) table.getValueAt(row, 3);
            setForeground("I OWE".equals(type) ? ATM_RED : ATM_GREEN);
            return c;
        }
    }

    class DebtStatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;
            if ("PAID".equals(status)) setForeground(ATM_GREEN);
            else if ("PARTIAL".equals(status)) setForeground(ATM_AMBER);
            else setForeground(ATM_RED);
            return c;
        }
    }

    private void loadAllDebts(DefaultTableModel model, JLabel lblTotalIOwe, JLabel lblTotalTheyOwe,
                              JLabel lblNetPosition, JLabel lblActiveDebts) {
        EXECUTOR.submit(() -> {
            model.setRowCount(0);
            double totalIOwe = 0, totalTheyOwe = 0;
            int activeCount = 0;

            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT id, person_name, amount, type, due_date, status, paid_amount " +
                                 "FROM debts WHERE user_id = ? AND status != 'PAID' ORDER BY due_date ASC")) {
                pst.setInt(1, Session.userId);
                ResultSet rs = pst.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String person = rs.getString("person_name");
                    double amount = rs.getDouble("amount");
                    String type = rs.getString("type");
                    String dueDate = rs.getString("due_date");
                    String status = rs.getString("status");
                    double paidAmount = rs.getDouble("paid_amount");

                    String displayType = type.equals("I_OWE") ? "I OWE" : "THEY OWE";
                    String displayPaid = paidAmount > 0 ? "Ksh " + String.format("%,.0f", paidAmount) : "-";

                    if (type.equals("I_OWE")) totalIOwe += amount;
                    else totalTheyOwe += amount;
                    activeCount++;

                    final int fId = id;
                    final String fPerson = person;
                    final double fAmount = amount;
                    final String fDisplayType = displayType;
                    final String fDueDate = dueDate != null ? dueDate : "No date";
                    final String fStatus = status;
                    final String fDisplayPaid = displayPaid;

                    SwingUtilities.invokeLater(() -> {
                        model.addRow(new Object[]{fId, fPerson, "Ksh " + String.format("%,.0f", fAmount),
                                fDisplayType, fDueDate, fStatus, fDisplayPaid, "Pay/View"});
                    });
                }
            } catch (SQLException e) { e.printStackTrace(); }

            double netPosition = totalTheyOwe - totalIOwe;
            int finalActiveCount = activeCount;
            double finalTotalIOwe = totalIOwe;
            double finalTotalTheyOwe = totalTheyOwe;
            SwingUtilities.invokeLater(() -> {
                lblTotalIOwe.setText("Ksh " + String.format("%,.0f", finalTotalIOwe));
                lblTotalTheyOwe.setText("Ksh " + String.format("%,.0f", finalTotalTheyOwe));
                lblNetPosition.setText((netPosition >= 0 ? "+ " : "- ") + "Ksh " + String.format("%,.0f", Math.abs(netPosition)));
                lblActiveDebts.setText(String.valueOf(finalActiveCount));
            });
        });
    }

    private void showAddDebtDialog(String type, DefaultTableModel model, JLabel lblTotalIOwe,
                                   JLabel lblTotalTheyOwe, JLabel lblNetPosition, JLabel lblActiveDebts) {
        JDialog dialog = new JDialog(this, type.equals("I_OWE") ? "Add Debt - I OWE" : "Add Debt - THEY OWE ME", true);
        dialog.setSize(480, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(20, 25, 25, 25)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel(type.equals("I_OWE") ? "💰 ADD DEBT - I OWE" : "💰 ADD DEBT - THEY OWE ME");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(ATM_AMBER);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, gbc);

        // Reset gridwidth
        gbc.gridwidth = 1;

        // Person Name
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel personLabel = new JLabel("👤 Person Name:");
        personLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        personLabel.setForeground(ATM_GREEN);
        panel.add(personLabel, gbc);

        gbc.gridx = 1;
        JTextField personField = new JTextField(20);
        styleTextField(personField);
        panel.add(personField, gbc);

        // Amount
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel amountLabel = new JLabel("💰 Amount (Ksh):");
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        amountLabel.setForeground(ATM_GREEN);
        panel.add(amountLabel, gbc);

        gbc.gridx = 1;
        JTextField amountField = new JTextField(20);
        styleTextField(amountField);
        panel.add(amountField, gbc);

        // Due Date
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel dueLabel = new JLabel("📅 Due Date (YYYY-MM-DD):");
        dueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dueLabel.setForeground(ATM_GREEN);
        panel.add(dueLabel, gbc);

        gbc.gridx = 1;
        JTextField dueDateField = new JTextField(LocalDate.now().plusDays(30).toString(), 20);
        styleTextField(dueDateField);
        panel.add(dueDateField, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel descLabel = new JLabel("📝 Description:");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        descLabel.setForeground(ATM_GREEN);
        panel.add(descLabel, gbc);

        gbc.gridx = 1;
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setBackground(new Color(15, 30, 45));
        descArea.setForeground(ATM_GREEN);
        descArea.setCaretColor(ATM_GREEN);
        descArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(null);
        panel.add(descScroll, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);

        JButton saveBtn = new JButton("✓ SAVE DEBT");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setBackground(ATM_GREEN);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        JButton cancelBtn = new JButton("✗ CANCEL");
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cancelBtn.setBackground(new Color(60, 65, 70));
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setBorder(BorderFactory.createLineBorder(ATM_RED, 1));
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(9, 24, 9, 24));

        saveBtn.addActionListener(e -> {
            String person = personField.getText().trim();
            String amountStr = amountField.getText().trim();
            String dueDate = dueDateField.getText().trim();
            String description = descArea.getText().trim();

            if (person.isEmpty()) {
                ATMDialog.error(this, "⚠️ Please enter person's name");
                personField.requestFocus();
                return;
            }

            if (amountStr.isEmpty()) {
                ATMDialog.error(this, "⚠️ Please enter amount");
                amountField.requestFocus();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    ATMDialog.error(this, "⚠️ Amount must be greater than 0");
                    amountField.requestFocus();
                    return;
                }

                if (dueDate.isEmpty()) {
                    ATMDialog.error(this, "⚠️ Please enter due date");
                    dueDateField.requestFocus();
                    return;
                }

                // Validate due date format
                try {
                    LocalDate.parse(dueDate);
                } catch (Exception ex) {
                    ATMDialog.error(this, "⚠️ Invalid date format. Use YYYY-MM-DD");
                    dueDateField.requestFocus();
                    return;
                }

                saveDebtToDatabase(person, amount, type, dueDate, description);
                dialog.dispose();
                loadAllDebts(model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts);
                soundSuccess();
                setStatus("► Debt added: " + person);

            } catch (NumberFormatException ex) {
                ATMDialog.error(this, "⚠️ Please enter a valid number for amount");
                amountField.requestFocus();
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }
    private void forceRefreshUI() {
        SwingUtilities.invokeLater(() -> {
            // Refresh all components
            refreshAll();
            refreshChamaData();
            refreshMyChamasPanel();
            refreshChamaList();

            // Force complete repaint
            contentArea.revalidate();
            contentArea.repaint();
            sidebar.revalidate();
            sidebar.repaint();

            System.out.println("✅ UI completely refreshed");
        });
    }

    private void saveDebtToDatabase(String person, double amount, String type, String dueDate, String description) {
        EXECUTOR.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "INSERT INTO debts (user_id, person_name, amount, type, description, due_date, status) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')")) {
                pst.setInt(1, Session.userId);
                pst.setString(2, person);
                pst.setDouble(3, amount);
                pst.setString(4, type);
                pst.setString(5, description);
                if (dueDate != null && !dueDate.isEmpty()) {
                    pst.setDate(6, java.sql.Date.valueOf(dueDate));
                } else {
                    pst.setNull(6, java.sql.Types.DATE);
                }
                pst.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> ATMDialog.error(this, "Failed to save debt: " + e.getMessage()));
            }
        });
    }

    private void makeDebtPayment(JTable table, DefaultTableModel model, JLabel lblTotalIOwe,
                                 JLabel lblTotalTheyOwe, JLabel lblNetPosition, JLabel lblActiveDebts) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            ATMDialog.warning(this, "Select a debt to make payment");
            return;
        }

        int debtId = (int) model.getValueAt(selectedRow, 0);
        String person = (String) model.getValueAt(selectedRow, 1);
        String amountStr = (String) model.getValueAt(selectedRow, 2);
        String type = (String) model.getValueAt(selectedRow, 3);

        double amount = Double.parseDouble(amountStr.replace("Ksh", "").replace(",", "").trim());

        if ("I OWE".equals(type)) {
            makePaymentForDebt(debtId, person, amount);
        } else {
            recordPaymentReceived(debtId, person, amount);
        }

        loadAllDebts(model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts);
    }

    private void makePaymentForDebt(int debtId, String person, double totalAmount) {
        String paymentStr = JOptionPane.showInputDialog(this,
                "Enter payment amount for " + person + ":\nTotal Debt: Ksh " + String.format("%,.0f", totalAmount),
                "Make Payment", JOptionPane.QUESTION_MESSAGE);

        if (paymentStr == null) return;

        try {
            double payment = Double.parseDouble(paymentStr.trim());
            if (payment <= 0 || payment > totalAmount) {
                ATMDialog.error(this, "Invalid payment amount");
                return;
            }

            String[] methods = {"CASH", "MPESA", "BANK TRANSFER", "CHEQUE"};
            String method = (String) JOptionPane.showInputDialog(this,
                    "Select payment method:", "Payment Method",
                    JOptionPane.QUESTION_MESSAGE, null, methods, methods[0]);

            if (method == null) return;

            double newAmount = totalAmount - payment;
            String newStatus = (newAmount <= 0) ? "PAID" : "PARTIAL";

            EXECUTOR.submit(() -> {
                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "UPDATE debts SET amount = ?, status = ?, payment_method = ?, paid_amount = ?, paid_date = NOW() "
                                     + "WHERE id = ? AND user_id = ?")) {
                    pst.setDouble(1, newAmount);
                    pst.setString(2, newStatus);
                    pst.setString(3, method);
                    pst.setDouble(4, payment);
                    pst.setInt(5, debtId);
                    pst.setInt(6, Session.userId);
                    pst.executeUpdate();

                    soundSuccess();
                    String message = newAmount <= 0 ?
                            "✓ Debt fully paid! You paid Ksh " + String.format("%,.0f", payment) + " to " + person :
                            "✓ Payment recorded! Paid Ksh " + String.format("%,.0f", payment) +
                                    ". Remaining: Ksh " + String.format("%,.0f", newAmount);
                    NotificationService.create(Session.userId, message, NotificationService.SUCCESS);
                    SwingUtilities.invokeLater(() -> {
                        ATMDialog.success(this, message, "Export Complete");
                        setStatus(message);
                    });
                } catch (SQLException e) {
                    soundError();
                    SwingUtilities.invokeLater(() -> ATMDialog.error(this, "Failed to record payment: " + e.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            ATMDialog.error(this, "Please enter a valid amount");
        }
    }

    private void recordPaymentReceived(int debtId, String person, double totalAmount) {
        String amountStr = JOptionPane.showInputDialog(this,
                "Enter amount received from " + person + ":\nTotal Owed: Ksh " + String.format("%,.0f", totalAmount),
                "Record Payment Received", JOptionPane.QUESTION_MESSAGE);

        if (amountStr == null) return;

        try {
            double received = Double.parseDouble(amountStr.trim());
            if (received <= 0 || received > totalAmount) {
                ATMDialog.error(this, "Invalid amount");
                return;
            }

            String[] methods = {"CASH", "MPESA", "BANK TRANSFER", "CHEQUE"};
            String method = (String) JOptionPane.showInputDialog(this,
                    "Select payment method received:", "Payment Method",
                    JOptionPane.QUESTION_MESSAGE, null, methods, methods[0]);

            if (method == null) return;

            double newAmount = totalAmount - received;
            String newStatus = (newAmount <= 0) ? "PAID" : "PARTIAL";

            EXECUTOR.submit(() -> {
                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "UPDATE debts SET amount = ?, status = ?, payment_method = ?, paid_amount = ?, paid_date = NOW() "
                                     + "WHERE id = ? AND user_id = ?")) {
                    pst.setDouble(1, newAmount);
                    pst.setString(2, newStatus);
                    pst.setString(3, method);
                    pst.setDouble(4, received);
                    pst.setInt(5, debtId);
                    pst.setInt(6, Session.userId);
                    pst.executeUpdate();

                    soundSuccess();
                    String message = newAmount <= 0 ?
                            "✓ Debt fully cleared! Received Ksh " + String.format("%,.0f", received) + " from " + person :
                            "✓ Payment recorded! Received Ksh " + String.format("%,.0f", received) +
                                    " from " + person + ". Remaining: Ksh " + String.format("%,.0f", newAmount);
                    NotificationService.create(Session.userId, message, NotificationService.SUCCESS);
                    SwingUtilities.invokeLater(() -> {
                        ATMDialog.success(this, message, "Export Complete");
                        setStatus(message);
                    });
                } catch (SQLException e) {
                    soundError();
                    SwingUtilities.invokeLater(() -> ATMDialog.error(this, "Failed to record payment: " + e.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            ATMDialog.error(this, "Please enter a valid amount");
        }
    }

    private void deleteDebt(JTable table, DefaultTableModel model, JLabel lblTotalIOwe,
                            JLabel lblTotalTheyOwe, JLabel lblNetPosition, JLabel lblActiveDebts) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            ATMDialog.warning(this, "Select a debt to delete");
            return;
        }

        int debtId = (int) model.getValueAt(selectedRow, 0);
        String person = (String) model.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete debt for " + person + "?\n\nThis action cannot be undone!",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            EXECUTOR.submit(() -> {
                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement("DELETE FROM debts WHERE id = ? AND user_id = ?")) {
                    pst.setInt(1, debtId);
                    pst.setInt(2, Session.userId);
                    pst.executeUpdate();

                    soundSuccess();
                    SwingUtilities.invokeLater(() -> {
                        ATMDialog.success(this, "Debt deleted successfully", "Export Complete");
                        loadAllDebts(model, lblTotalIOwe, lblTotalTheyOwe, lblNetPosition, lblActiveDebts);
                        setStatus("► Debt deleted for " + person);
                    });
                } catch (SQLException e) {
                    soundError();
                    SwingUtilities.invokeLater(() -> ATMDialog.error(this, "Failed to delete debt: " + e.getMessage()));
                }
            });
        }
    }

    private void sendDebtReminder(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            ATMDialog.warning(this, "Select a debt to send reminder");
            return;
        }

        int debtId = (int) table.getValueAt(selectedRow, 0);
        String person = (String) table.getValueAt(selectedRow, 1);
        String type = (String) table.getValueAt(selectedRow, 3);

        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT d.*, u.phone_number FROM debts d JOIN users u ON d.user_id = u.id "
                             + "WHERE d.id = ? AND d.user_id = ?")) {
            pst.setInt(1, debtId);
            pst.setInt(2, Session.userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                double amount = rs.getDouble("amount");
                String dueDate = rs.getString("due_date");

                String message;
                if ("I OWE".equals(type)) {
                    message = "🔔 REMINDER: You owe " + person + " Ksh " + String.format("%,.0f", amount) +
                            ". Due: " + (dueDate != null ? dueDate : "Not set") + ". Please clear your debt.";
                } else {
                    message = "🔔 REMINDER: " + person + " owes you Ksh " + String.format("%,.0f", amount) +
                            ". Due: " + (dueDate != null ? dueDate : "Not set") + ". Follow up on this debt.";
                }

                NotificationService.create(Session.userId, message, NotificationService.WARNING);
                ATMDialog.success(this, "Reminder notification sent to your notifications panel.", "Export Complete");
                setStatus("► Reminder sent for debt: " + person);
            }
        } catch (SQLException e) {
            soundError();
            ATMDialog.error(this, "Failed to send reminder: " + e.getMessage());
        }
    }

    // ================= REQUEST SCREEN =================
    private JPanel buildRequestScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_AMBER, 1)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 15, 5));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("  ◈ SEND REQUEST TO ADMIN");
        title.setFont(new Font("Courier New", Font.BOLD, 13));
        title.setForeground(ATM_AMBER);

        JButton backBtn = atmButton("◄ BACK");
        backBtn.addActionListener(e -> {
            soundBeep();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_MAIN);
        });

        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ATM_SCREEN_BG);
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] types = {"Account Issue", "Withdrawal Problem", "Deposit Dispute",
                "Goal Change Request", "Loan Inquiry", "General Inquiry", "Other"};
        JComboBox<String> typeBox = new JComboBox<>(types);
        typeBox.setBackground(new Color(5, 20, 5));
        typeBox.setForeground(ATM_GREEN);
        typeBox.setFont(FONT_SMALL);
        typeBox.setBorder(BorderFactory.createLineBorder(ATM_BORDER));

        JTextArea descArea = new JTextArea(4, 35);
        descArea.setBackground(new Color(5, 20, 5));
        descArea.setForeground(ATM_GREEN);
        descArea.setCaretColor(ATM_GREEN);
        descArea.setFont(FONT_SMALL);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(null);
        descScroll.getViewport().setBackground(new Color(5, 20, 5));

        JTextArea histArea = new JTextArea(6, 35);
        histArea.setBackground(new Color(5, 15, 5));
        histArea.setForeground(ATM_GREEN_DIM);
        histArea.setFont(FONT_SMALL);
        histArea.setEditable(false);
        histArea.setBorder(BorderFactory.createLineBorder(ATM_BORDER));

        JButton submitBtn = atmButton("◉ SUBMIT REQUEST");
        submitBtn.setForeground(ATM_AMBER);
        submitBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        loadRequestHistory(histArea);

        submitBtn.addActionListener(e -> {
            soundBeep();
            String rtype = (String) typeBox.getSelectedItem();
            String desc = descArea.getText().trim();

            if (desc.isEmpty()) {
                ATMDialog.error(this, "Please describe your request before submitting.");
                return;
            }

            EXECUTOR.submit(() -> {
                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "INSERT INTO admin_requests(user_id, request_type, description) VALUES(?,?,?)")) {
                    pst.setInt(1, Session.userId);
                    pst.setString(2, rtype);
                    pst.setString(3, desc);
                    int rows = pst.executeUpdate();

                    SwingUtilities.invokeLater(() -> {
                        if (rows > 0) {
                            soundSuccess();
                            descArea.setText("");
                            loadRequestHistory(histArea);
                            ATMDialog.success(this, "Request submitted!\nAdmin will review and respond via notification.", "Export Complete");
                            setStatus("► REQUEST SUBMITTED  [" + LocalDateTime.now().format(DT_FORMATTER) + "]");
                        } else {
                            soundError();
                            ATMDialog.error(this, "Submission failed. Please try again.");
                        }
                    });
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        soundError();
                        ATMDialog.error(this, "Submission failed:\n" + ex.getMessage());
                    });
                }
            });
        });

        JLabel typeLabel = new JLabel("Request Type:");
        typeLabel.setFont(FONT_LABEL);
        typeLabel.setForeground(ATM_GREEN_DIM);

        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(FONT_LABEL);
        descLabel.setForeground(ATM_GREEN_DIM);

        JLabel histLabel = new JLabel("Your Past Requests:");
        histLabel.setFont(FONT_LABEL);
        histLabel.setForeground(ATM_AMBER);

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(typeLabel, gbc);
        gbc.gridx = 1;
        form.add(typeBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(descLabel, gbc);
        gbc.gridx = 1;
        form.add(descScroll, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        form.add(submitBtn, gbc);

        gbc.gridy = 3;
        form.add(histLabel, gbc);

        JScrollPane hs = new JScrollPane(histArea);
        hs.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        hs.getViewport().setBackground(new Color(5, 15, 5));
        gbc.gridy = 4;
        form.add(hs, gbc);

        screen.add(header, BorderLayout.NORTH);
        screen.add(form, BorderLayout.CENTER);
        return screen;
    }

    private void loadRequestHistory(JTextArea area) {
        EXECUTOR.submit(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  %-5s %-20s %-10s %-15s%n", "ID", "TYPE", "STATUS", "RESPONSE DATE"));
            sb.append("  ─────────────────────────────────────────────────────────────\n");

            try (Connection conn = SecureDatabaseConnection.connect()) {
                boolean hasAdminNote = false;
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "admin_requests", "admin_note")) {
                    hasAdminNote = rs.next();
                } catch (SQLException ignored) {}

                String sql = hasAdminNote ?
                        "SELECT id, request_type, status, COALESCE(admin_note,'—') as note, responded_at " +
                                "FROM admin_requests WHERE user_id=? ORDER BY id DESC LIMIT 10" :
                        "SELECT id, request_type, status, '—' as note, responded_at " +
                                "FROM admin_requests WHERE user_id=? ORDER BY id DESC LIMIT 10";

                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setInt(1, Session.getUserId());
                    ResultSet rs = pst.executeQuery();
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        int id = rs.getInt("id");
                        String type = rs.getString("request_type");
                        String status = rs.getString("status");
                        String note = rs.getString("note");
                        Timestamp respondedAt = rs.getTimestamp("responded_at");
                        String responseDate = respondedAt != null ? respondedAt.toString().substring(0, 10) : "—";

                        sb.append(String.format("  %-5d %-20s %-10s %-15s%n", id, type, status, responseDate));
                        if (!note.equals("—")) {
                            sb.append("       📝 Response: ").append(note).append("\n");
                        }
                        sb.append("  ─────────────────────────────────────────────────────────────\n");
                    }
                    if (!any) {
                        sb.append("  No requests submitted yet.\n\n");
                        sb.append("  You can submit requests for:\n");
                        sb.append("  • Account Issues\n");
                        sb.append("  • Withdrawal Problems\n");
                        sb.append("  • Deposit Disputes\n");
                        sb.append("  • Goal Change Requests\n");
                        sb.append("  • Loan Inquiries\n");
                        sb.append("  • General Questions\n");
                    }
                }
            } catch (SQLException ex) {
                sb.append("  Error loading requests: ").append(ex.getMessage()).append("\n");
            }

            final String text = sb.toString();
            SwingUtilities.invokeLater(() -> area.setText(text));
        });
    }

    // ================= CHAMA MANAGEMENT SCREEN =================
    private JPanel buildChamaManagementScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_PURPLE, 1)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 5, 25));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("  ◈ CHAMA MANAGEMENT");
        title.setFont(new Font("Courier New", Font.BOLD, 13));
        title.setForeground(ATM_PURPLE);

        JButton backBtn = atmButton("◄ BACK");
        backBtn.addActionListener(e -> {
            soundBeep();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_MAIN);
        });

        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ATM_SCREEN_BG);
        tabbedPane.setForeground(ATM_GREEN);
        tabbedPane.setFont(FONT_SMALL);

        tabbedPane.addTab("📋 My Chamas", buildMyChamasPanel());
        tabbedPane.addTab("➕ Create Chama", buildCreateChamaPanel());
        tabbedPane.addTab("👥 Add Members", buildAddMembersPanel());  // NEW TAB
        tabbedPane.addTab("🔑 Join Chama", buildJoinChamaPanel());

        screen.add(header, BorderLayout.NORTH);
        screen.add(tabbedPane, BorderLayout.CENTER);
        return screen;
    }

    // ================= MY CHAMAS PANEL =================
    private JPanel buildMyChamasPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        DefaultListModel<String> chamaListModel = new DefaultListModel<>();
        JList<String> chamaList = new JList<>(chamaListModel);
        chamaList.setBackground(ATM_SCREEN_BG);
        chamaList.setForeground(ATM_GREEN);
        chamaList.setFont(FONT_SMALL);

        List<Map<String, Object>> userChamas = ChamaGroup.getUserChamas(Session.userId);
        chamaMap.clear();
        for (Map<String, Object> chama : userChamas) {
            String role = (String) chama.get("role");
            String status = (String) chama.get("status");
            String display = chama.get("group_name") + " (" + role + ")" +
                    ("PENDING".equals(status) ? " [AWAITING APPROVAL]" : "");
            chamaListModel.addElement(display);
            chamaMap.put(chamaListModel.getSize() - 1, chama);
        }

        if (chamaListModel.isEmpty()) {
            chamaListModel.addElement("No Chamas found. Create or join a Chama!");
        }

        JScrollPane scroll = new JScrollPane(chamaList);
        scroll.setBorder(BorderFactory.createLineBorder(ATM_PURPLE));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBackground(ATM_SCREEN_BG);
        JButton manualRefreshBtn = atmButton("🔄 Refresh List");
        manualRefreshBtn.setForeground(ATM_CYAN);
        manualRefreshBtn.addActionListener(e -> {
            refreshChamaList();
            ATMDialog.success(this, "Chama list refreshed!", "Export Complete");
        });
        topPanel.add(manualRefreshBtn);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        actionPanel.setBackground(ATM_SCREEN_BG);

        JButton exportReportBtn = atmButton("📄 Export Report");
        exportReportBtn.setForeground(ATM_AMBER);
        exportReportBtn.addActionListener(e -> {
            int selected = chamaList.getSelectedIndex();
            if (selected >= 0 && selected < chamaMap.size()) {
                Map<String, Object> chama = chamaMap.get(selected);
                int chamaId = (Integer) chama.get("id");
                String groupName = (String) chama.get("group_name");

                int confirm = JOptionPane.showConfirmDialog(this,
                        "Generate full financial report for\n" + groupName + "?",
                        "Export Report", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    String timestamp = LocalDateTime.now().format(DT_FORMATTER).replace(":", "-").replace(" ", "_");
                    String filename = getReportsDirectory() + File.separator + "Chama_" + groupName.replace(" ", "_") + "_Report_" + timestamp + ".pdf";

                    EXECUTOR.submit(() -> {
                        ChamaGroup.generateChamaReport(chamaId, filename);
                        SwingUtilities.invokeLater(() -> {
                            soundSuccess();
                            ATMDialog.success(ATMDashboard.this,
                                    "✓ Report generated!\n\n" + filename, "Export Complete");
                            setStatus("► Chama report saved: " + filename);
                        });
                    });
                }
            } else {
                ATMDialog.warning(this, "Select a Chama first");
            }
        });

        JButton viewDetailsBtn = atmButton("📊 View Details");
        viewDetailsBtn.addActionListener(e -> {
            int selected = chamaList.getSelectedIndex();
            if (selected >= 0 && selected < chamaMap.size()) {
                int chamaId = (Integer) chamaMap.get(selected).get("id");
                showChamaDetails(chamaId);
            } else {
                ATMDialog.warning(this, "Select a Chama first");
            }
        });

        JButton pendingRequestsBtn = atmButton("⏳ Pending Requests");
        pendingRequestsBtn.setForeground(ATM_AMBER);
        pendingRequestsBtn.addActionListener(e -> {
            int selected = chamaList.getSelectedIndex();
            if (selected >= 0 && selected < chamaMap.size()) {
                String role = (String) chamaMap.get(selected).get("role");
                if ("LEADER".equals(role)) {
                    int chamaId = (Integer) chamaMap.get(selected).get("id");
                    String groupName = (String) chamaMap.get(selected).get("group_name");
                    showPendingRequests(chamaId, groupName);
                } else {
                    ATMDialog.warning(this, "Only Chama leader can view pending requests");
                }
            }
        });

        JButton recordPaymentBtn = atmButton("💰 Record Payment");
        recordPaymentBtn.addActionListener(e -> {
            int selected = chamaList.getSelectedIndex();
            if (selected >= 0 && selected < chamaMap.size()) {
                String role = (String) chamaMap.get(selected).get("role");
                String status = (String) chamaMap.get(selected).get("status");

                if ("LEADER".equals(role) || "APPROVED".equals(status)) {
                    int chamaId = (Integer) chamaMap.get(selected).get("id");
                    String groupName = (String) chamaMap.get(selected).get("group_name");

                    JDialog paymentDialog = new JDialog(this, "Record Payment", true);
                    paymentDialog.setSize(450, 250);
                    paymentDialog.setLocationRelativeTo(this);
                    paymentDialog.setUndecorated(true);

                    JPanel dialogPanel = new JPanel(new BorderLayout(15, 15));
                    dialogPanel.setBackground(ATM_SCREEN_BG);
                    dialogPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ATM_AMBER, 2),
                            BorderFactory.createEmptyBorder(20, 20, 20, 20)));

                    JLabel titleLabel = new JLabel("◈ RECORD PAYMENT ◈", SwingConstants.CENTER);
                    titleLabel.setFont(new Font("Courier New", Font.BOLD, 18));
                    titleLabel.setForeground(ATM_AMBER);

                    JLabel chamaLabel = new JLabel("Chama: " + groupName, SwingConstants.CENTER);
                    chamaLabel.setFont(FONT_BODY);
                    chamaLabel.setForeground(ATM_GREEN);

                    dialogPanel.add(titleLabel, BorderLayout.NORTH);
                    dialogPanel.add(chamaLabel, BorderLayout.CENTER);

                    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 15, 0));
                    buttonPanel.setBackground(ATM_SCREEN_BG);

                    JButton myselfBtn = new JButton("👤 PAY FOR MYSELF");
                    myselfBtn.setFont(new Font("Courier New", Font.BOLD, 12));
                    myselfBtn.setBackground(BUTTON_BG);
                    myselfBtn.setForeground(ATM_GREEN);
                    myselfBtn.addActionListener(ev -> {
                        paymentDialog.dispose();

                        // Show amount input dialog
                        String amountStr = JOptionPane.showInputDialog(ATMDashboard.this,
                                "Enter amount to pay for yourself:\nChama: " + groupName,
                                "Pay for Myself", JOptionPane.QUESTION_MESSAGE);

                        if (amountStr == null || amountStr.trim().isEmpty()) return;

                        try {
                            double amount = Double.parseDouble(amountStr.trim());
                            if (amount <= 0) {
                                ATMDialog.error(ATMDashboard.this, "Amount must be greater than 0");
                                return;
                            }

                            String[] methods = {"CASH", "MPESA", "BANK TRANSFER", "CHEQUE"};
                            String method = (String) JOptionPane.showInputDialog(ATMDashboard.this,
                                    "Select payment method:", "Payment Method",
                                    JOptionPane.QUESTION_MESSAGE, null, methods, methods[0]);

                            if (method == null) return;

                            // Check if user is a registered member
                            boolean isRegistered = checkIfRegisteredMember(chamaId, Session.getUserId());

                            if (isRegistered) {
                                recordPaymentForMember(chamaId, Session.getUserId(), Session.getUsername(), amount, method);
                            } else {
                                // Check if user is a simple member
                                SimpleMember simpleMember = getSimpleMemberByUserId(chamaId, Session.getUserId());
                                if (simpleMember != null) {
                                    recordPaymentForSimpleMember(chamaId, simpleMember.getId(),
                                            simpleMember.getFullname(), amount, method);
                                } else {
                                    ATMDialog.warning(ATMDashboard.this, "You are not a member of this Chama");
                                }
                            }

                        } catch (NumberFormatException ex) {
                            ATMDialog.error(ATMDashboard.this, "Please enter a valid amount");
                        }
                    });
                    JButton otherBtn = new JButton("👥 PAY FOR ANOTHER");
                    otherBtn.setFont(new Font("Courier New", Font.BOLD, 12));
                    otherBtn.setBackground(BUTTON_BG);
                    otherBtn.setForeground(ATM_PURPLE);
                    otherBtn.addActionListener(ev -> {
                        if ("LEADER".equals(role)) {
                            paymentDialog.dispose();
                            // Show dialog with ALL members (registered + simple)
                            showRecordPaymentForMemberDialog(chamaId, groupName);
                        } else {
                            ATMDialog.warning(ATMDashboard.this, "Only Chama leader can record payments for other members");
                            paymentDialog.dispose();
                        }
                    });
                    buttonPanel.add(myselfBtn);
                    buttonPanel.add(otherBtn);
                    dialogPanel.add(buttonPanel, BorderLayout.CENTER);

                    JButton closeBtn = new JButton("✖ CLOSE");
                    closeBtn.addActionListener(ev -> paymentDialog.dispose());
                    JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                    closePanel.add(closeBtn);
                    dialogPanel.add(closePanel, BorderLayout.SOUTH);

                    paymentDialog.setContentPane(dialogPanel);
                    paymentDialog.setVisible(true);
                } else {
                    ATMDialog.warning(this, "You can only make payments after your membership is approved");
                }
            } else {
                ATMDialog.warning(this, "Select a Chama first");
            }
        });

        JButton removeMemberBtn = atmButton("❌ Remove Member");
        removeMemberBtn.setForeground(ATM_RED);
        removeMemberBtn.addActionListener(e -> {
            int selected = chamaList.getSelectedIndex();
            if (selected >= 0 && selected < chamaMap.size()) {
                String role = (String) chamaMap.get(selected).get("role");
                if ("LEADER".equals(role)) {
                    int chamaId = (Integer) chamaMap.get(selected).get("id");
                    removeMemberFromChama(chamaId);
                } else {
                    ATMDialog.warning(this, "Only Chama leader can remove members");
                }
            }
        });

        JButton deleteChamaBtn = atmButton("🗑 Delete Chama");
        deleteChamaBtn.setForeground(ATM_RED);
        deleteChamaBtn.addActionListener(e -> {
            int selected = chamaList.getSelectedIndex();
            if (selected >= 0 && selected < chamaMap.size()) {
                String role = (String) chamaMap.get(selected).get("role");
                if ("LEADER".equals(role)) {
                    int chamaId = (Integer) chamaMap.get(selected).get("id");
                    String groupName = (String) chamaMap.get(selected).get("group_name");

                    int confirm = JOptionPane.showConfirmDialog(ATMDashboard.this,
                            "⚠️ PERMANENT DELETION ⚠️\n\nDelete Chama '" + groupName + "'?\n\n" +
                                    "This action CANNOT be undone!",
                            "Confirm Permanent Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        EXECUTOR.submit(() -> {
                            boolean deleted = ChamaGroup.deleteChama(chamaId, Session.userId);
                            SwingUtilities.invokeLater(() -> {
                                if (deleted) {
                                    soundSuccess();
                                    ATMDialog.success(ATMDashboard.this, "✓ Chama '" + groupName + "' deleted.", "Export Complete");
                                    refreshChamaManagementScreen();
                                } else {
                                    soundError();
                                    ATMDialog.error(ATMDashboard.this, "Failed to delete Chama.");
                                }
                            });
                        });
                    }
                } else {
                    ATMDialog.warning(ATMDashboard.this, "Only Chama leader can delete the Chama");
                }
            }
        });

        actionPanel.add(exportReportBtn);
        actionPanel.add(viewDetailsBtn);
        actionPanel.add(pendingRequestsBtn);
        actionPanel.add(recordPaymentBtn);
        actionPanel.add(removeMemberBtn);
        actionPanel.add(deleteChamaBtn);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        return panel;
    }
    private boolean checkIfRegisteredMember(int chamaId, int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT COUNT(*) FROM chama_members WHERE chama_id = ? AND user_id = ? AND status = 'APPROVED'")) {
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

    private SimpleMember getSimpleMemberByUserId(int chamaId, int userId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT phone_number FROM users WHERE id = ?")) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String phone = rs.getString("phone_number");
                if (phone != null && !phone.isEmpty()) {
                    List<SimpleMember> members = ChamaSimpleMemberManager.getSimpleMembers(chamaId);
                    for (SimpleMember sm : members) {
                        if (phone.equals(sm.getPhoneNumber())) {
                            return sm;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ================= CREATE CHAMA PANEL =================
    private JPanel buildCreateChamaPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("➕ CREATE NEW CHAMA");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(ATM_AMBER);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        // Chama Name
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel nameLabel = new JLabel("📛 Chama Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(ATM_GREEN);
        panel.add(nameLabel, gbc);

        gbc.gridx = 1;
        JTextField groupNameField = new JTextField(20);
        styleTextField(groupNameField);
        panel.add(groupNameField, gbc);

        // Goal Amount
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel goalLabel = new JLabel("💰 Total Goal (Ksh):");
        goalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        goalLabel.setForeground(ATM_GREEN);
        panel.add(goalLabel, gbc);

        gbc.gridx = 1;
        JTextField goalField = new JTextField(20);
        styleTextField(goalField);
        panel.add(goalField, gbc);

        // Frequency
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel freqLabel = new JLabel("📅 Contribution Frequency:");
        freqLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        freqLabel.setForeground(ATM_GREEN);
        panel.add(freqLabel, gbc);

        gbc.gridx = 1;
        JComboBox<String> frequencyCombo = new JComboBox<>(new String[]{"DAILY", "WEEKLY", "MONTHLY", "YEARLY"});
        styleComboBox(frequencyCombo);
        panel.add(frequencyCombo, gbc);

        // Start Date
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel startLabel = new JLabel("📆 Start Date (YYYY-MM-DD):");
        startLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startLabel.setForeground(ATM_GREEN);
        panel.add(startLabel, gbc);

        gbc.gridx = 1;
        JTextField startDateField = new JTextField(LocalDate.now().toString(), 15);
        styleTextField(startDateField);
        panel.add(startDateField, gbc);

        // End Date
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel endLabel = new JLabel("📆 End Date (YYYY-MM-DD):");
        endLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        endLabel.setForeground(ATM_GREEN);
        panel.add(endLabel, gbc);

        gbc.gridx = 1;
        JTextField endDateField = new JTextField(LocalDate.now().plusMonths(6).toString(), 15);
        styleTextField(endDateField);
        panel.add(endDateField, gbc);

        // Info label
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("💡 After creating the Chama, you can add members from the 'Add Members' option.");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoLabel.setForeground(ATM_GREEN_DIM);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(infoLabel, gbc);

        // Create Button
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        JButton createBtn = new JButton("✓ CREATE CHAMA");
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        createBtn.setBackground(ATM_PURPLE);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createBtn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));

        createBtn.addActionListener(ev -> {
            String groupName = groupNameField.getText().trim();
            if (groupName.isEmpty()) {
                ATMDialog.error(ATMDashboard.this, "Enter Chama name");
                groupNameField.requestFocus();
                return;
            }

            double goal;
            try {
                goal = Double.parseDouble(goalField.getText().trim());
                if (goal <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                ATMDialog.error(ATMDashboard.this, "Enter a valid goal amount");
                goalField.requestFocus();
                return;
            }

            LocalDate startDate, endDate;
            try {
                startDate = LocalDate.parse(startDateField.getText().trim());
                endDate = LocalDate.parse(endDateField.getText().trim());
                if (endDate.isBefore(startDate)) {
                    ATMDialog.error(ATMDashboard.this, "End date must be after start date");
                    endDateField.requestFocus();
                    return;
                }
            } catch (Exception ex) {
                ATMDialog.error(ATMDashboard.this, "Enter valid dates (YYYY-MM-DD)");
                return;
            }

            createBtn.setEnabled(false);
            createBtn.setText("⏳ CREATING...");

            executor.submit(() -> {
                ChamaGroup group = new ChamaGroup();
                group.setGroupName(groupName);
                group.setCreatedBy(Session.getUserId());
                group.setLeaderId(Session.getUserId());
                group.setTotalGoal(goal);
                group.setStartDate(startDate);
                group.setEndDate(endDate);
                group.setContributionFrequency((String) frequencyCombo.getSelectedItem());

                // Create Chama with empty members list
                boolean success = ChamaGroup.createGroupWithSimpleMembers(group, new ArrayList<>());

                SwingUtilities.invokeLater(() -> {
                    createBtn.setEnabled(true);
                    createBtn.setText("✓ CREATE CHAMA");

                    if (success) {
                        soundSuccess();

                        // ============================================================
                        // ✅ IMMEDIATE REFRESH - NO DELAY
                        // ============================================================

                        // 1. Clear the form
                        groupNameField.setText("");
                        goalField.setText("");
                        startDateField.setText(LocalDate.now().toString());
                        endDateField.setText(LocalDate.now().plusMonths(6).toString());

                        // 2. Refresh ALL UI components immediately
                        refreshAll();                    // Refresh balance, statements, etc.
                        refreshChamaData();              // Refresh Chama data in memory
                        refreshMyChamasPanel();          // Refresh the My Chamas tab
                        refreshChamaList();              // Refresh the Chama list

                        // 3. Force UI update
                        contentArea.revalidate();
                        contentArea.repaint();

                        // 4. Switch to My Chamas tab to show the new Chama
                        JTabbedPane tabbedPane = findTabbedPane(contentArea);
                        if (tabbedPane != null) {
                            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                                String title = tabbedPane.getTitleAt(i);
                                if (title.contains("My Chamas") || title.contains("📋 My Chamas")) {
                                    tabbedPane.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }

                        // 5. Show success message
                        JOptionPane.showMessageDialog(ATMDashboard.this,
                                "✅ Chama '" + groupName + "' created successfully!\n\n" +
                                        "You can now add members using the 'Add Members' option.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);

                    } else {
                        soundError();
                        JOptionPane.showMessageDialog(ATMDashboard.this,
                                "❌ Failed to create Chama.\n\nPlease check console for errors.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
        });

        panel.add(createBtn, gbc);

        return panel;
    }
    private JPanel buildAddMembersPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // ============================================================
        // HEADER
        // ============================================================
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ATM_SCREEN_BG);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("👥 ADD MEMBERS TO CHAMA");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(ATM_AMBER);
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel subLabel = new JLabel("Select a Chama and add members (no account required)");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(ATM_GREEN_DIM);
        subLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        topPanel.add(subLabel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        // ============================================================
        // MAIN FORM PANEL
        // ============================================================
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(ATM_SCREEN_BG);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // ----- Chama Selection Row -----
        JPanel chamaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        chamaRow.setBackground(ATM_SCREEN_BG);
        chamaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel chamaLabel = new JLabel("📋 Select Chama:");
        chamaLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chamaLabel.setForeground(ATM_GREEN);
        chamaRow.add(chamaLabel);

        JComboBox<String> chamaCombo = new JComboBox<>();
        styleComboBox(chamaCombo);
        chamaCombo.setPreferredSize(new Dimension(280, 38));
        chamaCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loadLeaderChamas(chamaCombo);
        chamaRow.add(chamaCombo);

        JButton refreshChamaBtn = new JButton("🔄");
        refreshChamaBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshChamaBtn.setBackground(BUTTON_BG);
        refreshChamaBtn.setForeground(ATM_CYAN);
        refreshChamaBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshChamaBtn.setFocusPainted(false);
        refreshChamaBtn.setPreferredSize(new Dimension(45, 38));
        refreshChamaBtn.addActionListener(e -> loadLeaderChamas(chamaCombo));
        chamaRow.add(refreshChamaBtn);

        formPanel.add(chamaRow);
        formPanel.add(Box.createVerticalStrut(12));

        // ----- Member Count Label -----
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        countPanel.setBackground(ATM_SCREEN_BG);
        countPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel memberListLabel = new JLabel("👥 Members to Add (0):");
        memberListLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        memberListLabel.setForeground(ATM_GREEN);
        countPanel.add(memberListLabel);

        formPanel.add(countPanel);
        formPanel.add(Box.createVerticalStrut(8));

        // ----- Member List (Scrollable) -----
        DefaultListModel<String> memberListModel = new DefaultListModel<>();
        JList<String> memberList = new JList<>(memberListModel);
        memberList.setBackground(new Color(15, 30, 45));
        memberList.setForeground(ATM_GREEN);
        memberList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        memberList.setSelectionBackground(new Color(0, 130, 85));
        memberList.setSelectionForeground(Color.WHITE);
        memberList.setFixedCellHeight(32);

        JScrollPane memberScroll = new JScrollPane(memberList);
        memberScroll.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        memberScroll.getViewport().setBackground(new Color(15, 30, 45));
        memberScroll.setPreferredSize(new Dimension(0, 100));
        memberScroll.setMinimumSize(new Dimension(300, 80));
        memberScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        formPanel.add(memberScroll);
        formPanel.add(Box.createVerticalStrut(12));

        // ----- Add Member Input Row -----
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        inputRow.setBackground(ATM_SCREEN_BG);
        inputRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(ATM_GREEN_DIM);
        inputRow.add(nameLabel);

        JTextField memberNameField = new JTextField(18);
        styleTextField(memberNameField);
        memberNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        memberNameField.setPreferredSize(new Dimension(180, 36));
        memberNameField.setToolTipText("Enter member's full name");
        inputRow.add(memberNameField);

        JLabel phoneLabel = new JLabel("Phone:");
        phoneLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        phoneLabel.setForeground(ATM_GREEN_DIM);
        inputRow.add(phoneLabel);

        JTextField memberPhoneField = new JTextField(14);
        styleTextField(memberPhoneField);
        memberPhoneField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        memberPhoneField.setPreferredSize(new Dimension(150, 36));
        memberPhoneField.setToolTipText("Enter member's phone number");
        inputRow.add(memberPhoneField);

        JButton addMemberBtn = new JButton("➕ Add");
        addMemberBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addMemberBtn.setBackground(ATM_BLUE);
        addMemberBtn.setForeground(Color.WHITE);
        addMemberBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMemberBtn.setFocusPainted(false);
        addMemberBtn.setPreferredSize(new Dimension(90, 36));
        addMemberBtn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        inputRow.add(addMemberBtn);

        JButton removeMemberBtn = new JButton("✖ Remove");
        removeMemberBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        removeMemberBtn.setBackground(ATM_RED);
        removeMemberBtn.setForeground(Color.WHITE);
        removeMemberBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeMemberBtn.setFocusPainted(false);
        removeMemberBtn.setPreferredSize(new Dimension(100, 36));
        removeMemberBtn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        inputRow.add(removeMemberBtn);

        formPanel.add(inputRow);
        formPanel.add(Box.createVerticalStrut(12));

        // ----- Action Buttons Row -----
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        actionPanel.setBackground(ATM_SCREEN_BG);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton saveMembersBtn = new JButton("💾 SAVE ALL MEMBERS");
        saveMembersBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveMembersBtn.setBackground(ATM_GREEN);
        saveMembersBtn.setForeground(Color.WHITE);
        saveMembersBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveMembersBtn.setFocusPainted(false);
        saveMembersBtn.setPreferredSize(new Dimension(200, 44));
        saveMembersBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        JButton clearBtn = new JButton("🗑 CLEAR LIST");
        clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clearBtn.setBackground(BUTTON_BG);
        clearBtn.setForeground(ATM_RED);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.setFocusPainted(false);
        clearBtn.setPreferredSize(new Dimension(160, 44));
        clearBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_RED, 1),
                BorderFactory.createEmptyBorder(9, 24, 9, 24)));

        actionPanel.add(saveMembersBtn);
        actionPanel.add(clearBtn);

        formPanel.add(actionPanel);

        // Wrap form in scroll pane
        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(ATM_SCREEN_BG);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(formScroll, BorderLayout.CENTER);

        // ============================================================
        // EVENT HANDLERS
        // ============================================================

        // Update member count when list changes
        Runnable updateCount = () -> {
            int count = memberListModel.getSize();
            memberListLabel.setText("👥 Members to Add (" + count + "):");
        };

        // Add member
        addMemberBtn.addActionListener(e -> {
            String name = memberNameField.getText().trim();
            String phone = memberPhoneField.getText().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                ATMDialog.warning(ATMDashboard.this, "Please enter member's name and phone number");
                if (name.isEmpty()) memberNameField.requestFocus();
                else memberPhoneField.requestFocus();
                return;
            }

            // Check if already added
            String display = name + " | " + phone;
            for (int i = 0; i < memberListModel.getSize(); i++) {
                if (memberListModel.get(i).equals(display)) {
                    ATMDialog.warning(ATMDashboard.this, "⚠️ Member already added: " + name);
                    return;
                }
            }

            memberListModel.addElement(display);
            memberNameField.setText("");
            memberPhoneField.setText("");
            memberNameField.requestFocus();
            updateCount.run();
            soundSuccess();
            setStatus("► Added: " + name);
        });

        // Enter key to add
        memberNameField.addActionListener(e -> addMemberBtn.doClick());
        memberPhoneField.addActionListener(e -> addMemberBtn.doClick());

        // Remove member
        removeMemberBtn.addActionListener(e -> {
            int selected = memberList.getSelectedIndex();
            if (selected >= 0) {
                String removed = memberListModel.getElementAt(selected);
                memberListModel.remove(selected);
                updateCount.run();
                soundBeep();
                setStatus("► Removed: " + removed);
            } else {
                ATMDialog.warning(ATMDashboard.this, "Select a member to remove from the list.");
            }
        });

        // Clear list
        clearBtn.addActionListener(e -> {
            if (!memberListModel.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(ATMDashboard.this,
                        "Clear all " + memberListModel.getSize() + " members from the list?",
                        "Confirm Clear",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    memberListModel.clear();
                    updateCount.run();
                    soundBeep();
                    setStatus("► List cleared");
                }
            }
        });

        // Load existing members when chama is selected
        chamaCombo.addActionListener(e -> {
            if (chamaCombo.getSelectedItem() != null) {
                String selected = (String) chamaCombo.getSelectedItem();
                if (selected.contains("|")) {
                    String[] parts = selected.split(" \\| ");
                    try {
                        int chamaId = Integer.parseInt(parts[0]);
                        loadExistingSimpleMembers(chamaId, memberListModel);
                        updateCount.run();
                    } catch (NumberFormatException ex) {
                        // Ignore - "No Chamas" message
                    }
                }
            }
        });

        // SAVE ALL MEMBERS
        saveMembersBtn.addActionListener(e -> {
            if (chamaCombo.getSelectedItem() == null || chamaCombo.getItemCount() == 0) {
                ATMDialog.warning(ATMDashboard.this, "Please select a Chama first.");
                return;
            }

            String selected = (String) chamaCombo.getSelectedItem();
            if (selected.contains("No Chamas")) {
                ATMDialog.warning(ATMDashboard.this, "You are not a leader of any Chama.\n\nOnly Chama leaders can add members.");
                return;
            }

            String[] parts = selected.split(" \\| ");
            int chamaId = Integer.parseInt(parts[0]);
            String groupName = parts[1];

            if (memberListModel.isEmpty()) {
                ATMDialog.warning(ATMDashboard.this,
                        "⚠️ No members to save.\n\n" +
                                "Add at least one member using the form above,\n" +
                                "then click 'SAVE ALL MEMBERS'.");
                return;
            }

            List<SimpleMember> members = new ArrayList<>();
            for (int i = 0; i < memberListModel.getSize(); i++) {
                String item = memberListModel.get(i);
                String[] memberParts = item.split(" \\| ");
                if (memberParts.length == 2) {
                    SimpleMember member = new SimpleMember();
                    member.setFullname(memberParts[0]);
                    member.setPhoneNumber(memberParts[1]);
                    member.setMpesaNumber(memberParts[1]);
                    members.add(member);
                }
            }

            int confirm = JOptionPane.showConfirmDialog(ATMDashboard.this,
                    "Add " + members.size() + " members to Chama:\n" + groupName + "?",
                    "Confirm Save",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) return;

            saveMembersBtn.setEnabled(false);
            saveMembersBtn.setText("⏳ SAVING...");

            executor.submit(() -> {
                boolean success = ChamaGroup.addSimpleMembersToChama(chamaId, members);

                SwingUtilities.invokeLater(() -> {
                    saveMembersBtn.setEnabled(true);
                    saveMembersBtn.setText("💾 SAVE ALL MEMBERS");

                    if (success) {
                        soundSuccess();
                        ATMDialog.success(ATMDashboard.this,
                                "✅ " + members.size() + " members added successfully to\n" + groupName,
                                "Export Complete");
                        memberListModel.clear();
                        updateCount.run();
                        loadExistingSimpleMembers(chamaId, memberListModel);
                        updateCount.run();
                        refreshChamaData();
                        refreshMyChamasPanel();
                        setStatus("► " + members.size() + " members added to " + groupName);
                    } else {
                        soundError();
                        ATMDialog.error(ATMDashboard.this, "❌ Failed to save members.\n\nPlease check console for errors.");
                    }
                });
            });
        });

        return panel;
    }
// ========== HELPER METHODS ==========

    private void loadLeaderChamas(JComboBox<String> combo) {
        combo.removeAllItems();
        List<Map<String, Object>> chamas = ChamaGroup.getUserChamas(Session.getUserId());

        for (Map<String, Object> chama : chamas) {
            String role = (String) chama.get("role");
            if ("LEADER".equals(role)) {
                int id = (Integer) chama.get("id");
                String name = (String) chama.get("group_name");
                combo.addItem(id + " | " + name);
            }
        }

        if (combo.getItemCount() == 0) {
            combo.addItem("No Chamas where you are leader");
        }
    }
    private void loadExistingSimpleMembers(int chamaId, DefaultListModel<String> model) {
        model.clear();
        List<SimpleMember> members = ChamaSimpleMemberManager.getSimpleMembers(chamaId);
        for (SimpleMember member : members) {
            model.addElement(member.getFullname() + " | " + member.getPhoneNumber() + " ✅");
        }
    }


    private void refreshMyChamasPanel() {
        SwingUtilities.invokeLater(() -> {
            // Find the Chama Management tabbed pane
            JTabbedPane tabbedPane = findTabbedPane(contentArea);
            if (tabbedPane == null) {
                System.out.println("⚠️ No tabbed pane found");
                return;
            }

            // Find and refresh the My Chamas tab
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                String title = tabbedPane.getTitleAt(i);
                if (title.contains("My Chamas") || title.contains("📋 My Chamas")) {
                    System.out.println("🔄 Refreshing My Chamas panel...");

                    // Create a fresh panel
                    JPanel newPanel = buildMyChamasPanel();
                    tabbedPane.setComponentAt(i, newPanel);
                    tabbedPane.setSelectedIndex(i);

                    // Force refresh of the panel's contents
                    newPanel.revalidate();
                    newPanel.repaint();

                    System.out.println("✅ My Chamas panel refreshed!");
                    break;
                }
            }

            // Also refresh the Chama management screen if it's the current view
            contentArea.revalidate();
            contentArea.repaint();
        });
    }
    private JTabbedPane findTabbedPane(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTabbedPane) {
                return (JTabbedPane) comp;
            }
            if (comp instanceof Container) {
                JTabbedPane found = findTabbedPane((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ================= JOIN CHAMA PANEL =================
    private JPanel buildJoinChamaPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField codeField = new JTextField(20);
        styleTextField(codeField);

        JLabel enterCodeLabel = new JLabel("Enter Chama Code:");
        enterCodeLabel.setFont(FONT_LABEL);
        enterCodeLabel.setForeground(ATM_GREEN_DIM);

        JLabel hintLabel = new JLabel("Enter Chama code to request membership");
        hintLabel.setFont(FONT_SMALL);
        hintLabel.setForeground(ATM_GREEN_DIM);

        JButton debugBtn = atmButton("🔍 DEBUG");
        debugBtn.setForeground(ATM_CYAN);

        JButton joinBtn = atmButton("🔑 REQUEST TO JOIN");
        joinBtn.setForeground(ATM_PURPLE);

        debugBtn.addActionListener(e -> {
            ChamaGroup.debugListAllChamas();
            ATMDialog.info(this, "Check console for list of all Chamas and their codes.");
        });

        joinBtn.addActionListener(e -> {
            String code = codeField.getText().trim().toUpperCase();
            if (code.isEmpty()) {
                ATMDialog.error(this, "Enter Chama code");
                return;
            }

            if (!ATMDialog.confirm(this, "Request to join Chama with code: " + code + "?")) {
                return;
            }

            joinBtn.setEnabled(false);
            joinBtn.setText("⏳ PROCESSING...");

            EXECUTOR.submit(() -> {
                boolean success = ChamaGroup.requestToJoinChama(code, Session.userId, Session.fullname);

                SwingUtilities.invokeLater(() -> {
                    joinBtn.setEnabled(true);
                    joinBtn.setText("🔑 REQUEST TO JOIN");

                    if (success) {
                        soundSuccess();
                        ATMDialog.success(this, "✓ Join request sent!\n\nThe Chama leader will review your request.", "Export Complete");
                        codeField.setText("");
                        refreshAll();
                    } else {
                        soundError();
                        ATMDialog.error(this, "Cannot request to join.\n\nInvalid code or already a member.");
                    }
                });
            });
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(ATM_SCREEN_BG);
        buttonPanel.add(debugBtn);
        buttonPanel.add(joinBtn);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(enterCodeLabel, gbc);
        gbc.gridx = 1;
        panel.add(codeField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(hintLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        return panel;
    }

    // ================= CHAMA HELPER METHODS =================
    private void refreshChamaList() {
        SwingUtilities.invokeLater(() -> {
            // Find the My Chamas panel and refresh its list
            JTabbedPane tabbedPane = findTabbedPane(contentArea);
            if (tabbedPane == null) return;

            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                String title = tabbedPane.getTitleAt(i);
                if (title.contains("My Chamas") || title.contains("📋 My Chamas")) {
                    // Replace the entire panel
                    JPanel newPanel = buildMyChamasPanel();
                    tabbedPane.setComponentAt(i, newPanel);
                    tabbedPane.setSelectedIndex(i);
                    break;
                }
            }

            contentArea.revalidate();
            contentArea.repaint();
        });
    }

    private void refreshChamaManagementScreen() {
        SwingUtilities.invokeLater(() -> {
            int index = -1;
            Component[] components = contentArea.getComponents();
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JPanel && components[i].getName() != null &&
                        components[i].getName().equals("CHAMA_MANAGEMENT_PANEL")) {
                    index = i;
                    break;
                }
            }

            JPanel newPanel = buildChamaManagementScreen();
            newPanel.setName("CHAMA_MANAGEMENT_PANEL");

            if (index >= 0) {
                contentArea.remove(index);
                contentArea.add(newPanel, CARD_CHAMA_MANAGEMENT, index);
            } else {
                contentArea.add(newPanel, CARD_CHAMA_MANAGEMENT);
            }

            contentArea.revalidate();
            contentArea.repaint();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_CHAMA_MANAGEMENT);
        });
    }

    private void recordPaymentForMember(int chamaId, int userId, String username, double amount, String paymentMethod) {
        String groupName = "";
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT group_name FROM chama_groups WHERE id = ?")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                groupName = rs.getString("group_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ATMDialog.error(this, "Error getting Chama info: " + e.getMessage());
            return;
        }

        String finalGroupName = groupName;
        EXECUTOR.submit(() -> {
            boolean success = ChamaGroup.recordContribution(chamaId, userId, amount, paymentMethod, Session.userId);

            SwingUtilities.invokeLater(() -> {
                if (success) {
                    soundSuccess();
                    String message = String.format(
                            "✓ Payment Recorded!\n\nChama: %s\nMember: %s\nAmount: Ksh %,.0f\nMethod: %s",
                            finalGroupName, username, amount, paymentMethod);
                    ATMDialog.success(ATMDashboard.this, message, "Export Complete");
                    setStatus("► Payment of Ksh " + amount + " recorded for " + username);
                    refreshAll();
                    refreshChamaList();
                } else {
                    soundError();
                    ATMDialog.error(ATMDashboard.this, "Failed to record payment.");
                }
            });
        });
    }

    private void removeMemberFromChama(int chamaId) {
        String fullname = ATMDialog.input(this, "Enter member's full name to remove:");
        if (fullname == null) return;
        int userId = getUserIdByFullname(fullname.trim());
        if (userId <= 0) {
            ATMDialog.error(this, "Member not found");
            return;
        }
        if (userId == Session.userId) {
            ATMDialog.error(this, "You cannot remove yourself");
            return;
        }
        if (ATMDialog.confirm(this, "Remove " + fullname + " from this Chama?")) {
            if (ChamaGroup.removeMember(chamaId, userId, Session.userId)) {
                soundSuccess();
                ATMDialog.success(this, "Member removed successfully", "Export Complete");
                refreshAll();
            } else {
                soundError();
                ATMDialog.error(this, "Failed to remove member");
            }
        }
    }

    private void showPendingRequests(int chamaId, String groupName) {
        List<Map<String, Object>> requests = ChamaGroup.getPendingRequests(chamaId);
        if (requests.isEmpty()) {
            ATMDialog.info(this, "No pending join requests for " + groupName);
            return;
        }

        String[] options = new String[requests.size()];
        for (int i = 0; i < requests.size(); i++) {
            Map<String, Object> req = requests.get(i);
            options[i] = req.get("fullname") + " (Requested: " + req.get("requested_at") + ")";
        }

        String selected = (String) JOptionPane.showInputDialog(this,
                "Select member to approve/reject:",
                "Pending Join Requests - " + groupName,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (selected != null) {
            int index = -1;
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(selected)) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                Map<String, Object> req = requests.get(index);
                int userId = (int) req.get("user_id");
                String fullname = (String) req.get("fullname");

                String[] actions = {"Approve", "Reject"};
                int action = JOptionPane.showOptionDialog(this,
                        "Approve or reject " + fullname + "'s request?",
                        "Respond to Request", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, actions, actions[0]);

                boolean approve = (action == 0);

                EXECUTOR.submit(() -> {
                    boolean success = ChamaGroup.respondToJoinRequest(chamaId, userId, approve, Session.userId);

                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            soundSuccess();
                            String message = fullname + " has been " + (approve ? "APPROVED" : "REJECTED");
                            ATMDialog.success(ATMDashboard.this, message, "Export Complete");
                            refreshChamaList();
                            setStatus("► " + message);
                        } else {
                            soundError();
                            ATMDialog.error(ATMDashboard.this, "Failed to process request");
                        }
                    });
                });
            }
        }
    }

    private void showChamaDetails(int chamaId) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT g.*, COALESCE(u.fullname, u.username) as leader_name, " +
                            "cm.role, cm.status as member_status, cm.join_date, cm.approved_at " +
                            "FROM chama_groups g " +
                            "JOIN users u ON g.leader_id = u.id " +
                            "JOIN chama_members cm ON g.id = cm.chama_id " +
                            "WHERE g.id = ? AND cm.user_id = ?");
            pst.setInt(1, chamaId);
            pst.setInt(2, Session.userId);
            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                ATMDialog.error(this, "Chama not found or you don't have access");
                return;
            }

            String groupName = rs.getString("group_name");
            String groupCode = rs.getString("group_code");
            String leaderName = rs.getString("leader_name");
            double totalGoal = rs.getDouble("total_goal");
            String frequency = rs.getString("contribution_frequency");
            Date startDate = rs.getDate("start_date");
            Date endDate = rs.getDate("end_date");
            String userRole = rs.getString("role");
            String memberStatus = rs.getString("member_status");

            boolean isApproved = "APPROVED".equals(memberStatus) || "LEADER".equals(userRole);
            boolean isLeader = "LEADER".equals(userRole);

            if (!isApproved) {
                String pendingMsg = "⏳ MEMBERSHIP PENDING APPROVAL ⏳\n\n" +
                        "Your request to join '" + groupName + "' is waiting for leader approval.\n" +
                        "You will be notified once approved.\n\nLeader: " + leaderName;

                JTextArea ta = new JTextArea(pendingMsg);
                ta.setFont(FONT_SMALL);
                ta.setBackground(ATM_SCREEN_BG);
                ta.setForeground(ATM_AMBER);
                ta.setEditable(false);

                JScrollPane sp = new JScrollPane(ta);
                sp.setPreferredSize(new Dimension(450, 300));
                JOptionPane.showMessageDialog(this, sp, "Membership Pending", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // ============================================================
            // GET REGISTERED MEMBERS (users with accounts)
            // ============================================================
            PreparedStatement memberPst = conn.prepareStatement(
                    "SELECT u.id, COALESCE(u.fullname, u.username) as display_name, " +
                            "cm.role, cm.join_date, cm.approved_at " +
                            "FROM chama_members cm JOIN users u ON cm.user_id = u.id " +
                            "WHERE cm.chama_id = ? AND cm.status = 'APPROVED' " +
                            "ORDER BY cm.approved_at DESC");
            memberPst.setInt(1, chamaId);
            ResultSet memberRs = memberPst.executeQuery();

            // ============================================================
            // GET SIMPLE MEMBERS (no account required)
            // ============================================================
            List<SimpleMember> simpleMembers = ChamaSimpleMemberManager.getSimpleMembers(chamaId);

            // ============================================================
            // BUILD MEMBERS LIST - BOTH TYPES
            // ============================================================
            StringBuilder membersList = new StringBuilder();
            int memberCount = 0;

            // First, add registered members
            while (memberRs.next()) {
                memberCount++;
                String displayName = memberRs.getString("display_name");
                String role = memberRs.getString("role");
                Date joinDate = memberRs.getDate("join_date");
                Timestamp approvedAt = memberRs.getTimestamp("approved_at");

                String joinInfo = "";
                if (approvedAt != null) {
                    joinInfo = " - Joined: " + approvedAt.toString().replace("T", " ").substring(0, 19);
                } else if (joinDate != null) {
                    joinInfo = " - Joined: " + joinDate.toString();
                }
                membersList.append(String.format("  • %s (%s)%s\n", displayName, role, joinInfo));
            }

            // Then, add simple members (no account)
            for (SimpleMember sm : simpleMembers) {
                memberCount++;
                String joinInfo = " - Joined: " + sm.getJoinDate();
                String status = sm.getStatus() != null ? sm.getStatus() : "ACTIVE";
                membersList.append(String.format("  • %s (MEMBER - No Account)%s\n", sm.getFullname(), joinInfo));
            }

            // ============================================================
            // GET CONTRIBUTIONS (including simple members)
            // ============================================================
            PreparedStatement contribPst = conn.prepareStatement(
                    "SELECT c.amount, c.contribution_date, c.payment_method, " +
                            "COALESCE(u.fullname, u.username) as member_name " +
                            "FROM chama_contributions c " +
                            "JOIN users u ON c.user_id = u.id " +
                            "WHERE c.chama_id = ? AND c.status = 'CONFIRMED' " +
                            "ORDER BY c.contribution_date DESC LIMIT 20");
            contribPst.setInt(1, chamaId);
            ResultSet contribRs = contribPst.executeQuery();

            double totalAmount = 0;
            int totalPayments = 0;
            StringBuilder contribList = new StringBuilder();
            while (contribRs.next()) {
                totalPayments++;
                double amount = contribRs.getDouble("amount");
                totalAmount += amount;
                String memberName = contribRs.getString("member_name");
                Date contribDate = contribRs.getDate("contribution_date");
                String method = contribRs.getString("payment_method");
                contribList.append(String.format("  • %s: Ksh %,.0f on %s (%s)\n",
                        memberName, amount, contribDate, method != null ? method : "CASH"));
            }

            // Also get simple member contributions
            PreparedStatement simpleContribPst = conn.prepareStatement(
                    "SELECT cs.amount, cs.contribution_date, cs.payment_method, " +
                            "sm.fullname as member_name " +
                            "FROM chama_simple_contributions cs " +
                            "JOIN chama_simple_members sm ON cs.member_id = sm.id " +
                            "WHERE cs.chama_id = ? " +
                            "ORDER BY cs.contribution_date DESC LIMIT 20");
            simpleContribPst.setInt(1, chamaId);
            ResultSet simpleContribRs = simpleContribPst.executeQuery();

            while (simpleContribRs.next()) {
                totalPayments++;
                double amount = simpleContribRs.getDouble("amount");
                totalAmount += amount;
                String memberName = simpleContribRs.getString("member_name");
                Date contribDate = simpleContribRs.getDate("contribution_date");
                String method = simpleContribRs.getString("payment_method");
                contribList.append(String.format("  • %s: Ksh %,.0f on %s (%s)\n",
                        memberName, amount, contribDate, method != null ? method : "CASH"));
            }

            double progress = totalGoal > 0 ? (totalAmount / totalGoal) * 100 : 0;
            double remaining = totalGoal - totalAmount;

            // ============================================================
            // BUILD DISPLAY
            // ============================================================
            StringBuilder sb = new StringBuilder();
            sb.append("╔════════════════════════════════════════════════════════════════╗\n");
            sb.append("║                        CHAMA DETAILS                          ║\n");
            sb.append("╚════════════════════════════════════════════════════════════════╝\n\n");
            sb.append(String.format("  Name: %s\n", groupName));
            sb.append(String.format("  Code: %s\n", groupCode));
            sb.append(String.format("  Leader: %s\n", leaderName));
            sb.append(String.format("  Goal: Ksh %,.0f\n", totalGoal));
            sb.append(String.format("  Frequency: %s\n", frequency));
            sb.append(String.format("  Period: %s to %s\n", startDate, endDate));
            sb.append(String.format("  Members: %d\n\n", memberCount));

            sb.append("  MEMBERS\n");
            sb.append("  ────────────────────────────────────────────────────────────────\n");
            sb.append(membersList.toString());

            sb.append("\n  CONTRIBUTIONS SUMMARY\n");
            sb.append("  ────────────────────────────────────────────────────────────────\n");
            sb.append(String.format("  Total Payments: %d\n", totalPayments));
            sb.append(String.format("  Total Collected: Ksh %,.0f\n", totalAmount));
            sb.append(String.format("  Progress: %.1f%%\n", progress));
            sb.append(String.format("  Remaining: Ksh %,.0f\n", remaining));

            if (contribList.length() > 0) {
                sb.append("\n  RECENT CONTRIBUTIONS\n");
                sb.append("  ────────────────────────────────────────────────────────────────\n");
                sb.append(contribList.toString());
            }

            if (!isLeader) {
                sb.append("\n  ⚠️ READ-ONLY VIEW ⚠️\n");
                sb.append("  Only the Chama leader can manage members.\n");
            }

            JTextArea ta = new JTextArea(sb.toString());
            ta.setFont(FONT_MONO);
            ta.setBackground(ATM_SCREEN_BG);
            ta.setForeground(ATM_GREEN);
            ta.setEditable(false);
            ta.setMargin(new Insets(10, 10, 10, 10));

            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(800, 600));
            JOptionPane.showMessageDialog(this, sp, "Chama Details - " + groupName, JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            ATMDialog.error(this, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getUserIdByUsername(String username) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id FROM users WHERE username = ? OR fullname = ?")) {
            pst.setString(1, username.trim());
            pst.setString(2, username.trim());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    private int getUserIdByFullname(String fullname) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id FROM users WHERE LOWER(TRIM(fullname)) = LOWER(TRIM(?)) OR LOWER(username) = LOWER(?)")) {
            pst.setString(1, fullname.trim());
            pst.setString(2, fullname.trim());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    private String getReportsDirectory() {
        String userHome = System.getProperty("user.home");
        String reportsPath = userHome + File.separator + "SupremeMoneyCoach_Reports";
        File reportsDir = new File(reportsPath);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        return reportsPath;
    }

    // ================= SMART CHAMA DIALOG =================
    private void showSmartChamaDialog() {
        JDialog chamaDialog = new JDialog(this, "CHAMA MENU", true);
        chamaDialog.setSize(550, 480);
        chamaDialog.setLocationRelativeTo(this);
        chamaDialog.setUndecorated(true);
        chamaDialog.setBackground(ATM_SCREEN_BG);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(ATM_SCREEN_BG);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_PURPLE, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ATM_SCREEN_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("◈ CHAMA MENU ◈", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 22));
        titleLabel.setForeground(ATM_AMBER);

        JLabel subtitleLabel = new JLabel("Select Chama Feature", SwingConstants.CENTER);
        subtitleLabel.setFont(FONT_SMALL);
        subtitleLabel.setForeground(ATM_GREEN_DIM);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(3, 2, 15, 12));
        centerPanel.setBackground(ATM_SCREEN_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton groupGoalBtn = createSmartMenuButton("📊 GROUP GOAL", "Calculate group savings goals", ATM_GREEN);
        JButton chamaMgmtBtn = createSmartMenuButton("📋 CHAMA MGMT", "Full Chama management", ATM_PURPLE);
        JButton exportReportBtn = createSmartMenuButton("📄 EXPORT REPORT", "Generate Chama reports", ATM_AMBER);
        JButton viewDetailsBtn = createSmartMenuButton("👁 VIEW DETAILS", "View Chama details", ATM_BLUE);
        JButton pendingRequestsBtn = createSmartMenuButton("⏳ PENDING REQUESTS", "Approve join requests", ATM_RED);
        JButton recordPaymentBtn = createSmartMenuButton("💰 RECORD PAYMENT", "Record member payments", ATM_CYAN);

        groupGoalBtn.addActionListener(e -> { soundBeep(); chamaDialog.dispose(); openChamaPlanner(); });
        chamaMgmtBtn.addActionListener(e -> { soundBeep(); chamaDialog.dispose(); CardLayout cl = (CardLayout) contentArea.getLayout(); cl.show(contentArea, CARD_CHAMA_MANAGEMENT); });
        exportReportBtn.addActionListener(e -> { soundBeep(); chamaDialog.dispose(); showChamaReportOptions(); });
        viewDetailsBtn.addActionListener(e -> { soundBeep(); chamaDialog.dispose(); showMyChamasList(); });
        pendingRequestsBtn.addActionListener(e -> { soundBeep(); chamaDialog.dispose(); showPendingRequestsForChamas(); });
        recordPaymentBtn.addActionListener(e -> { soundBeep(); chamaDialog.dispose(); showRecordPaymentOptions(); });
        trackActivity("VIEW_CHAMA");


        centerPanel.add(groupGoalBtn);
        centerPanel.add(chamaMgmtBtn);
        centerPanel.add(exportReportBtn);
        centerPanel.add(viewDetailsBtn);
        centerPanel.add(pendingRequestsBtn);
        centerPanel.add(recordPaymentBtn);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(ATM_SCREEN_BG);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JButton closeBtn = new JButton("✖ CLOSE");
        closeBtn.setFont(FONT_BUTTON);
        closeBtn.setBackground(BUTTON_BG);
        closeBtn.setForeground(ATM_RED);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(120, 40));
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_RED, 1),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setBackground(ATM_RED); closeBtn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { closeBtn.setBackground(BUTTON_BG); closeBtn.setForeground(ATM_RED); }
        });
        closeBtn.addActionListener(e -> chamaDialog.dispose());

        footerPanel.add(closeBtn);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        chamaDialog.setContentPane(mainPanel);
        chamaDialog.setVisible(true);
    }

    private JButton createSmartMenuButton(String text, String tooltip, Color color) {
        JButton btn = new JButton("<html><center>" + text + "<br><font size='1' color='#8d93ab'>" + tooltip + "</font></center></html>");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(BUTTON_BG);
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(12, 10, 12, 10)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(color); btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BUTTON_BG); btn.setForeground(color); }
        });
        return btn;
    }

    private void showMyChamasList() {
        List<Map<String, Object>> userChamas = ChamaGroup.getUserChamas(Session.userId);
        if (userChamas.isEmpty()) {
            ATMDialog.warning(this, "You are not a member of any Chama.\n\nCreate a new Chama or join an existing one!");
            return;
        }

        JDialog chamaListDialog = new JDialog(this, "My Chamas", true);
        chamaListDialog.setSize(500, 400);
        chamaListDialog.setLocationRelativeTo(this);
        chamaListDialog.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        JLabel titleLabel = new JLabel("◈ MY CHAMAS ◈", SwingConstants.CENTER);
        titleLabel.setFont(FONT_HEAD);
        titleLabel.setForeground(ATM_AMBER);
        panel.add(titleLabel, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        Map<String, Integer> chamaIdMap = new HashMap<>();

        for (Map<String, Object> chama : userChamas) {
            String name = (String) chama.get("group_name");
            String role = (String) chama.get("role");
            String status = (String) chama.get("status");
            String display = name + " (" + role + ")" + (status.equals("PENDING") ? " - AWAITING APPROVAL" : "");
            listModel.addElement(display);
            chamaIdMap.put(display, (Integer) chama.get("id"));
        }

        JList<String> chamaList = new JList<>(listModel);
        chamaList.setBackground(ATM_SCREEN_BG);
        chamaList.setForeground(ATM_GREEN);
        chamaList.setFont(FONT_SMALL);
        chamaList.setSelectionBackground(new Color(0, 130, 85));

        JScrollPane scroll = new JScrollPane(chamaList);
        scroll.setBorder(BorderFactory.createLineBorder(ATM_BORDER));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton viewBtn = new JButton("View Details");
        JButton closeBtn = new JButton("Close");

        viewBtn.addActionListener(e -> {
            String selected = chamaList.getSelectedValue();
            if (selected != null) {
                int chamaId = chamaIdMap.get(selected);
                chamaListDialog.dispose();
                showChamaDetails(chamaId);
            } else {
                ATMDialog.warning(this, "Select a Chama first");
            }
        });

        closeBtn.addActionListener(e -> chamaListDialog.dispose());

        buttonPanel.add(viewBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        chamaListDialog.setContentPane(panel);
        chamaListDialog.setVisible(true);
    }

    private void showPendingRequestsForChamas() {
        List<Map<String, Object>> userChamas = ChamaGroup.getUserChamas(Session.userId);
        List<Map<String, Object>> leaderChamas = new ArrayList<>();

        for (Map<String, Object> chama : userChamas) {
            if ("LEADER".equals(chama.get("role"))) {
                leaderChamas.add(chama);
            }
        }

        if (leaderChamas.isEmpty()) {
            ATMDialog.warning(this, "Only Chama leaders can view pending requests.\n\nYou are not a leader of any Chama.");
            return;
        }

        String[] chamaNames = new String[leaderChamas.size()];
        Map<String, Integer> chamaIdMap = new HashMap<>();
        for (int i = 0; i < leaderChamas.size(); i++) {
            String name = (String) leaderChamas.get(i).get("group_name");
            chamaNames[i] = name;
            chamaIdMap.put(name, (Integer) leaderChamas.get(i).get("id"));
        }

        String selected = (String) JOptionPane.showInputDialog(this,
                "Select Chama to view pending requests:",
                "Pending Join Requests", JOptionPane.QUESTION_MESSAGE,
                null, chamaNames, chamaNames.length > 0 ? chamaNames[0] : null);

        if (selected != null) {
            int chamaId = chamaIdMap.get(selected);
            showPendingRequests(chamaId, selected);
        }
    }
    private void showRecordPaymentForMemberDialog(int chamaId, String groupName) {
        JDialog dialog = new JDialog(this, "Pay for Member", true);
        dialog.setSize(450, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(ATM_SCREEN_BG);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // ============================================================
        // HEADER
        // ============================================================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ATM_SCREEN_BG);

        JLabel titleLabel = new JLabel("💰 PAY FOR MEMBER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 18));
        titleLabel.setForeground(ATM_AMBER);
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel chamaLabel = new JLabel("Chama: " + groupName, SwingConstants.CENTER);
        chamaLabel.setFont(FONT_BODY);
        chamaLabel.setForeground(ATM_GREEN);
        chamaLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        headerPanel.add(chamaLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ============================================================
        // FORM PANEL
        // ============================================================
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ATM_SCREEN_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Member Selection
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel memberLabel = new JLabel("Select Member:");
        memberLabel.setFont(FONT_LABEL);
        memberLabel.setForeground(ATM_GREEN);
        formPanel.add(memberLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JComboBox<String> memberCombo = new JComboBox<>();
        styleComboBox(memberCombo);
        memberCombo.setPreferredSize(new Dimension(200, 35));

        // Load ALL members (registered + simple)
        loadAllMembersForChama(chamaId, memberCombo);

        formPanel.add(memberCombo, gbc);

        // Amount
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel amountLabel = new JLabel("Amount (Ksh):");
        amountLabel.setFont(FONT_LABEL);
        amountLabel.setForeground(ATM_GREEN);
        formPanel.add(amountLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField amountField = new JTextField(15);
        styleTextField(amountField);
        amountField.setFont(new Font("Courier New", Font.BOLD, 18));
        amountField.setHorizontalAlignment(JTextField.CENTER);
        amountField.setPreferredSize(new Dimension(200, 40));
        formPanel.add(amountField, gbc);

        // Payment Method
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 1;
        JLabel methodLabel = new JLabel("Payment Method:");
        methodLabel.setFont(FONT_LABEL);
        methodLabel.setForeground(ATM_GREEN);
        formPanel.add(methodLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"CASH", "MPESA", "BANK TRANSFER", "CHEQUE"});
        styleComboBox(methodCombo);
        methodCombo.setPreferredSize(new Dimension(200, 35));
        formPanel.add(methodCombo, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // ============================================================
        // BUTTON PANEL
        // ============================================================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton recordBtn = new JButton("✓ RECORD PAYMENT");
        recordBtn.setFont(new Font("Courier New", Font.BOLD, 13));
        recordBtn.setBackground(ATM_GREEN);
        recordBtn.setForeground(Color.WHITE);
        recordBtn.setFocusPainted(false);
        recordBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        recordBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton cancelBtn = new JButton("✖ CANCEL");
        cancelBtn.setFont(new Font("Courier New", Font.BOLD, 13));
        cancelBtn.setBackground(BUTTON_BG);
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_RED, 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // ============================================================
        // EVENT HANDLERS
        // ============================================================

        // Enter key to submit
        amountField.addActionListener(e -> recordBtn.doClick());

        recordBtn.addActionListener(e -> {
            // Validate member selection
            String selected = (String) memberCombo.getSelectedItem();
            if (selected == null || selected.contains("No members")) {
                ATMDialog.warning(ATMDashboard.this, "Please select a member");
                return;
            }

            // Validate amount
            String amountStr = amountField.getText().trim();
            if (amountStr.isEmpty()) {
                ATMDialog.warning(ATMDashboard.this, "Please enter an amount");
                amountField.requestFocus();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    ATMDialog.warning(ATMDashboard.this, "Amount must be greater than 0");
                    amountField.requestFocus();
                    return;
                }

                // Parse the selected item to get member info
                // Format: "ID | TYPE | NAME"
                String[] parts = selected.split(" \\| ");
                int memberId = Integer.parseInt(parts[0]);
                String memberType = parts[1];
                String memberName = parts[2];

                String paymentMethod = (String) methodCombo.getSelectedItem();

                // Confirm before recording
                int confirm = JOptionPane.showConfirmDialog(ATMDashboard.this,
                        String.format("Record payment of Ksh %,.0f for\n%s (%s) using %s?",
                                amount, memberName, memberType, paymentMethod),
                        "Confirm Payment",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (confirm != JOptionPane.YES_OPTION) return;

                dialog.dispose();

                if ("REGISTERED".equals(memberType)) {
                    // Registered user
                    recordPaymentForMember(chamaId, memberId, memberName, amount, paymentMethod);
                } else {
                    // Simple member
                    recordPaymentForSimpleMember(chamaId, memberId, memberName, amount, paymentMethod);
                }

            } catch (NumberFormatException ex) {
                ATMDialog.warning(ATMDashboard.this, "Please enter a valid amount");
                amountField.requestFocus();
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(recordBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }
    private void loadAllMembersForChama(int chamaId, JComboBox<String> combo) {
        combo.removeAllItems();

        // Load registered members
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT u.id, COALESCE(u.fullname, u.username) as display_name " +
                             "FROM chama_members cm " +
                             "JOIN users u ON cm.user_id = u.id " +
                             "WHERE cm.chama_id = ? AND cm.status = 'APPROVED' " +
                             "ORDER BY display_name")) {
            pst.setInt(1, chamaId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("display_name");
                combo.addItem(id + " | REGISTERED | " + name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Load simple members
        List<SimpleMember> simpleMembers = ChamaSimpleMemberManager.getSimpleMembers(chamaId);
        for (SimpleMember sm : simpleMembers) {
            combo.addItem(sm.getId() + " | SIMPLE | " + sm.getFullname());
        }

        if (combo.getItemCount() == 0) {
            combo.addItem("No members found in this Chama");
        }
    }

    private void showRecordPaymentOptions() {
        List<Map<String, Object>> userChamas = ChamaGroup.getUserChamas(Session.userId);
        if (userChamas.isEmpty()) {
            ATMDialog.warning(this, "You are not a member of any Chama.");
            return;
        }

        // Build list of available chamas (excluding PENDING)
        List<String> chamaNameList = new ArrayList<>();
        Map<String, Integer> chamaIdMap = new HashMap<>();
        Map<String, String> chamaRoleMap = new HashMap<>();
        Map<String, String> chamaStatusMap = new HashMap<>();

        for (Map<String, Object> chama : userChamas) {
            String name = (String) chama.get("group_name");
            String status = (String) chama.get("status");
            if (!"PENDING".equals(status)) {
                chamaNameList.add(name);
                chamaIdMap.put(name, (Integer) chama.get("id"));
                chamaRoleMap.put(name, (String) chama.get("role"));
                chamaStatusMap.put(name, status);
            }
        }

        if (chamaNameList.isEmpty()) {
            ATMDialog.warning(this, "You have no active Chamas. Your membership may be pending approval.");
            return;
        }

        String[] chamaNames = chamaNameList.toArray(new String[0]);

        String selected = (String) JOptionPane.showInputDialog(this,
                "Select Chama to record payment:",
                "Record Payment", JOptionPane.QUESTION_MESSAGE,
                null, chamaNames, chamaNames.length > 0 ? chamaNames[0] : null);

        if (selected != null) {
            int chamaId = chamaIdMap.get(selected);
            String role = chamaRoleMap.get(selected);

            String[] options = {"Pay for Myself", "Pay for Another Member (Leader only)"};
            int choice = JOptionPane.showOptionDialog(this,
                    "Select payment type for Chama: " + selected,
                    "Payment Options", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                // Pay for Myself - ask for amount and method
                showPayForMyselfDialog(chamaId, selected);
            } else if (choice == 1 && "LEADER".equals(role)) {
                // Pay for Another Member - show dialog with ALL members
                showRecordPaymentForMemberDialog(chamaId, selected);
            } else if (choice == 1 && !"LEADER".equals(role)) {
                ATMDialog.warning(this, "Only Chama leaders can record payments for other members.");
            }
        }
    }
    private void showPayForMyselfDialog(int chamaId, String groupName) {
        // Show amount input dialog
        String amountStr = JOptionPane.showInputDialog(this,
                "Enter amount to pay for yourself:\n\nChama: " + groupName + "\nMember: " + Session.getUsername(),
                "Pay for Myself", JOptionPane.QUESTION_MESSAGE);

        if (amountStr == null || amountStr.trim().isEmpty()) return;

        try {
            double amount = Double.parseDouble(amountStr.trim());
            if (amount <= 0) {
                ATMDialog.error(this, "Amount must be greater than 0");
                return;
            }

            // Show payment method selection
            String[] methods = {"CASH", "MPESA", "BANK TRANSFER", "CHEQUE"};
            String method = (String) JOptionPane.showInputDialog(this,
                    "Select payment method:", "Payment Method",
                    JOptionPane.QUESTION_MESSAGE, null, methods, methods[0]);

            if (method == null) return;

            // Check if user is a registered member
            boolean isRegistered = checkIfRegisteredMember(chamaId, Session.getUserId());

            if (isRegistered) {
                // Registered member
                recordPaymentForMember(chamaId, Session.getUserId(), Session.getUsername(), amount, method);
            } else {
                // Check if user is a simple member
                SimpleMember simpleMember = getSimpleMemberByUserId(chamaId, Session.getUserId());
                if (simpleMember != null) {
                    recordPaymentForSimpleMember(chamaId, simpleMember.getId(),
                            simpleMember.getFullname(), amount, method);
                } else {
                    ATMDialog.warning(this, "You are not a member of this Chama");
                }
            }

        } catch (NumberFormatException ex) {
            ATMDialog.error(this, "Please enter a valid amount");
        }
    }

    private void showChamaReportOptions() {
        String[] reportTypes = {"Full Chama Report", "Member Contributions", "Payment History", "Financial Summary"};
        String selected = (String) JOptionPane.showInputDialog(this,
                "Select report type:", "Chama Reports",
                JOptionPane.QUESTION_MESSAGE, null, reportTypes, reportTypes[0]);

        if (selected != null) {
            List<Map<String, Object>> userChamas = ChamaGroup.getUserChamas(Session.userId);
            if (userChamas.isEmpty()) {
                ATMDialog.warning(this, "You are not a member of any Chama.");
                return;
            }

            String[] chamaNames = new String[userChamas.size()];
            Map<String, Integer> chamaIdMap = new HashMap<>();
            for (int i = 0; i < userChamas.size(); i++) {
                String name = (String) userChamas.get(i).get("group_name");
                chamaNames[i] = name;
                chamaIdMap.put(name, (Integer) userChamas.get(i).get("id"));
            }

            String selectedChama = (String) JOptionPane.showInputDialog(this,
                    "Select Chama:", "Generate " + selected,
                    JOptionPane.QUESTION_MESSAGE, null, chamaNames, chamaNames[0]);

            if (selectedChama != null) {
                int chamaId = chamaIdMap.get(selectedChama);
                String timestamp = LocalDateTime.now().format(DT_FORMATTER).replace(":", "-").replace(" ", "_");
                String filename = getReportsDirectory() + File.separator + "Chama_" +
                        selectedChama.replace(" ", "_") + "_" + selected.replace(" ", "_") + "_" + timestamp + ".pdf";

                EXECUTOR.submit(() -> {
                    ChamaGroup.generateChamaReport(chamaId, filename);
                    SwingUtilities.invokeLater(() -> {
                        soundSuccess();
                        ATMDialog.success(ATMDashboard.this, "Report generated:\n" + filename, "Export Complete");
                        setStatus("► Chama report saved: " + filename);
                    });
                });
            }
        }
    }

    // ================= VIDEO PLAYER =================
    private void showVideoPlayer() {
        JDialog loadingDialog = new JDialog(this, "Loading...", true);
        loadingDialog.setSize(300, 150);
        loadingDialog.setLocationRelativeTo(this);
        JPanel loadPanel = new JPanel(new BorderLayout());
        loadPanel.setBackground(ATM_SCREEN_BG);
        loadPanel.add(new JLabel("📺 Loading videos...", SwingConstants.CENTER), BorderLayout.CENTER);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        trackActivity("VIEW_VIDEOS");
        loadPanel.add(progressBar, BorderLayout.SOUTH);
        loadingDialog.add(loadPanel);

        EXECUTOR.submit(() -> {
            String liveVideoUrl = getLiveVideoUrl();
            String liveVideoTitle = getLiveVideoTitle();
            String liveBusinessName = getLiveBusinessName();
            String videoId = null;
            if (liveVideoUrl != null && !liveVideoUrl.isEmpty()) videoId = extractYouTubeId(liveVideoUrl);
            DefaultListModel<String> videoListModel = new DefaultListModel<>();
            loadVideoListBackground(videoListModel);
            final String finalLiveVideoUrl = liveVideoUrl;
            final String finalLiveVideoTitle = liveVideoTitle;
            final String finalLiveBusinessName = liveBusinessName;
            final String finalVideoId = videoId;
            SwingUtilities.invokeLater(() -> {
                loadingDialog.dispose();
                showVideoPlayerDialog(finalLiveVideoUrl, finalLiveVideoTitle, finalLiveBusinessName, finalVideoId, videoListModel);
            });
        });
        loadingDialog.setVisible(true);
    }

    private String extractYouTubeId(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/)([a-zA-Z0-9_-]{11})";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(url);
            if (m.find()) return m.group(1);

            if (url.contains("youtu.be/")) {
                String[] parts = url.split("youtu.be/");
                if (parts.length > 1) {
                    String id = parts[1];
                    if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                    if (id.length() >= 11) return id.substring(0, 11);
                }
            }
            if (url.contains("v=")) {
                String[] parts = url.split("v=");
                if (parts.length > 1) {
                    String id = parts[1];
                    if (id.contains("&")) id = id.substring(0, id.indexOf("&"));
                    if (id.length() >= 11) return id.substring(0, 11);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void showVideoPlayerDialog(String liveVideoUrl, String liveVideoTitle, String liveBusinessName,
                                       String videoId, DefaultListModel<String> videoListModel) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(1100, screenSize.width - 50);
        int dialogHeight = Math.min(650, screenSize.height - 50);
        JDialog dialog = new JDialog(this, "📺 Business Videos", true);
        dialog.setSize(dialogWidth,dialogHeight);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        JPanel mainPanel = new JPanel(new BorderLayout(6, 6));
        mainPanel.setBackground(ATM_SCREEN_BG);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_PURPLE, 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ATM_SCREEN_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel titleLabel = new JLabel("📺 BUSINESS VIDEOS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 18));
        titleLabel.setForeground(ATM_AMBER);

        JLabel subtitleLabel = new JLabel("Discover and support local businesses", SwingConstants.CENTER);
        subtitleLabel.setFont(FONT_SMALL);
        subtitleLabel.setForeground(ATM_GREEN_DIM);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(0.7);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(ATM_SCREEN_BG);

        List<Map<String, Object>> allVideos = getAllApprovedVideos();

        JPanel videoGridPanel = new JPanel(new GridBagLayout());
        videoGridPanel.setBackground(ATM_SCREEN_BG);
        videoGridPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (allVideos.isEmpty()) {
            JLabel noVideosLabel = new JLabel("🎬 No videos available. Be the first to submit!", SwingConstants.CENTER);
            noVideosLabel.setFont(new Font("Courier New", Font.BOLD, 16));
            noVideosLabel.setForeground(ATM_AMBER);
            videoGridPanel.add(noVideosLabel);
        } else {
            for (Map<String, Object> video : allVideos) {
                videoGridPanel.add(createCompactVideoCard(video));
            }
        }

        JScrollPane gridScroll = new JScrollPane(videoGridPanel);
        gridScroll.setBorder(null);
        gridScroll.getViewport().setBackground(ATM_SCREEN_BG);
        gridScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftPanel.add(gridScroll, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(15, 25, 40));
        rightPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ATM_BORDER));
        rightPanel.setPreferredSize(new Dimension(280, 0));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bottomPanel.setBackground(ATM_SCREEN_BG);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JButton submitBtn = new JButton("💰 SUBMIT VIDEO (Ksh 20)");
        submitBtn.setFont(new Font("Courier New", Font.BOLD, 10));
        submitBtn.setBackground(new Color(0, 168, 107));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        submitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitBtn.addActionListener(e -> {
            dialog.dispose();
            showVideoSubmissionForm();
        });

        JButton refreshBtn = new JButton("🔄 REFRESH");
        refreshBtn.setFont(new Font("Courier New", Font.BOLD, 10));
        refreshBtn.setBackground(BUTTON_BG);
        refreshBtn.setForeground(ATM_CYAN);
        refreshBtn.setBorder(BorderFactory.createLineBorder(ATM_CYAN, 1));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            dialog.dispose();
            showVideoPlayer();
        });

        JButton closeBtn = new JButton("✖ CLOSE");
        closeBtn.setFont(new Font("Courier New", Font.BOLD, 10));
        closeBtn.setBackground(BUTTON_BG);
        closeBtn.setForeground(ATM_RED);
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_RED, 1),
                BorderFactory.createEmptyBorder(6, 18, 6, 18)));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dialog.dispose());

        bottomPanel.add(submitBtn);
        bottomPanel.add(refreshBtn);
        bottomPanel.add(closeBtn);

        splitPane.setRightComponent(rightPanel);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }
    class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int w = target.getWidth();
                if (w == 0) w = Integer.MAX_VALUE;
                Insets ins = target.getInsets();
                int maxW = w - ins.left - ins.right - getHgap() * 2;
                int x = 0, y = ins.top + getVgap(), rowH = 0;

                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (x + d.width > maxW && x > 0) {
                        y += rowH + getVgap();
                        x = 0;
                        rowH = 0;
                    }
                    x += d.width + getHgap();
                    rowH = Math.max(rowH, d.height);
                }
                return new Dimension(w, y + rowH + getVgap() + ins.bottom);
            }
        }
    }
    private void recordPaymentForSimpleMember(int chamaId, int memberId, String memberName,
                                              double amount, String paymentMethod) {
        if (!ATMDialog.confirm(this,
                "Record payment of Ksh " + String.format("%,.0f", amount) +
                        " for\n" + memberName + " using " + paymentMethod + "?")) {
            return;
        }

        EXECUTOR.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect()) {
                conn.setAutoCommit(false);

                // Insert contribution
                PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO chama_simple_contributions (chama_id, member_id, amount, payment_method, recorded_by, contribution_date) " +
                                "VALUES (?, ?, ?, ?, ?, NOW())");
                pst.setInt(1, chamaId);
                pst.setInt(2, memberId);
                pst.setDouble(3, amount);
                pst.setString(4, paymentMethod);
                pst.setInt(5, Session.getUserId());
                pst.executeUpdate();

                // Update member total
                PreparedStatement updatePst = conn.prepareStatement(
                        "UPDATE chama_simple_members SET total_contributions = total_contributions + ? WHERE id = ?");
                updatePst.setDouble(1, amount);
                updatePst.setInt(2, memberId);
                updatePst.executeUpdate();

                conn.commit();

                soundSuccess();
                SwingUtilities.invokeLater(() -> {
                    String message = String.format(
                            "✅ Payment Recorded!\n\nChama Member: %s\nAmount: Ksh %,.0f\nMethod: %s",
                            memberName, amount, paymentMethod);
                    ATMDialog.success(ATMDashboard.this, message, "Export Complete");
                    setStatus("► Payment of Ksh " + amount + " recorded for " + memberName);
                    refreshAll();
                    refreshChamaList();
                });

            } catch (SQLException e) {
                soundError();
                SwingUtilities.invokeLater(() ->
                        ATMDialog.error(ATMDashboard.this, "Failed to record payment: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }
    // ================= VIDEO HELPER METHODS =================
    private List<Map<String, Object>> getAllApprovedVideos() {
        List<Map<String, Object>> videos = new ArrayList<>();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, video_title, business_name, video_url, video_description, " +
                             "views, submitted_at, status, is_featured, platform " +
                             "FROM video_submissions WHERE status IN ('APPROVED', 'LIVE') " +
                             "ORDER BY is_featured DESC, views DESC, submitted_at DESC")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Map<String, Object> video = new HashMap<>();
                video.put("id", rs.getInt("id"));
                video.put("video_title", rs.getString("video_title"));
                video.put("business_name", rs.getString("business_name"));
                video.put("video_url", rs.getString("video_url"));
                video.put("video_description", rs.getString("video_description"));
                video.put("views", rs.getInt("views"));
                video.put("submitted_at", rs.getString("submitted_at"));
                video.put("status", rs.getString("status"));
                video.put("is_featured", rs.getBoolean("is_featured"));
                video.put("platform", rs.getString("platform"));
                videos.add(video);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return videos;
    }

    private String getLiveVideoUrl() {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT video_url FROM video_submissions WHERE status = 'LIVE' LIMIT 1")) {
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String url = rs.getString("video_url");
                return (url != null && !url.isEmpty()) ? url : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getLiveVideoTitle() {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT video_title FROM video_submissions WHERE status = 'LIVE' LIMIT 1")) {
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("video_title");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private String getLiveBusinessName() {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT business_name FROM video_submissions WHERE status = 'LIVE' LIMIT 1")) {
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("business_name");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private void loadVideoListBackground(DefaultListModel<String> model) {
        model.clear();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, video_title, business_name, views, platform FROM video_submissions " +
                             "WHERE status IN ('APPROVED', 'LIVE') ORDER BY views DESC LIMIT 20")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String title = rs.getString("video_title");
                String business = rs.getString("business_name");
                int views = rs.getInt("views");
                String platform = rs.getString("platform");
                String display = title + " | " + (business != null ? business : "Business") +
                        " | 👁 " + views + " | " + (platform != null ? platform : "YouTube");
                model.addElement(display);
            }
            if (model.isEmpty()) {
                model.addElement("No videos available. Check back later!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            model.addElement("Error loading videos");
        }
    }

    private JPanel createCompactVideoCard(Map<String, Object> video) {
        JPanel card = new JPanel(new BorderLayout(0, 3));
        card.setBackground(new Color(20, 35, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        card.setPreferredSize(new Dimension(250, 220));
        card.setMaximumSize(new Dimension(250, 220));

        String videoTitle = (String) video.get("video_title");
        String businessName = (String) video.get("business_name");
        String videoUrl = (String) video.get("video_url");
        String videoId = extractYouTubeId(videoUrl);
        int views = video.get("views") != null ? (int) video.get("views") : 0;
        String status = (String) video.get("status");
        boolean isLive = "LIVE".equals(status);

        JPanel thumbPanel = new JPanel(new BorderLayout());
        thumbPanel.setBackground(new Color(10, 20, 35));
        thumbPanel.setPreferredSize(new Dimension(240, 130));

        JLabel thumbLabel = new JLabel();
        thumbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        thumbLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (videoId != null && !videoId.isEmpty()) {
            String thumbUrl = "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg";
            loadCompactThumbnail(thumbLabel, thumbUrl, videoUrl);
        } else {
            thumbLabel.setText("🎬");
            thumbLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
            thumbLabel.setForeground(ATM_GREEN_DIM);
        }

        thumbLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    try {
                        Desktop.getDesktop().browse(new URI(videoUrl));
                        incrementVideoViews((int) video.get("id"));
                        soundBeep();
                    } catch (Exception ex) {
                        ATMDialog.error(ATMDashboard.this, "Could not open video");
                    }
                }
            }
        });

        thumbPanel.add(thumbLabel, BorderLayout.CENTER);

        if (isLive) {
            JLabel liveBadge = new JLabel("🔴 LIVE");
            liveBadge.setFont(new Font("Courier New", Font.BOLD, 9));
            liveBadge.setForeground(ATM_RED);
            liveBadge.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            thumbPanel.add(liveBadge, BorderLayout.NORTH);
        }

        card.add(thumbPanel, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel(new BorderLayout(3, 1));
        infoPanel.setBackground(new Color(20, 35, 55));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(4, 3, 2, 3));

        JLabel titleLabel = new JLabel("<html><b>" + truncateText(videoTitle != null ? videoTitle : "Untitled", 35) + "</b></html>");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(ATM_GREEN);
        titleLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        titleLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    try {
                        Desktop.getDesktop().browse(new URI(videoUrl));
                        incrementVideoViews((int) video.get("id"));
                        soundBeep();
                    } catch (Exception ex) {
                        ATMDialog.error(ATMDashboard.this, "Could not open video");
                    }
                }
            }
        });
        infoPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel metaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        metaPanel.setBackground(new Color(20, 35, 55));

        JLabel channelLabel = new JLabel("🏢 " + (businessName != null ? truncateText(businessName, 20) : "Business"));
        channelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        channelLabel.setForeground(ATM_GREEN_DIM);
        metaPanel.add(channelLabel);

        JLabel viewsLabel = new JLabel("👁 " + formatViews(views));
        viewsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        viewsLabel.setForeground(ATM_GREEN_DIM);
        metaPanel.add(viewsLabel);

        infoPanel.add(metaPanel, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.CENTER);

        return card;
    }

    private void loadCompactThumbnail(JLabel label, String thumbUrl, String videoUrl) {
        EXECUTOR.submit(() -> {
            try {
                java.net.URL url = URI.create(thumbUrl).toURL();
                ImageIcon icon = new ImageIcon(url);
                Image scaled = icon.getImage().getScaledInstance(240, 130, Image.SCALE_SMOOTH);
                SwingUtilities.invokeLater(() -> {
                    label.setIcon(new ImageIcon(scaled));
                    label.setText("");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    label.setText("🎬");
                    label.setFont(new Font("Segoe UI", Font.BOLD, 30));
                    label.setForeground(ATM_GREEN_DIM);
                });
            }
        });
    }

    private void incrementVideoViews(int videoId) {
        EXECUTOR.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "UPDATE video_submissions SET views = views + 1 WHERE id = ?")) {
                pst.setInt(1, videoId);
                pst.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private String formatViews(int views) {
        if (views >= 1_000_000) return String.format("%.1fM", views / 1_000_000.0).replace(".0", "") + " views";
        if (views >= 1_000) return String.format("%.0fK", views / 1_000.0) + " views";
        return views + " views";
    }

    private void showVideoSubmissionForm() {
        JDialog dialog = new JDialog(this, "Submit Your Business Video", true);
        dialog.setSize(650, 750);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.setBackground(ATM_SCREEN_BG);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(ATM_SCREEN_BG);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ATM_SCREEN_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("📺 SUBMIT YOUR BUSINESS VIDEO", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 22));
        titleLabel.setForeground(ATM_AMBER);

        JLabel subtitleLabel = new JLabel("Promote your business to thousands of users for only Ksh 20 per video!", SwingConstants.CENTER);
        subtitleLabel.setFont(FONT_SMALL);
        subtitleLabel.setForeground(ATM_GREEN_DIM);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ATM_SCREEN_BG);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel titleFieldLabel = new JLabel("🎬 Video Title:");
        titleFieldLabel.setFont(FONT_LABEL);
        titleFieldLabel.setForeground(ATM_GREEN);
        formPanel.add(titleFieldLabel, gbc);
        gbc.gridx = 1;
        JTextField titleField = new JTextField(25);
        styleFormField(titleField);
        formPanel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel descFieldLabel = new JLabel("📝 Description:");
        descFieldLabel.setFont(FONT_LABEL);
        descFieldLabel.setForeground(ATM_GREEN);
        formPanel.add(descFieldLabel, gbc);
        gbc.gridx = 1;
        JTextArea descArea = new JTextArea(3, 25);
        descArea.setBackground(new Color(15, 30, 45));
        descArea.setForeground(ATM_GREEN);
        descArea.setCaretColor(ATM_GREEN);
        descArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        formPanel.add(descScroll, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel businessFieldLabel = new JLabel("🏢 Business Name:");
        businessFieldLabel.setFont(FONT_LABEL);
        businessFieldLabel.setForeground(ATM_GREEN);
        formPanel.add(businessFieldLabel, gbc);
        gbc.gridx = 1;
        JTextField businessField = new JTextField(25);
        styleFormField(businessField);
        formPanel.add(businessField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        JLabel platformLabel = new JLabel("📱 Platform:");
        platformLabel.setFont(FONT_LABEL);
        platformLabel.setForeground(ATM_GREEN);
        formPanel.add(platformLabel, gbc);
        gbc.gridx = 1;
        JComboBox<String> platformCombo = new JComboBox<>(new String[]{"📺 YOUTUBE", "🎵 TIKTOK", "📘 FACEBOOK", "📸 INSTAGRAM"});
        styleComboBox(platformCombo);
        formPanel.add(platformCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        JLabel urlFieldLabel = new JLabel("🔗 Video URL:");
        urlFieldLabel.setFont(FONT_LABEL);
        urlFieldLabel.setForeground(ATM_GREEN);
        formPanel.add(urlFieldLabel, gbc);
        gbc.gridx = 1;
        JTextField urlField = new JTextField(25);
        styleFormField(urlField);
        urlField.setToolTipText("Paste your YouTube, TikTok, or other video URL");
        formPanel.add(urlField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        JLabel mpesaLabel = new JLabel("💳 M-Pesa Code:");
        mpesaLabel.setFont(FONT_LABEL);
        mpesaLabel.setForeground(ATM_GREEN);
        formPanel.add(mpesaLabel, gbc);
        gbc.gridx = 1;
        JTextField mpesaField = new JTextField(25);
        styleFormField(mpesaField);
        mpesaField.setToolTipText("Enter the M-Pesa confirmation code after payment");
        formPanel.add(mpesaField, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        JPanel feePanel = new JPanel(new BorderLayout(10, 5));
        feePanel.setBackground(new Color(15, 30, 45));
        feePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_CYAN, 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JPanel feeTextPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        feeTextPanel.setBackground(new Color(15, 30, 45));

        JLabel feeLabel = new JLabel("💰 Fee: Ksh 20 per video - Submit as many videos as you want!");
        feeLabel.setFont(new Font("Courier New", Font.BOLD, 14));
        feeLabel.setForeground(ATM_AMBER);

        JLabel feeHint = new JLabel("⭐ More videos = More exposure for your business!");
        feeHint.setFont(FONT_SMALL);
        feeHint.setForeground(ATM_GREEN);

        feeTextPanel.add(feeLabel);
        feeTextPanel.add(feeHint);

        feePanel.add(feeTextPanel, BorderLayout.CENTER);
        formPanel.add(feePanel, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);

        JButton submitBtn = new JButton("✓ SUBMIT VIDEO (Ksh 20)");
        submitBtn.setFont(new Font("Courier New", Font.BOLD, 14));
        submitBtn.setBackground(new Color(0, 168, 107));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.setPreferredSize(new Dimension(220, 48));
        submitBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        submitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { submitBtn.setBackground(new Color(0, 140, 90)); }
            public void mouseExited(MouseEvent e) { submitBtn.setBackground(new Color(0, 168, 107)); }
        });

        JButton cancelBtn = new JButton("✗ CANCEL");
        cancelBtn.setFont(new Font("Courier New", Font.BOLD, 14));
        cancelBtn.setBackground(new Color(60, 65, 70));
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setPreferredSize(new Dimension(180, 48));
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_RED, 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { cancelBtn.setBackground(ATM_RED); cancelBtn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { cancelBtn.setBackground(new Color(60, 65, 70)); cancelBtn.setForeground(ATM_RED); }
        });

        submitBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String description = descArea.getText().trim();
            String business = businessField.getText().trim();
            String platform = ((String) platformCombo.getSelectedItem()).replace("📺 ", "").replace("🎵 ", "").replace("📘 ", "").replace("📸 ", "");
            String url = urlField.getText().trim();
            String mpesa = mpesaField.getText().trim();

            if (title.isEmpty() || url.isEmpty() || mpesa.isEmpty()) {
                ATMDialog.error(ATMDashboard.this, "Please fill in all required fields");
                return;
            }

            if (!isValidUrl(url)) {
                ATMDialog.error(ATMDashboard.this, "Please enter a valid URL");
                return;
            }

            submitBtn.setEnabled(false);
            submitBtn.setText("⏳ SUBMITTING...");

            EXECUTOR.submit(() -> {
                boolean saved = saveVideoSubmission(title, business, description, platform, url, mpesa);
                SwingUtilities.invokeLater(() -> {
                    submitBtn.setEnabled(true);
                    submitBtn.setText("✓ SUBMIT VIDEO (Ksh 20)");

                    if (saved) {
                        soundSuccess();
                        ATMDialog.success(ATMDashboard.this,
                                "✓ Video submitted successfully!\n\nAdmin will review within 24 hours.",
                                "Export Complete");
                        dialog.dispose();
                    } else {
                        soundError();
                        ATMDialog.error(ATMDashboard.this, "Submission failed. Please try again.");
                    }
                });
            });
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(submitBtn);
        buttonPanel.add(cancelBtn);
        formPanel.add(buttonPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    private boolean saveVideoSubmission(String title, String business, String description, String platform, String url, String mpesaCode) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO video_submissions (user_id, user_name, business_name, video_title, " +
                             "video_description, platform, video_url, mpesa_code, payment_status, payment_amount, status, submitted_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PAID', 20.00, 'PENDING', NOW())")) {
            pst.setInt(1, Session.userId);
            pst.setString(2, Session.username);
            pst.setString(3, business.isEmpty() ? Session.username + "'s Business" : business);
            pst.setString(4, title);
            pst.setString(5, description.isEmpty() ? "Great business video!" : description);
            pst.setString(6, platform);
            pst.setString(7, url);
            pst.setString(8, mpesaCode);
            int result = pst.executeUpdate();

            if (result > 0) {
                NotificationService.create(1, "📹 New video submission from " + Session.username + ": " + title, NotificationService.INFO);
            }
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error saving video submission: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    // ================= PROFILE SCREEN =================
    private JPanel buildProfileScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ATM_SCREEN_BG);
        screen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 10, 4, 10, ATM_BG),
                BorderFactory.createLineBorder(ATM_AMBER, 1)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ATM_SCREEN_BG);
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("  ◈ USER PROFILE  ◈");
        title.setFont(FONT_HEAD);
        title.setForeground(ATM_AMBER);

        JButton backBtn = atmButton("◄ BACK");
        backBtn.addActionListener(e -> {
            soundBeep();
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, CARD_MAIN);
        });

        header.add(title, BorderLayout.CENTER);
        header.add(backBtn, BorderLayout.EAST);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ATM_SCREEN_BG);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel picDisplay = new JLabel();
        picDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        picDisplay.setPreferredSize(new Dimension(100, 100));
        loadProfilePicDisplay(picDisplay);

        JPanel picRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        picRow.setBackground(ATM_SCREEN_BG);

        JButton uploadPicBtn = createProfileButton("📷 UPLOAD PHOTO", ATM_CYAN);
        JButton removePicBtn = createProfileButton("🗑 REMOVE", ATM_RED);

        uploadPicBtn.addActionListener(e -> { soundBeep(); uploadProfilePicture(picDisplay); });
        removePicBtn.addActionListener(e -> {
            soundBeep();
            if (!ATMDialog.confirm(this, "Remove your profile picture?")) return;
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement("UPDATE users SET profile_pic=NULL WHERE id=?")) {
                pst.setInt(1, Session.userId);
                pst.executeUpdate();
                loadProfilePicDisplay(picDisplay);
                setStatus("► PROFILE PICTURE REMOVED");
            } catch (SQLException ex) { soundError(); }
        });

        picRow.add(uploadPicBtn);
        picRow.add(removePicBtn);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(picDisplay, gbc);

        gbc.gridy = 1;
        formPanel.add(picRow, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        JLabel section1 = createSectionLabel("📋 PERSONAL INFORMATION");
        formPanel.add(section1, gbc);

        String[] occupations = {"Not specified","Student","Teacher","Doctor/Nurse","Engineer/IT","Business Owner","Farmer","ICT Technician","Other"};
        occupationCombo = new JComboBox<>(occupations);
        styleComboBox(occupationCombo);

        monthlyIncomeField = createStyledTextField();
        monthlyExpensesField = createStyledTextField();

        String[] goalTypes = {"General","Business","Education","Land","Emergency Fund","House","Vehicle"};
        goalTypeCombo = new JComboBox<>(goalTypes);
        styleComboBox(goalTypeCombo);

        JTextField emailField = createStyledTextField();
        emailField.setToolTipText("Email for OTP and notifications");

        gbc.gridy = 3;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createFieldLabel("Occupation:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(occupationCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createFieldLabel("Monthly Income:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(monthlyIncomeField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createFieldLabel("Monthly Expenses:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(monthlyExpensesField, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createFieldLabel("Goal Type:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(goalTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createFieldLabel("Email Address:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(emailField, gbc);

        gbc.gridy = 8;
        JButton verifyEmailBtn = createProfileButton("✓ VERIFY EMAIL", ATM_BLUE);
        verifyEmailBtn.addActionListener(e -> {
            soundBeep();
            String email = emailField.getText().trim();
            if (email.isEmpty() || !email.contains("@")) {
                ATMDialog.error(this, "Enter a valid email address first.");
                return;
            }
            setStatus("► SENDING VERIFICATION CODE...");
            EXECUTOR.submit(() -> {
                boolean sent = OTPService.sendEmailVerification(Session.userId, email);
                SwingUtilities.invokeLater(() -> {
                    if (!sent) {
                        soundError();
                        ATMDialog.error(this, "Failed to send verification email.");
                        return;
                    }
                    String code = ATMDialog.passwordInput(ATMDashboard.this,
                            "Enter the 6-digit verification code\nsent to: " + email);
                    if (code == null || code.trim().isEmpty()) return;

                    if (OTPService.confirmEmailToken(Session.userId, code)) {
                        soundSuccess();
                        ATMDialog.success(ATMDashboard.this, "Email verified successfully!", "Export Complete");
                        NotificationService.create(Session.userId, "Email " + email + " verified successfully.", NotificationService.SUCCESS);
                        updateNotificationBadge();
                        setStatus("► EMAIL VERIFIED: " + email);
                    } else {
                        soundError();
                        ATMDialog.error(ATMDashboard.this, "Invalid or expired code. Try again.");
                    }
                });
            });
        });
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(verifyEmailBtn, gbc);

        gbc.gridy = 9;
        JButton securityQuestionsBtn = createProfileButton("🔐 SETUP SECURITY QUESTIONS", ATM_PURPLE);
        securityQuestionsBtn.addActionListener(e -> { soundBeep(); setupSecurityQuestions(); });
        formPanel.add(securityQuestionsBtn, gbc);

        gbc.gridy = 10;
        JButton saveProfileBtn = createProfileButton("💾 SAVE PROFILE", ATM_GREEN);
        saveProfileBtn.setFont(new Font("Courier New", Font.BOLD, 14));
        saveProfileBtn.setPreferredSize(new Dimension(200, 45));
        saveProfileBtn.addActionListener(e -> {
            soundBeep();
            saveUserProfile(occupationCombo, monthlyIncomeField, monthlyExpensesField, goalTypeCombo);
        });
        formPanel.add(saveProfileBtn, gbc);

        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT occupation, monthly_income, monthly_expenses, goal_type, email FROM users WHERE id=?")) {
            pst.setInt(1, Session.userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                occupationCombo.setSelectedItem(rs.getString("occupation"));
                monthlyIncomeField.setText(String.valueOf(rs.getInt("monthly_income")));
                monthlyExpensesField.setText(String.valueOf(rs.getInt("monthly_expenses")));
                goalTypeCombo.setSelectedItem(rs.getString("goal_type"));
                emailField.setText(rs.getString("email") != null ? rs.getString("email") : "");
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(ATM_SCREEN_BG);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        screen.add(header, BorderLayout.NORTH);
        screen.add(formScroll, BorderLayout.CENTER);
        return screen;
    }

    private JButton createProfileButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(BUTTON_BG);
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(BUTTON_HOVER); btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BUTTON_BG); btn.setForeground(color); }
        });
        return btn;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(ATM_GREEN_DIM);
        return label;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_HEAD);
        label.setForeground(ATM_AMBER);
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ATM_BORDER));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField(15);
        field.setFont(FONT_SMALL);
        field.setBackground(new Color(15, 30, 45));
        field.setForeground(ATM_GREEN);
        field.setCaretColor(ATM_GREEN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }

    private void saveUserProfile(JComboBox<String> occupationCombo, JTextField monthlyIncomeField,
                                 JTextField monthlyExpensesField, JComboBox<String> goalTypeCombo) {
        try {
            String occupation = (String) occupationCombo.getSelectedItem();
            int income = Integer.parseInt(monthlyIncomeField.getText().trim());
            int expenses = Integer.parseInt(monthlyExpensesField.getText().trim());
            String goalType = (String) goalTypeCombo.getSelectedItem();

            if (income < 0 || expenses < 0) {
                soundError();
                ATMDialog.error(this, "Income and expenses cannot be negative.");
                return;
            }

            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "UPDATE users SET occupation=?, monthly_income=?, monthly_expenses=?, goal_type=? WHERE id=?")) {
                pst.setString(1, occupation);
                pst.setInt(2, income);
                pst.setInt(3, expenses);
                pst.setString(4, goalType);
                pst.setInt(5, Session.userId);
                pst.executeUpdate();

                soundSuccess();
                ATMDialog.success(this, "Profile saved successfully!", "Export Complete");
                setStatus("► PROFILE UPDATED  [" + LocalDateTime.now().format(DT_FORMATTER) + "]");

            } catch (SQLException ex) {
                soundError();
                ATMDialog.error(this, "Database error:\n" + ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            soundError();
            ATMDialog.error(this, "Please enter valid numbers for income and expenses.");
        }
    }

    private void loadProfilePicDisplay(JLabel picLabel) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT profile_pic FROM users WHERE id=?")) {
            pst.setInt(1, Session.userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                byte[] bytes = rs.getBytes("profile_pic");
                if (bytes != null && bytes.length > 0) {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (img != null) {
                        BufferedImage circle = new BufferedImage(90, 90, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = circle.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, 90, 90));
                        g2.drawImage(img.getScaledInstance(90, 90, Image.SCALE_SMOOTH), 0, 0, null);
                        g2.dispose();
                        picLabel.setIcon(new ImageIcon(circle));
                        picLabel.setText("");
                        return;
                    }
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        picLabel.setIcon(null);
        picLabel.setText("NO PHOTO");
        picLabel.setFont(FONT_SMALL);
        picLabel.setForeground(ATM_GREEN_DIM);
    }

    private void uploadProfilePicture(JLabel picDisplay) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Profile Picture");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Images (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            BufferedImage orig = ImageIO.read(fc.getSelectedFile());
            if (orig == null) {
                soundError();
                ATMDialog.error(this, "Could not read image file.");
                return;
            }
            BufferedImage resized = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(orig.getScaledInstance(200, 200, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            byte[] imgBytes = baos.toByteArray();

            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement("UPDATE users SET profile_pic=?, pic_type='jpg' WHERE id=?")) {
                pst.setBytes(1, imgBytes);
                pst.setInt(2, Session.userId);
                pst.executeUpdate();
            }
            soundSuccess();
            loadProfilePicDisplay(picDisplay);
            setStatus("► PROFILE PICTURE UPDATED  [" + LocalDateTime.now().format(DT_FORMATTER) + "]");
        } catch (Exception ex) {
            soundError();
            ATMDialog.error(this, "Upload failed:\n" + ex.getMessage());
        }
    }

    private void setupSecurityQuestions() {
        if (hasSecurityQuestions()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "You already have a security question set up.\nDo you want to change it?",
                    "Security Question", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        JDialog questionDialog = new JDialog(this, "Setup Security Question", true);
        questionDialog.setSize(500, 400);
        questionDialog.setLocationRelativeTo(this);
        questionDialog.setUndecorated(true);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ATM_SCREEN_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JLabel titleLabel = new JLabel("◈ SECURITY QUESTION ◈");
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 16));
        titleLabel.setForeground(ATM_AMBER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel infoLabel = new JLabel("This will help you reset your password if you forget it");
        infoLabel.setFont(FONT_SMALL);
        infoLabel.setForeground(ATM_GREEN_DIM);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 20, 0));

        JLabel questionLabel = new JLabel("Choose a security question:");
        questionLabel.setFont(FONT_LABEL);
        questionLabel.setForeground(ATM_GREEN);
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String[] questions = {
                "What is your mother's maiden name?",
                "What was the name of your first pet?",
                "What city were you born in?",
                "What is your favorite book?",
                "What is your favorite color?",
                "What is the name of your best friend?"
        };
        JComboBox<String> questionCombo = new JComboBox<>(questions);
        questionCombo.setFont(new Font("Courier New", Font.PLAIN, 13));
        questionCombo.setBackground(new Color(15, 30, 45));
        questionCombo.setForeground(ATM_GREEN);
        questionCombo.setMaximumSize(new Dimension(350, 35));
        questionCombo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel answerLabel = new JLabel("Your answer:");
        answerLabel.setFont(FONT_LABEL);
        answerLabel.setForeground(ATM_GREEN);
        answerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        answerLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JTextField answerField = new JTextField(20);
        styleField(answerField);
        answerField.setMaximumSize(new Dimension(350, 40));
        answerField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(FONT_SMALL);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setForeground(ATM_RED);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);

        JButton saveBtn = createStyledButton("SAVE", ATM_GREEN);
        JButton cancelBtn = createStyledButton("CANCEL", ATM_RED);

        saveBtn.addActionListener(e -> {
            String selectedQuestion = (String) questionCombo.getSelectedItem();
            String answer = answerField.getText().trim().toLowerCase();

            if (answer.isEmpty()) {
                errorLabel.setText("⚠️ Please enter your answer");
                errorLabel.setForeground(ATM_RED);
                return;
            }

            if (saveSingleSecurityQuestion(selectedQuestion, answer)) {
                soundSuccess();
                ATMDialog.success(this, "✓ Security question saved successfully!", "Export Complete");
                questionDialog.dispose();
            } else {
                soundError();
                ATMDialog.error(this, "Failed to save security question");
            }
        });

        cancelBtn.addActionListener(e -> questionDialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        panel.add(titleLabel);
        panel.add(infoLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(questionLabel);
        panel.add(questionCombo);
        panel.add(answerLabel);
        panel.add(answerField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(errorLabel);
        panel.add(buttonPanel);

        questionDialog.add(panel);
        questionDialog.setVisible(true);
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("Courier New", Font.PLAIN, 14));
        field.setBackground(new Color(15, 30, 45));
        field.setForeground(ATM_GREEN);
        field.setCaretColor(ATM_GREEN);
        field.setMaximumSize(new Dimension(300, 40));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(BUTTON_BG);
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(8, 25, 8, 25)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(color); btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BUTTON_BG); btn.setForeground(color); }
        });
        return btn;
    }

    private boolean hasSecurityQuestions() {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id FROM security_questions WHERE user_id = ? AND question1 IS NOT NULL")) {
            pst.setInt(1, Session.getUserId());
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean saveSingleSecurityQuestion(String question, String answer) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement deletePst = conn.prepareStatement(
                    "DELETE FROM security_questions WHERE user_id = ?");
            deletePst.setInt(1, Session.getUserId());
            deletePst.executeUpdate();

            PreparedStatement insertPst = conn.prepareStatement(
                    "INSERT INTO security_questions (user_id, question1, answer1) VALUES (?, ?, ?)");
            insertPst.setInt(1, Session.getUserId());
            insertPst.setString(2, question);
            insertPst.setString(3, answer);
            insertPst.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================= SET GOAL DIALOG =================
    private void showSetGoalDialog() {
        JDialog goalDialog = new JDialog(this, "Set Savings Goal", true);
        goalDialog.setSize(550, 520);
        goalDialog.setLocationRelativeTo(this);
        goalDialog.setUndecorated(true);

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        dialogPanel.setBackground(ATM_SCREEN_BG);
        dialogPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ATM_SCREEN_BG);
        JLabel titleLabel = new JLabel("◈ SET YOUR SAVINGS GOAL ◈", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Courier New", Font.BOLD, 20));
        titleLabel.setForeground(ATM_AMBER);
        JLabel subtitleLabel = new JLabel("Define your financial target", SwingConstants.CENTER);
        subtitleLabel.setFont(FONT_SMALL);
        subtitleLabel.setForeground(ATM_GREEN_DIM);
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        dialogPanel.add(titlePanel);
        dialogPanel.add(Box.createVerticalStrut(10));

        double currentGoalValue = getCurrentGoalValue();
        JPanel currentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        currentPanel.setBackground(ATM_SCREEN_BG);
        currentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_GREEN_DIM, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        JLabel currentLabel = new JLabel("📊 CURRENT GOAL:");
        currentLabel.setFont(FONT_BOLD);
        currentLabel.setForeground(ATM_GREEN);
        JLabel currentValueLabel = new JLabel("Ksh " + String.format("%,.0f", currentGoalValue));
        currentValueLabel.setFont(new Font("Courier New", Font.BOLD, 18));
        currentValueLabel.setForeground(ATM_AMBER);
        currentPanel.add(currentLabel);
        currentPanel.add(currentValueLabel);
        dialogPanel.add(currentPanel);
        dialogPanel.add(Box.createVerticalStrut(15));

        JPanel inputContainer = new JPanel();
        inputContainer.setLayout(new BoxLayout(inputContainer, BoxLayout.Y_AXIS));
        inputContainer.setBackground(ATM_SCREEN_BG);
        JLabel amountLabel = new JLabel("💰 NEW GOAL AMOUNT (Ksh):");
        amountLabel.setFont(new Font("Courier New", Font.BOLD, 14));
        amountLabel.setForeground(ATM_GREEN_DIM);
        amountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JTextField goalField = new JTextField(15);
        goalField.setFont(new Font("Courier New", Font.BOLD, 24));
        goalField.setBackground(new Color(15, 30, 45));
        goalField.setForeground(ATM_GREEN);
        goalField.setCaretColor(ATM_GREEN);
        goalField.setHorizontalAlignment(JTextField.CENTER);
        goalField.setMaximumSize(new Dimension(350, 60));
        goalField.setText(String.format("%.0f", currentGoalValue));
        goalField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_AMBER, 2),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        goalField.setAlignmentX(Component.CENTER_ALIGNMENT);
        goalField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { goalField.selectAll(); }
        });
        inputContainer.add(amountLabel);
        inputContainer.add(Box.createVerticalStrut(10));
        inputContainer.add(goalField);
        inputContainer.add(Box.createVerticalStrut(15));
        dialogPanel.add(inputContainer);

        JLabel quickLabel = new JLabel("Quick Select:");
        quickLabel.setFont(FONT_SMALL);
        quickLabel.setForeground(ATM_GREEN_DIM);
        quickLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        dialogPanel.add(quickLabel);
        dialogPanel.add(Box.createVerticalStrut(5));

        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        quickPanel.setBackground(ATM_SCREEN_BG);
        int[] quickAmounts = {5000, 10000, 20000, 50000, 100000, 250000, 500000, 1000000};
        String[] quickLabels = {"5,000", "10,000", "20,000", "50,000", "100k", "250k", "500k", "1M"};
        for (int i = 0; i < quickAmounts.length; i++) {
            final int amount = quickAmounts[i];
            JButton quickBtn = new JButton(quickLabels[i]);
            quickBtn.setFont(new Font("Courier New", Font.BOLD, 12));
            quickBtn.setBackground(BUTTON_BG);
            quickBtn.setForeground(ATM_CYAN);
            quickBtn.setFocusPainted(false);
            quickBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ATM_CYAN, 1),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)));
            quickBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            quickBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { quickBtn.setBackground(new Color(0, 168, 107)); quickBtn.setForeground(Color.WHITE); }
                public void mouseExited(MouseEvent e) { quickBtn.setBackground(BUTTON_BG); quickBtn.setForeground(ATM_CYAN); }
            });
            quickBtn.addActionListener(e -> {
                goalField.setText(String.valueOf(amount));
                goalField.requestFocus();
                goalField.selectAll();
                soundBeep();
            });
            quickPanel.add(quickBtn);
        }
        dialogPanel.add(quickPanel);
        dialogPanel.add(Box.createVerticalStrut(10));

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(new Color(15, 30, 45));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_CYAN, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        JLabel infoIcon = new JLabel("💡", SwingConstants.CENTER);
        infoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        JTextArea infoText = new JTextArea(
                "TIP: Set a realistic goal based on your income.\n" +
                        "• Monthly income × 6 = Emergency Fund\n" +
                        "• Break large goals into smaller milestones");
        infoText.setFont(FONT_SMALL);
        infoText.setForeground(ATM_GREEN);
        infoText.setBackground(new Color(15, 30, 45));
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoPanel.add(infoIcon, BorderLayout.WEST);
        infoPanel.add(infoText, BorderLayout.CENTER);
        dialogPanel.add(infoPanel);
        dialogPanel.add(Box.createVerticalStrut(15));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(ATM_SCREEN_BG);
        JButton saveBtn = new JButton("✓ SAVE GOAL");
        saveBtn.setFont(new Font("Courier New", Font.BOLD, 14));
        saveBtn.setBackground(BUTTON_BG);
        saveBtn.setForeground(ATM_GREEN);
        saveBtn.setFocusPainted(false);
        saveBtn.setPreferredSize(new Dimension(160, 50));
        saveBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_GREEN, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JButton cancelBtn = new JButton("✗ CANCEL");
        cancelBtn.setFont(new Font("Courier New", Font.BOLD, 14));
        cancelBtn.setBackground(BUTTON_BG);
        cancelBtn.setForeground(ATM_RED);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setPreferredSize(new Dimension(160, 50));
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_RED, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        saveBtn.addActionListener(e -> {
            String amountStr = goalField.getText().trim().replace(",", "").replace("Ksh", "").trim();
            if (amountStr.isEmpty()) {
                JOptionPane.showMessageDialog(goalDialog, "⚠️ Please enter an amount", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double newGoal = Double.parseDouble(amountStr);
                if (newGoal <= 0) throw new NumberFormatException();
                if (newGoal > 10_000_000) {
                    JOptionPane.showMessageDialog(goalDialog, "⚠️ Goal cannot exceed Ksh 10,000,000", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                saveBtn.setEnabled(false);
                saveBtn.setText("⏳ SAVING...");
                EXECUTOR.submit(() -> {
                    boolean saved = saveUserGoal(newGoal);
                    SwingUtilities.invokeLater(() -> {
                        if (saved) {
                            soundSuccess();
                            goalDialog.dispose();
                            refreshAll();
                            setStatus("► Savings goal updated to Ksh " + String.format("%,.0f", newGoal));
                        } else {
                            JOptionPane.showMessageDialog(goalDialog, "❌ Failed to save goal.", "Error", JOptionPane.ERROR_MESSAGE);
                            saveBtn.setEnabled(true);
                            saveBtn.setText("✓ SAVE GOAL");
                        }
                    });
                });
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(goalDialog, "⚠️ Please enter a valid number", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> {
            soundBeep();
            goalDialog.dispose();
        });
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        dialogPanel.add(buttonPanel);
        goalField.addActionListener(e -> saveBtn.doClick());
        goalDialog.setContentPane(dialogPanel);
        goalDialog.setVisible(true);
    }

    private double getCurrentGoalValue() {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT COALESCE(savings_goal, 15000) FROM users WHERE id = ? LIMIT 1")) {
            pst.setInt(1, Session.userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 15000;
    }

    private boolean saveUserGoal(double goal) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("UPDATE users SET savings_goal = ? WHERE id = ?")) {
            pst.setDouble(1, goal);
            pst.setInt(2, Session.userId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ================= LANGUAGE TOGGLE =================
    private void toggleLanguage() {
        isKiswahili = !isKiswahili;
        if (isKiswahili) {
            bankTitleLabel.setText("◈ SUPREME MONEY COACH - MKUFUNZI WA FEDHA ◈");
            balTitleLbl.setText("SALIO");
            goalTitleLbl.setText("LENGO");
            remTitleLbl.setText("KILICHOSALIA");
            progressTitleLbl.setText("MAENDELEO:");
            miniStatTitleLbl.setText("SHUGHULI ZA HIVI PUNDE (3)");
            withdrawBtn.setText("TOA");
            customBtn.setText("WEKA");
            statementBtn.setText("TAARIFA");
            langBtn.setText("🌍 ENGLISH");
        } else {
            bankTitleLabel.setText("◈ SUPREME MONEY COACH ◈");
            balTitleLbl.setText("BALANCE");
            goalTitleLbl.setText("GOAL");
            remTitleLbl.setText("REMAINING");
            progressTitleLbl.setText("PROGRESS:");
            miniStatTitleLbl.setText("RECENT TXNS (LAST 3)");
            withdrawBtn.setText("WITHDRAW");
            customBtn.setText("DEPOSIT");
            statementBtn.setText("STATEMENT");
            langBtn.setText("🌍 KISWAHILI");
        }
        soundBeep();
        revalidate();
        repaint();
        setStatus(isKiswahili ? "► LUGHA: KISWAHILI" : "► LANGUAGE: ENGLISH");
    }

    // ================= EXIT =================
    private void exitWithAnimation() {
        soundExit();
        JDialog exitDlg = new JDialog(this, true);
        exitDlg.setUndecorated(true);
        exitDlg.setSize(420, 220);
        exitDlg.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(ATM_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ATM_RED, 3),
                BorderFactory.createEmptyBorder(24, 30, 24, 30)));
        JLabel titleLbl = new JLabel("ENDING SESSION", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Courier New", Font.BOLD, 22));
        titleLbl.setForeground(ATM_RED);
        JLabel subLbl = new JLabel("Please take your card. Thank you, " + Session.getUsername().toUpperCase() + "!", SwingConstants.CENTER);
        subLbl.setFont(FONT_SMALL);
        subLbl.setForeground(ATM_GREEN_DIM);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setForeground(ATM_RED);
        bar.setBackground(new Color(30, 5, 5));
        bar.setBorder(BorderFactory.createLineBorder(ATM_RED));
        bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(0, 8));
        panel.add(bar, BorderLayout.NORTH);
        panel.add(titleLbl, BorderLayout.CENTER);
        panel.add(subLbl, BorderLayout.SOUTH);
        exitDlg.setContentPane(panel);
        Timer timer = new Timer(30, null);
        int[] progress = {0};
        timer.addActionListener(e -> {
            progress[0] += 2;
            bar.setValue(Math.min(progress[0], 100));
            if (progress[0] >= 100) {
                timer.stop();
                exitDlg.dispose();
                dispose();
                SwingUtilities.invokeLater(() -> new LoginForm());
            }
        });
        timer.start();
        exitDlg.setVisible(true);
    }

    // ================= CLOCK & NOTIFICATIONS =================
    private void startClock() {
        Timer clockTimer = new Timer(1000, e ->
                clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
        );
        clockTimer.start();

        Timer badgeTimer = new Timer(120000, e -> updateNotificationBadge());
        badgeTimer.start();
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        SwingUtilities.invokeLater(() -> {
            try {
                int count = NotificationService.getUnreadCount(Session.userId);
                if (count > 0) {
                    notificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    notificationBadge.setVisible(true);
                    notificationBellBtn.setText("🔔");
                } else {
                    notificationBadge.setVisible(false);
                    notificationBellBtn.setText("🔕");
                }
            } catch (Exception ignored) {}
        });
    }

    private void setStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("  " + message));
    }

    // ================= NOTIFICATION SERVICE INNER CLASS =================
    public static class NotificationService {
        public static final String SUCCESS = "SUCCESS";
        public static final String WARNING = "WARNING";
        public static final String INFO = "INFO";
        public static final String ALERT = "ALERT";

        public static void create(int userId, String message, String type) {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "INSERT INTO notifications (user_id, message, type, created_at, is_read) VALUES (?, ?, ?, NOW(), 0)")) {
                pst.setInt(1, userId);
                pst.setString(2, message);
                pst.setString(3, type);
                pst.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static List<String[]> getAll(int userId) {
            List<String[]> notifications = new ArrayList<>();
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT id, message, type, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50")) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    notifications.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("message"),
                            rs.getString("type"),
                            String.valueOf(rs.getBoolean("is_read")),
                            rs.getString("created_at")
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return notifications;
        }

        public static void markAllRead(int userId) {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0")) {
                pst.setInt(1, userId);
                pst.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static void clearAll(int userId) {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement("DELETE FROM notifications WHERE user_id = ?")) {
                pst.setInt(1, userId);
                pst.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static int getUnreadCount(int userId) {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0")) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    // ================= MAIN =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm());
    }
}