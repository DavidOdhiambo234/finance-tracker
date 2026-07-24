import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

public class AdminDashboard extends JFrame {

    // =====================================================
    //  COLORS
    // =====================================================
    private static final Color BG          = new Color(13, 17, 23);
    private static final Color SIDEBAR_BG  = new Color(22, 27, 34);
    static final Color CARD_BG     = new Color(33, 38, 45);
    private static final Color BORDER_COL  = new Color(48, 54, 61);
    private static final Color BLUE        = new Color(88, 166, 255);
    private static final Color GREEN       = new Color(63, 185, 80);
    private static final Color RED         = new Color(248, 81, 73);
    private static final Color YELLOW      = new Color(210, 153, 34);
    private static final Color PURPLE      = new Color(188, 140, 255);
    private static final Color CYAN        = new Color(0, 210, 210);
    private static final Color TEXT        = new Color(201, 209, 217);
    private static final Color TEXT_DIM    = new Color(110, 118, 129);
    private static final Color TEXT_BRIGHT = new Color(240, 246, 252);
    static final Color ROW_ALT     = new Color(22, 27, 34);

    // =====================================================
    //  FONTS
    // =====================================================
    private static final Font F_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font F_HEAD  = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font F_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_STAT  = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font F_BTN   = new Font("Segoe UI", Font.BOLD, 12);
    static final Font F_MONO  = new Font("Courier New", Font.PLAIN, 11);

    // =====================================================
    //  PANEL CONSTANTS
    // =====================================================
    private static final String P_OVERVIEW   = "OVERVIEW";
    private static final String P_USERS      = "USERS";
    private static final String P_REQUESTS   = "REQUESTS";
    private static final String P_PICTURES   = "PICTURES";
    private static final String P_ANALYTICS  = "ANALYTICS";
    private static final String P_ANNOUNCE   = "ANNOUNCE";
    private static final String P_DEBTS      = "DEBTS";
    private static final String P_TXNS       = "TXNS";
    private static final String P_VIDEO      = "VIDEO";

    // =====================================================
    //  THREAD POOL FOR BACKGROUND TASKS
    // =====================================================
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    // =====================================================
    //  SIMPLE CACHE
    // =====================================================
    private static final Map<String, Object> cache = new ConcurrentHashMap<>();
    private static final int CACHE_TTL = 60_000; // 60 seconds

    // =====================================================
    //  COMPONENT REFERENCES
    // =====================================================
    private JPanel     contentArea;
    private CardLayout cardLayout;
    private JButton[]  sidebarBtns;
    private String     activePanel = P_OVERVIEW;

    // Overview stats - ENHANCED
    private JLabel lblTotalUsers, lblVerifiedEmails, lblProfilePics, lblActiveUsers;
    private JLabel lblAIConversations, lblPdfReports, lblChamasCreated, lblActiveToday;
    private boolean statsLoaded = false;

    // Other panels
    private DefaultTableModel usersModel, requestsModel, txnsModel, debtsModel;
    private JTable usersTable, requestsTable, txnsTable, debtsTable;
    private boolean usersLoaded = false, requestsLoaded = false, txnsLoaded = false, debtsLoaded = false;
    private JPanel picturesGrid;
    private boolean picturesLoaded = false;

    // Video stats
    private JLabel lblVideoPending, lblVideoApproved, lblVideoEarned, lblVideoViews;
    private DefaultTableModel pendingModel, approvedModel, liveModel, rejectedModel;
    // Video stats - additional
    private JLabel lblVideoLive;
    private JLabel lblTotalSubmissions;

    // =====================================================
    //  CONSTRUCTOR
    // =====================================================
    public AdminDashboard() {
        if (!checkIsAdmin()) {
            JOptionPane.showMessageDialog(null, "Access Denied! Admin privileges required.",
                    "Unauthorized", JOptionPane.ERROR_MESSAGE);
            dispose();
            SwingUtilities.invokeLater(() -> new LoginForm());
            return;
        }

        setTitle("Supreme Money Coach — Admin Panel");
        setSize(1280, 840);
        setMinimumSize(new Dimension(1050, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.add(buildTopBar(),      BorderLayout.NORTH);
        root.add(buildSidebar(),     BorderLayout.WEST);
        root.add(buildContentArea(), BorderLayout.CENTER);
        setContentPane(root);
        setVisible(true);

        // Load initial data in background
        loadOverviewStats();

        // Refresh every 5 minutes (reduced from 1 minute)
        new javax.swing.Timer(300_000, e -> {
            if (P_OVERVIEW.equals(activePanel)) {
                cache.clear(); // Clear cache on refresh
                loadOverviewStats();
            }
        }).start();
    }

    // =====================================================
    //  ADMIN CHECK
    // =====================================================
    private boolean checkIsAdmin() {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT is_admin FROM users WHERE id=? LIMIT 1")) {
            pst.setInt(1, Session.userId);
            ResultSet rs = pst.executeQuery();
            return rs.next() && rs.getBoolean("is_admin");
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // =====================================================
    //  TOP BAR
    // =====================================================
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SIDEBAR_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                BorderFactory.createEmptyBorder(0, 22, 0, 22)));
        bar.setPreferredSize(new Dimension(0, 60));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setBackground(SIDEBAR_BG);
        JLabel logo = new JLabel("◈");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        logo.setForeground(BLUE);
        JLabel bankName = new JLabel("SUPREME MONEY COACH");
        bankName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        bankName.setForeground(TEXT_BRIGHT);
        JLabel badge = new JLabel("  ADMIN PANEL");
        badge.setFont(F_SMALL);
        badge.setForeground(BLUE);
        left.add(logo); left.add(bankName); left.add(badge);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setBackground(SIDEBAR_BG);

        JLabel avatar = new JLabel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE); g2.fillOval(0, 0, 34, 34);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String ch = Session.username != null ? Session.username.substring(0, 1).toUpperCase() : "A";
                g2.drawString(ch, (34 - fm.stringWidth(ch)) / 2,
                        (34 + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        avatar.setPreferredSize(new Dimension(34, 34));
        JLabel nameLabel = new JLabel(
                (Session.username != null ? Session.username.toUpperCase() : "ADMIN") + "  ·  Admin");
        nameLabel.setFont(F_BODY); nameLabel.setForeground(TEXT);

        JButton logoutBtn = buildBtn("Logout", RED);
        logoutBtn.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginForm());
        });
        right.add(avatar); right.add(nameLabel); right.add(logoutBtn);
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // =====================================================
    //  SIDEBAR
    // =====================================================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COL));
        sidebar.setPreferredSize(new Dimension(225, 0));
        sidebar.add(Box.createVerticalStrut(18));

        String[][] items = {
                {"[=]", "Overview",          P_OVERVIEW},
                {"[U]", "User Management",   P_USERS},
                {"[R]", "Request Queue",     P_REQUESTS},
                {"[P]", "Profile Pics",      P_PICTURES},
                {"[A]", "Analytics",         P_ANALYTICS},
                {"[!]", "Announcements",     P_ANNOUNCE},
                {"[D]", "Debts Monitor",     P_DEBTS},
                {"[$]", "Transactions",      P_TXNS},
                {"[V]", "Video Submissions", P_VIDEO}
        };

        sidebarBtns = new JButton[items.length];
        for (int i = 0; i < items.length; i++) {
            final String pName = items[i][2];
            final int    idx   = i;
            JButton btn = new JButton(items[i][0] + "   " + items[i][1]);
            btn.setFont(F_BODY); btn.setForeground(TEXT_DIM);
            btn.setBackground(SIDEBAR_BG); btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            btn.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 10));
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!pName.equals(activePanel)) btn.setBackground(CARD_BG);
                }
                public void mouseExited(MouseEvent e) {
                    if (!pName.equals(activePanel)) btn.setBackground(SIDEBAR_BG);
                }
            });
            btn.addActionListener(e -> {
                activePanel = pName;
                cardLayout.show(contentArea, pName);
                highlightSidebar(idx);
                onSwitch(pName);
            });
            sidebarBtns[i] = btn;
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(2));
        }

        sidebar.add(Box.createVerticalGlue());
        JLabel ver = new JLabel("   Admin Panel v2.0");
        ver.setFont(F_SMALL); ver.setForeground(TEXT_DIM);
        ver.setBorder(BorderFactory.createEmptyBorder(10, 10, 12, 0));
        sidebar.add(ver);
        return sidebar;
    }
    private void loadActiveUsers(JPanel panel) {
        panel.removeAll();
        try (Connection conn = SecureDatabaseConnection.connect()) {
            // Get users who have activity today - MySQL compatible version
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT DISTINCT u.id, u.username, u.is_active, " +
                            "MAX(m.dateOfPayment) as last_activity " +
                            "FROM users u " +
                            "LEFT JOIN mysaving2 m ON u.id = m.user_id AND DATE(m.dateOfPayment) = CURDATE() " +
                            "WHERE u.is_active = 1 " +
                            "GROUP BY u.id, u.username, u.is_active " +
                            "ORDER BY last_activity IS NULL, last_activity DESC " +
                            "LIMIT 15");

            ResultSet rs = pst.executeQuery();
            int count = 0;
            while (rs.next()) {
                count++;
                String username = rs.getString("username");
                boolean isActive = rs.getBoolean("is_active");
                Date lastActivity = rs.getDate("last_activity");

                JPanel userRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
                userRow.setBackground(SIDEBAR_BG);
                userRow.setMaximumSize(new Dimension(225, 25));

                // Green dot for active today, gray for inactive
                JLabel statusIcon = new JLabel(lastActivity != null ? "🟢" : "⭕");
                statusIcon.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                JLabel nameLabel = new JLabel(username);
                nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                nameLabel.setForeground(lastActivity != null ? GREEN : TEXT_DIM);
                nameLabel.setToolTipText(lastActivity != null ? "Active today" : "No activity today");

                userRow.add(statusIcon);
                userRow.add(nameLabel);
                panel.add(userRow);
            }

            if (count == 0) {
                JLabel noUsers = new JLabel("   No active users");
                noUsers.setFont(F_SMALL);
                noUsers.setForeground(TEXT_DIM);
                panel.add(noUsers);
            }

        } catch (SQLException e) {
            JLabel errorLabel = new JLabel("   Error loading users");
            errorLabel.setFont(F_SMALL);
            errorLabel.setForeground(RED);
            panel.add(errorLabel);
            e.printStackTrace();
        }
        panel.revalidate();
        panel.repaint();
    }

    private void highlightSidebar(int sel) {
        for (int i = 0; i < sidebarBtns.length; i++) {
            if (i == sel) {
                sidebarBtns[i].setForeground(BLUE);
                sidebarBtns[i].setBackground(new Color(31, 111, 235, 28));
                sidebarBtns[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, BLUE),
                        BorderFactory.createEmptyBorder(11, 19, 11, 10)));
            } else {
                sidebarBtns[i].setForeground(TEXT_DIM);
                sidebarBtns[i].setBackground(SIDEBAR_BG);
                sidebarBtns[i].setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 10));
            }
        }
    }

    private void onSwitch(String name) {
        switch (name) {
            case P_OVERVIEW:
                if (!statsLoaded) loadOverviewStats();
                break;
            case P_USERS:
                if (!usersLoaded) loadUsersTable();
                break;
            case P_REQUESTS:
                if (!requestsLoaded) loadRequestsTable();
                break;
            case P_DEBTS:
                if (!debtsLoaded) loadDebtsTable();
                break;
            case P_PICTURES:
                if (!picturesLoaded) loadPicturesGrid();
                break;
            case P_ANALYTICS:
                contentArea.repaint();
                break;
            case P_TXNS:
                if (!txnsLoaded) loadTransactionsTable();
                break;
            case P_VIDEO:
                refreshAllVideoTabs();
                break;
        }
    }

    // =====================================================
    //  CONTENT AREA
    // =====================================================
    private JPanel buildContentArea() {
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(BG);
        contentArea.add(buildOverviewPanel(),         P_OVERVIEW);
        contentArea.add(buildUsersPanel(),            P_USERS);
        contentArea.add(buildRequestsPanel(),         P_REQUESTS);
        contentArea.add(buildPicturesPanel(),         P_PICTURES);
        contentArea.add(buildAnalyticsPanel(),        P_ANALYTICS);
        contentArea.add(buildAnnouncementsPanel(),    P_ANNOUNCE);
        contentArea.add(buildDebtsPanel(),            P_DEBTS);
        contentArea.add(buildTransactionsPanel(),     P_TXNS);
        contentArea.add(buildVideoPanel(),            P_VIDEO);
        return contentArea;
    }

    // =====================================================
    //  OVERVIEW PANEL (ENHANCED WITH ALL METRICS)
    // =====================================================
    private JPanel buildOverviewPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        panel.add(pageHeader("Dashboard Overview",
                "Welcome back, " + Session.username + ". Here is what is happening today."));

        // Row 1: Core Stats (4 cards)
        lblTotalUsers    = new JLabel("--"); lblTotalUsers.setFont(F_STAT);    lblTotalUsers.setForeground(BLUE);
        lblVerifiedEmails = new JLabel("--"); lblVerifiedEmails.setFont(F_STAT); lblVerifiedEmails.setForeground(GREEN);
        lblProfilePics   = new JLabel("--"); lblProfilePics.setFont(F_STAT);   lblProfilePics.setForeground(PURPLE);
        lblActiveUsers   = new JLabel("--"); lblActiveUsers.setFont(F_STAT);   lblActiveUsers.setForeground(GREEN);

        JPanel row1 = new JPanel(new GridLayout(1, 4, 14, 0));
        row1.setBackground(BG);
        row1.setAlignmentX(0);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        row1.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        row1.add(statCard("Total Users",      lblTotalUsers,    "[U]", BLUE));
        row1.add(statCard("Verified Emails",  lblVerifiedEmails, "[✓]", GREEN));
        row1.add(statCard("Profile Pictures", lblProfilePics,   "[P]", PURPLE));
        row1.add(statCard("Active Users",     lblActiveUsers,   "[+]", GREEN));
        panel.add(row1);

        // Row 2: Engagement Stats (4 cards)
        lblAIConversations = new JLabel("--"); lblAIConversations.setFont(F_STAT); lblAIConversations.setForeground(CYAN);
        lblPdfReports      = new JLabel("--"); lblPdfReports.setFont(F_STAT);      lblPdfReports.setForeground(YELLOW);
        lblChamasCreated   = new JLabel("--"); lblChamasCreated.setFont(F_STAT);   lblChamasCreated.setForeground(PURPLE);
        lblActiveToday     = new JLabel("--"); lblActiveToday.setFont(F_STAT);     lblActiveToday.setForeground(BLUE);

        JPanel row2 = new JPanel(new GridLayout(1, 4, 14, 0));
        row2.setBackground(BG);
        row2.setAlignmentX(0);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        row2.setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 0));
        row2.add(statCard("AI Conversations", lblAIConversations, "[🤖]", CYAN));
        row2.add(statCard("PDF Reports",      lblPdfReports,      "[📄]", YELLOW));
        row2.add(statCard("Chamas Created",   lblChamasCreated,   "[👥]", PURPLE));
        row2.add(statCard("Active Today",     lblActiveToday,     "[🔥]", BLUE));
        panel.add(row2);

        // ========== ACTIVE USERS TODAY LIST ==========
        panel.add(sectionLabel("👤 Active Users Today (With Activity)"));

        String[] activeCols = {"User", "Last Activity", "Status", "Actions"};
        DefaultTableModel activeModel = new DefaultTableModel(activeCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable activeTable = buildTable(activeModel);
// ✅ Make Status column wider so text doesn't truncate
        activeTable.getColumnModel().getColumn(0).setPreferredWidth(100);  // User
        activeTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Last Activity
        activeTable.getColumnModel().getColumn(2).setPreferredWidth(350);  // Status - WIDER!
        activeTable.getColumnModel().getColumn(3).setPreferredWidth(200);  // Actions

        // Custom renderer for status column
        activeTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setToolTipText(value != null ? value.toString() : "");
                }
                return c;
            }
        });

        // Load active users in background
        executor.submit(() -> loadActiveUsersTable(activeModel));

        panel.add(darkScroll(activeTable));
        panel.add(Box.createVerticalStrut(10));
        JPanel activeActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        activeActions.setBackground(BG);
        JButton refreshActiveBtn = buildBtn("🔄 Refresh Active Users", BLUE);
        refreshActiveBtn.addActionListener(e -> {
            activeModel.setRowCount(0);
            executor.submit(() -> loadActiveUsersTable(activeModel));
        });
        activeActions.add(refreshActiveBtn);
        panel.add(activeActions);

        // Recent Transactions Table
        panel.add(sectionLabel("Recent Transactions (Last 10)"));


        String[] cols = {"ID", "User", "Date", "Amount", "Type"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildTable(model);
        colorizeCol(table, 3);

        executor.submit(() -> loadRecentTransactions(model));
        panel.add(darkScroll(table));

        JScrollPane outer = new JScrollPane(panel);
        outer.setBorder(null);
        outer.getViewport().setBackground(BG);
        outer.getVerticalScrollBar().setUnitIncrement(16);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(outer, BorderLayout.CENTER);
        return wrapper;
    }
    private void loadActiveUsersTable(DefaultTableModel model) {
        model.setRowCount(0);
        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect()) {
                PreparedStatement pst = conn.prepareStatement(
                        "SELECT u.id, u.username, " +
                                "MAX(ua.activity_time) as last_activity, " +
                                "COUNT(ua.id) as total_actions, " +
                                "GROUP_CONCAT(DISTINCT ua.activity_type ORDER BY ua.activity_time DESC SEPARATOR ', ') as actions, " +
                                "DATE_FORMAT(CONVERT_TZ(MAX(ua.activity_time), '+00:00', '+03:00'), '%H:%i') as local_time " +
                                "FROM users u " +
                                "LEFT JOIN user_activity ua ON u.id = ua.user_id " +
                                "WHERE DATE(CONVERT_TZ(ua.activity_time, '+00:00', '+03:00')) = CURDATE() " +
                                "GROUP BY u.id, u.username " +
                                "ORDER BY last_activity DESC " +
                                "LIMIT 20");

                ResultSet rs = pst.executeQuery();
                boolean hasUsers = false;
                while (rs.next()) {
                    hasUsers = true;
                    String username = rs.getString("username");
                    int actions = rs.getInt("total_actions");
                    String actionTypes = rs.getString("actions");
                    String localTime = rs.getString("local_time");
                    Timestamp lastActivity = rs.getTimestamp("last_activity");

                    // Format time
                    String timeStr = localTime != null ? localTime : formatTime(lastActivity);

                    // ✅ SHOW FULL STATUS (not truncated)
                    String status;
                    if (actionTypes != null && !actionTypes.isEmpty()) {
                        // Show ALL actions with count
                        status = "🟢 " + actions + " actions: " + actionTypes;
                    } else {
                        status = "⭕ No actions today";
                    }

                    final String finalUsername = username;
                    final String finalTime = timeStr;
                    final String finalStatus = status;
                    final String finalActions = actionTypes != null ? actionTypes : "None";

                    SwingUtilities.invokeLater(() -> {
                        model.addRow(new Object[]{finalUsername, finalTime, finalStatus, finalActions});
                    });
                }

                if (!hasUsers) {
                    SwingUtilities.invokeLater(() -> {
                        model.addRow(new Object[]{"No users active today", "—", "⭕ No activity", "—"});
                    });
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "—";

        try {
            // Convert from UTC to local timezone (EAT = UTC+3)
            java.time.Instant instant = timestamp.toInstant();
            java.time.ZonedDateTime utcTime = instant.atZone(java.time.ZoneId.of("UTC"));
            java.time.ZonedDateTime localTime = utcTime.withZoneSameInstant(java.time.ZoneId.of("Africa/Nairobi"));

            java.time.LocalDateTime time = localTime.toLocalDateTime();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.Duration duration = java.time.Duration.between(time, now);

            long seconds = duration.getSeconds();
            if (seconds < 0) return "Just now";
            if (seconds < 60) return "Just now";
            if (seconds < 3600) {
                long minutes = seconds / 60;
                return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
            }
            if (seconds < 86400) {
                long hours = seconds / 3600;
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            }
            // If more than 24 hours, show time
            return localTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return "—";
        }
    }
    // =====================================================
    //  LOAD ALL STATS (OPTIMIZED WITH CACHING)
    // =====================================================
    private void loadOverviewStats() {
        if (lblTotalUsers == null) return;
        statsLoaded = false;

        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect()) {

                Map<String, String> stats = new HashMap<>();

                // 1. Total Users
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                    if (rs.next()) stats.put("totalUsers", String.valueOf(rs.getInt(1)));
                }

                // 2. Verified Emails
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE email_verified = 1 AND email IS NOT NULL AND email != ''")) {
                    if (rs.next()) stats.put("verifiedEmails", String.valueOf(rs.getInt(1)));
                }

                // 3. Profile Pictures
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE profile_pic IS NOT NULL")) {
                    if (rs.next()) stats.put("profilePics", String.valueOf(rs.getInt(1)));
                }

                // 4. Active Users (is_active = 1)
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE is_active = 1")) {
                    if (rs.next()) stats.put("activeUsers", String.valueOf(rs.getInt(1)));
                }

                // 5. AI Conversations - Count actual AI chats
                int aiCount = 0;
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM chat_history")) {
                    if (rs.next()) aiCount = rs.getInt(1);
                } catch (SQLException e) {
                    // Try alternative table
                    try (Statement st2 = conn.createStatement();
                         ResultSet rs2 = st2.executeQuery("SELECT COUNT(*) FROM mysaving2 WHERE day = 'AI_CHAT' OR type = 'AI_CONVERSATION'")) {
                        if (rs2.next()) aiCount = rs2.getInt(1);
                    } catch (SQLException e2) {
                        aiCount = 0;
                    }
                }
                stats.put("aiConversations", String.valueOf(aiCount));

                // 6. PDF Reports - Count actual PDF exports
                int pdfCount = 0;
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM pdf_reports")) {
                    if (rs.next()) pdfCount = rs.getInt(1);
                } catch (SQLException e) {
                    // If table doesn't exist, create it
                    createPDFReportsTable();
                    pdfCount = 0;
                }
                stats.put("pdfReports", String.valueOf(pdfCount));

                // 7. Chamas Created
                int chamaCount = 0;
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM chama_groups")) {
                    if (rs.next()) chamaCount = rs.getInt(1);
                } catch (SQLException e) {
                    chamaCount = 0;
                }
                stats.put("chamasCreated", String.valueOf(chamaCount));

                // 8. Active Users Today - Count users with ANY activity today
                int activeTodayCount = 0;
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT user_id) FROM user_activity WHERE DATE(activity_time) = CURDATE()")) {
                    if (rs.next()) activeTodayCount = rs.getInt(1);
                } catch (SQLException e) {
                    // Fallback to mysaving2
                    try (Statement st2 = conn.createStatement();
                         ResultSet rs2 = st2.executeQuery("SELECT COUNT(DISTINCT user_id) FROM mysaving2 WHERE DATE(dateOfPayment) = CURDATE()")) {
                        if (rs2.next()) activeTodayCount = rs2.getInt(1);
                    } catch (SQLException e2) {
                        activeTodayCount = 0;
                    }
                }
                stats.put("activeToday", String.valueOf(activeTodayCount));

                // Store in cache and update UI
                SwingUtilities.invokeLater(() -> {
                    updateStatsUI(stats);
                    statsLoaded = true;
                });

            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    lblTotalUsers.setText("Error");
                    lblVerifiedEmails.setText("Error");
                    lblProfilePics.setText("Error");
                    lblActiveUsers.setText("Error");
                    lblAIConversations.setText("Error");
                    lblPdfReports.setText("Error");
                    lblChamasCreated.setText("Error");
                    lblActiveToday.setText("Error");
                });
            }
        });
    }
    private void createPDFReportsTable() {
        try (Connection conn = SecureDatabaseConnection.connect();
             Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS pdf_reports (" +
                            "id INT PRIMARY KEY AUTO_INCREMENT, " +
                            "user_id INT NOT NULL, " +
                            "report_type VARCHAR(50), " +
                            "filename VARCHAR(255), " +
                            "created_at DATETIME DEFAULT NOW(), " +
                            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                            ")"
            );
            System.out.println("✅ pdf_reports table created");
        } catch (SQLException e) {
            System.err.println("❌ Failed to create pdf_reports table: " + e.getMessage());
        }
    }
    private void updateStatsUI(Map<String, String> stats) {
        lblTotalUsers.setText(stats.getOrDefault("totalUsers", "0"));
        lblVerifiedEmails.setText(stats.getOrDefault("verifiedEmails", "0"));
        lblProfilePics.setText(stats.getOrDefault("profilePics", "0"));
        lblActiveUsers.setText(stats.getOrDefault("activeUsers", "0"));
        lblAIConversations.setText(stats.getOrDefault("aiConversations", "0"));
        lblPdfReports.setText(stats.getOrDefault("pdfReports", "0"));  // ✅ This should now show real count
        lblChamasCreated.setText(stats.getOrDefault("chamasCreated", "0"));
        lblActiveToday.setText(stats.getOrDefault("activeToday", "0"));
    }

    private void loadRecentTransactions(DefaultTableModel model) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT m.id, u.username, m.dateOfPayment, m.amount " +
                             "FROM mysaving2 m JOIN users u ON m.user_id=u.id " +
                             "ORDER BY m.id DESC LIMIT 10")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                int amt = rs.getInt("amount");
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getDate("dateOfPayment"),
                        (amt < 0 ? "- " : "+ ") + "Ksh " + Math.abs(amt),
                        amt < 0 ? "WITHDRAWAL" : "DEPOSIT"
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // =====================================================
    //  USERS PANEL (KEPT SIMILAR BUT OPTIMIZED)
    // =====================================================
    private JPanel buildUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(BG);
        top.add(pageHeader("User Management", "View, activate, deactivate or delete user accounts."), BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setBackground(BG);
        JButton activateBtn   = buildBtn("Activate", GREEN);
        JButton deactivateBtn = buildBtn("Deactivate", RED);
        JButton deleteBtn     = buildBtn("🗑 Delete", RED);  // ✅ ADD DELETE BUTTON
        JButton viewTxnsBtn   = buildBtn("View Transactions", YELLOW);
        JButton refreshBtn    = buildBtn("Refresh", BLUE);

        activateBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) { info("Select a user first."); return; }
            int userId = (int) usersModel.getValueAt(row, 0);
            executor.submit(() -> {
                toggleActive(userId, true);
                SwingUtilities.invokeLater(this::loadUsersTable);
            });
        });

        deactivateBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) { info("Select a user first."); return; }
            int userId = (int) usersModel.getValueAt(row, 0);
            executor.submit(() -> {
                toggleActive(userId, false);
                SwingUtilities.invokeLater(this::loadUsersTable);
            });
        });

        // ✅ DELETE USER ACTION
        deleteBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) { info("Select a user first."); return; }
            int userId = (int) usersModel.getValueAt(row, 0);
            String username = (String) usersModel.getValueAt(row, 1);

            // Confirm deletion
            int confirm = JOptionPane.showConfirmDialog(this,
                    "⚠️ PERMANENT DELETION ⚠️\n\n" +
                            "Delete user '" + username + "' (ID: " + userId + ")?\n\n" +
                            "This will permanently remove:\n" +
                            "• User account\n" +
                            "• All transactions\n" +
                            "• All debts\n" +
                            "• All Chama records\n" +
                            "• All notifications\n" +
                            "• All activity logs\n\n" +
                            "This action CANNOT be undone!\n\n" +
                            "Are you absolutely sure?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                executor.submit(() -> {
                    boolean deleted = deleteUserCompletely(userId);
                    SwingUtilities.invokeLater(() -> {
                        if (deleted) {
                            info("✅ User '" + username + "' deleted successfully!");
                            loadUsersTable();
                        } else {
                            info("❌ Failed to delete user. Please check console for errors.");
                        }
                    });
                });
            }
        });

        viewTxnsBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) { info("Select a user first."); return; }
            String uname = (String) usersModel.getValueAt(row, 1);
            showUserTransactions(uname);
        });

        refreshBtn.addActionListener(e -> loadUsersTable());

        actions.add(activateBtn);
        actions.add(deactivateBtn);
        actions.add(deleteBtn);  // ✅ ADD DELETE BUTTON
        actions.add(viewTxnsBtn);
        actions.add(refreshBtn);
        top.add(actions, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        // ✅ ADD FULL NAME COLUMN
        String[] cols = {"ID", "Username", "Full Name", "Email", "Phone", "Verified", "Status", "Occupation", "Income", "Goal"};
        usersModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = buildTable(usersModel);

        // ✅ Set column widths
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        usersTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        usersTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        usersTable.getColumnModel().getColumn(7).setPreferredWidth(120);
        usersTable.getColumnModel().getColumn(8).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(9).setPreferredWidth(100);

        usersTable.getColumnModel().getColumn(5).setCellRenderer(statusRenderer());
        panel.add(darkScroll(usersTable), BorderLayout.CENTER);
        return panel;
    }
    private boolean deleteUserCompletely(int userId) {
        // List all tables that reference users
        String[] tables = {
                "savings_streak",
                "mysaving2",
                "chama_members",
                "chama_contributions",
                "debts",
                "user_activity",
                "notifications",
                "credit_scores",
                "admin_requests",
                "video_submissions",
                "security_questions",
                "email_verifications",
                "password_resets"
        };

        try (Connection conn = SecureDatabaseConnection.connect()) {
            conn.setAutoCommit(false);

            try {
                // Delete from all related tables
                for (String table : tables) {
                    try (PreparedStatement pst = conn.prepareStatement("DELETE FROM " + table + " WHERE user_id = ?")) {
                        pst.setInt(1, userId);
                        pst.executeUpdate();
                    } catch (SQLException e) {
                        // Table might not exist, continue
                        System.out.println("⚠️ Table " + table + " not found or no data, continuing...");
                    }
                }

                // Finally delete the user
                try (PreparedStatement pst = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    pst.setInt(1, userId);
                    pst.executeUpdate();
                }

                conn.commit();
                System.out.println("✅ User " + userId + " deleted successfully!");
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("❌ Error deleting user: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Database connection error: " + e.getMessage());
            return false;
        }
    }

    private void loadUsersTable() {
        if (usersModel == null) return;
        usersLoaded = false;

        executor.submit(() -> {
            try {
                usersModel.setRowCount(0);
                Connection conn = SecureDatabaseConnection.connect();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT id, username, " +
                                "IFNULL(fullname, '—') as fullname, " +   // ✅ ADD FULL NAME
                                "IFNULL(email, '—') as email, " +
                                "IFNULL(phone_number, '—') as phone, " +
                                "IFNULL(email_verified, 0) as verified, " +
                                "IFNULL(is_active, 1) as active, " +
                                "IFNULL(occupation, '—') as occupation, " +
                                "IFNULL(monthly_income, 0) as income, " +
                                "IFNULL(goal_type, '—') as goal_type " +
                                "FROM users ORDER BY id ASC LIMIT 100");

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String username = rs.getString("username");
                    String fullname = rs.getString("fullname");   // ✅ GET FULL NAME
                    String email = rs.getString("email");
                    String phone = rs.getString("phone");
                    int verifiedInt = rs.getInt("verified");
                    int activeInt = rs.getInt("active");
                    String occupation = rs.getString("occupation");
                    int income = rs.getInt("income");
                    String goalType = rs.getString("goal_type");

                    boolean verified = verifiedInt == 1;
                    boolean active = activeInt == 1;

                    SwingUtilities.invokeLater(() -> {
                        usersModel.addRow(new Object[]{
                                id,
                                username,
                                fullname,    // ✅ ADD FULL NAME
                                email,
                                phone,
                                verified ? "Yes" : "No",
                                active ? "Active" : "Inactive",
                                occupation,
                                "Ksh " + income,
                                goalType
                        });
                    });
                }
                rs.close();
                st.close();
                conn.close();
                usersLoaded = true;

            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null,
                            "Error loading users: " + ex.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void toggleActive(int userId, boolean active) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("UPDATE users SET is_active=? WHERE id=?")) {
            pst.setBoolean(1, active); pst.setInt(2, userId);
            pst.executeUpdate();
            try {
                NotificationService.create(userId,
                        active ? "Your account has been activated by Admin."
                                : "Your account has been deactivated. Contact admin.",
                        active ? NotificationService.SUCCESS : NotificationService.ALERT);
            } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> info("User " + (active ? "activated" : "deactivated") + "."));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void showUserTransactions(String username) {
        JDialog dlg = new JDialog(this, "Transactions — " + username, true);
        dlg.setSize(700, 450); dlg.setLocationRelativeTo(this);
        String[] cols = {"ID", "Date", "Amount", "Type"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT m.id, m.dateOfPayment, m.amount FROM mysaving2 m " +
                                 "JOIN users u ON m.user_id=u.id WHERE u.username=? ORDER BY m.id DESC LIMIT 50")) {
                pst.setString(1, username);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    int amt = rs.getInt("amount");
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getDate("dateOfPayment"),
                            (amt < 0 ? "- " : "+ ") + "Ksh " + Math.abs(amt),
                            amt < 0 ? "WITHDRAWAL" : "DEPOSIT"
                    });
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> {
                JTable t = buildTable(model);
                colorizeCol(t, 2);
                JPanel p = new JPanel(new BorderLayout());
                p.setBackground(BG);
                p.add(darkScroll(t), BorderLayout.CENTER);
                dlg.setContentPane(p);
                dlg.setVisible(true);
            });
        });
    }

    // =====================================================
    //  REQUESTS PANEL (OPTIMIZED)
    // =====================================================
    private JPanel buildRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(BG);
        top.add(pageHeader("Request Queue", "Review and respond to user-submitted requests."), BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setBackground(BG);
        JButton approveBtn = buildBtn("Approve", GREEN);
        JButton rejectBtn  = buildBtn("Reject",  RED);
        JButton refreshBtn = buildBtn("Refresh", BLUE);
        JButton viewFullBtn = buildBtn("📄 View Full Description", YELLOW);

        approveBtn.addActionListener(e -> handleRequest(true));
        rejectBtn.addActionListener(e  -> handleRequest(false));
        refreshBtn.addActionListener(e -> loadRequestsTable());

        viewFullBtn.addActionListener(e -> {
            int row = requestsTable.getSelectedRow();
            if (row < 0) { info("Select a request first."); return; }
            String fullDesc = (String) requestsModel.getValueAt(row, 3);
            String user = (String) requestsModel.getValueAt(row, 1);
            String type = (String) requestsModel.getValueAt(row, 2);
            String status = (String) requestsModel.getValueAt(row, 4);

            JTextArea ta = new JTextArea(fullDesc);
            ta.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            ta.setBackground(CARD_BG);
            ta.setForeground(TEXT);
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setMargin(new Insets(15, 15, 15, 15));

            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(550, 350));

            JPanel infoPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            infoPanel.setBackground(CARD_BG);
            infoPanel.add(new JLabel("User:"));
            infoPanel.add(new JLabel(user));
            infoPanel.add(new JLabel("Type:"));
            infoPanel.add(new JLabel(type));
            infoPanel.add(new JLabel("Status:"));
            infoPanel.add(new JLabel(status));

            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBackground(CARD_BG);
            mainPanel.add(infoPanel, BorderLayout.NORTH);
            mainPanel.add(sp, BorderLayout.CENTER);

            JOptionPane.showMessageDialog(panel, mainPanel,
                    "📄 Full Request Details",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        actions.add(approveBtn);
        actions.add(rejectBtn);
        actions.add(viewFullBtn);
        actions.add(refreshBtn);
        top.add(actions, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        // Wider columns for better visibility
        String[] cols = {"ID", "User", "Type", "Description", "Status", "Submitted"};
        requestsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        requestsTable = buildTable(requestsModel);

        // Set column widths - Description gets more space
        requestsTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // ID
        requestsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // User
        requestsTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Type
        requestsTable.getColumnModel().getColumn(3).setPreferredWidth(300); // Description - WIDER!
        requestsTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Status
        requestsTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Submitted

        requestsTable.getColumnModel().getColumn(4).setCellRenderer(requestStatusRenderer());

        // Custom renderer for description to show full text with tooltip and truncation
        requestsTable.getColumnModel().getColumn(3).setCellRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                String text = value != null ? value.toString() : "";
                // Truncate to 50 characters for display, but keep full in tooltip
                String displayText = text.length() > 50 ? text.substring(0, 47) + "..." : text;

                JTextArea textArea = new JTextArea(displayText);
                textArea.setFont(F_BODY);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setBackground(isSelected ? new Color(31,111,235,70) : (row % 2 == 0 ? CARD_BG : ROW_ALT));
                textArea.setForeground(TEXT);
                textArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                // Set tooltip with full text
                textArea.setToolTipText(text);
                return textArea;
            }
        });

        panel.add(darkScroll(requestsTable), BorderLayout.CENTER);
        return panel;
    }

    private void loadRequestsTable() {
        if (requestsModel == null) return;
        requestsLoaded = false;

        executor.submit(() -> {
            requestsModel.setRowCount(0);
            try (Connection conn = SecureDatabaseConnection.connect()) {

                // Check if admin_note column exists
                boolean hasAdminNote = false;
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "admin_requests", "admin_note")) {
                    hasAdminNote = rs.next();
                } catch (SQLException ignored) {}

                // Build query based on available columns
                String sql;
                if (hasAdminNote) {
                    sql = "SELECT r.id, u.username, r.request_type, r.description, r.status, " +
                            "DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i') as created " +
                            "FROM admin_requests r JOIN users u ON r.user_id = u.id " +
                            "ORDER BY r.id DESC LIMIT 50";
                } else {
                    sql = "SELECT r.id, u.username, r.request_type, r.description, r.status, " +
                            "DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i') as created " +
                            "FROM admin_requests r JOIN users u ON r.user_id = u.id " +
                            "ORDER BY r.id DESC LIMIT 50";
                }

                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        final int id = rs.getInt("id");
                        final String username = rs.getString("username");
                        final String type = rs.getString("request_type");
                        final String desc = rs.getString("description");
                        final String status = rs.getString("status");
                        final String created = rs.getString("created");

                        SwingUtilities.invokeLater(() -> {
                            requestsModel.addRow(new Object[]{id, username, type, desc, status, created});
                        });
                    }
                    requestsLoaded = true;
                }
            } catch (SQLException ex) {
                System.out.println("Requests: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private void handleRequest(boolean approve) {
        if (requestsTable == null || requestsTable.getSelectedRow() < 0) {
            info("Select a request first.");
            return;
        }
        int row = requestsTable.getSelectedRow();
        int id = (int) requestsModel.getValueAt(row, 0);
        String user = (String) requestsModel.getValueAt(row, 1);
        String note = JOptionPane.showInputDialog(this, "Enter your response note for " + user + ":");
        if (note == null) return;
        String status = approve ? "APPROVED" : "REJECTED";

        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "UPDATE admin_requests SET status=?,admin_note=?,responded_at=NOW() WHERE id=?")) {
                pst.setString(1, status); pst.setString(2, note); pst.setInt(3, id);
                pst.executeUpdate();
                try {
                    int uid = getUserId(user);
                    if (uid > 0) NotificationService.create(uid,
                            "Your request #" + id + " was " + status + ". Note: " + note,
                            approve ? NotificationService.SUCCESS : NotificationService.WARNING);
                } catch (Exception ignored) {}
                SwingUtilities.invokeLater(() -> {
                    info("Request " + status.toLowerCase() + ".");
                    loadRequestsTable();
                });
            } catch (SQLException ex) {
                info("DB error: " + ex.getMessage());
            }
        });
    }

    // =====================================================
    //  PICTURES PANEL (OPTIMIZED)
    // =====================================================
    private JPanel buildPicturesPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 14));
        outer.setBackground(BG);
        outer.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(BG);
        headerRow.add(pageHeader("Profile Pictures", "View and manage all user profile avatars."), BorderLayout.CENTER);
        JButton refreshBtn = buildBtn("Refresh", BLUE);
        refreshBtn.addActionListener(e -> loadPicturesGrid());
        headerRow.add(refreshBtn, BorderLayout.EAST);
        outer.add(headerRow, BorderLayout.NORTH);

        picturesGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        picturesGrid.setBackground(BG);
        JScrollPane scroll = new JScrollPane(picturesGrid);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private void loadPicturesGrid() {
        if (picturesGrid == null) return;
        picturesLoaded = false;
        picturesGrid.removeAll();

        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id,username,profile_pic FROM users ORDER BY id ASC LIMIT 100")) {
                while (rs.next()) {
                    final int userId = rs.getInt("id");
                    final String username = rs.getString("username");
                    final byte[] picBytes = rs.getBytes("profile_pic");

                    SwingUtilities.invokeLater(() -> {
                        picturesGrid.add(buildPicCard(userId, username, picBytes));
                    });
                }
                picturesLoaded = true;
            } catch (SQLException ex) {
                System.out.println("Pics: " + ex.getMessage());
            }
            SwingUtilities.invokeLater(() -> {
                picturesGrid.revalidate();
                picturesGrid.repaint();
            });
        });
    }

    private JPanel buildPicCard(int userId, String username, byte[] picBytes) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        card.setPreferredSize(new Dimension(145, 185));

        JLabel imgLbl = new JLabel();
        imgLbl.setHorizontalAlignment(SwingConstants.CENTER);
        imgLbl.setPreferredSize(new Dimension(90, 90));
        if (picBytes != null && picBytes.length > 0) {
            try {
                BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(picBytes));
                if (img != null) imgLbl.setIcon(new ImageIcon(circularCrop(img, 90)));
            } catch (IOException ignored) {}
        }
        if (imgLbl.getIcon() == null) imgLbl.setIcon(defaultAvatar(username, 90));

        JLabel nameLbl = new JLabel(username, SwingConstants.CENTER);
        nameLbl.setFont(F_BODY); nameLbl.setForeground(TEXT_BRIGHT);
        JLabel idLbl = new JLabel("ID: " + userId, SwingConstants.CENTER);
        idLbl.setFont(F_SMALL); idLbl.setForeground(TEXT_DIM);
        JButton delBtn = buildBtn("Remove", RED); delBtn.setFont(F_SMALL);
        delBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Remove pic of " + username + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                removePic(userId); loadPicturesGrid();
            }
        });
        JPanel info = new JPanel(new GridLayout(3, 1, 0, 3));
        info.setBackground(CARD_BG);
        info.add(nameLbl); info.add(idLbl); info.add(delBtn);
        card.add(imgLbl, BorderLayout.CENTER);
        card.add(info,   BorderLayout.SOUTH);
        return card;
    }

    private void removePic(int userId) {
        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "UPDATE users SET profile_pic=NULL,pic_type=NULL WHERE id=?")) {
                pst.setInt(1, userId);
                pst.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    // =====================================================
    //  ANALYTICS PANEL
    // =====================================================
    private JPanel buildAnalyticsPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 18));
        outer.setBackground(BG);
        outer.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        outer.add(pageHeader("System Analytics", "Visual breakdown of savings, users and performance."),
                BorderLayout.NORTH);

        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 18, 0));
        chartsRow.setBackground(BG);
        chartsRow.setPreferredSize(new Dimension(0, 300));
        chartsRow.add(buildBarChart());
        chartsRow.add(buildPieChart());

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setBackground(BG);
        bottom.add(sectionLabel("Per-User Savings Summary"), BorderLayout.NORTH);

        String[] cols = {"User", "Deposited", "Withdrawn", "Net Balance", "Progress", "Transactions"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT u.username," +
                                 "COALESCE(SUM(CASE WHEN m.amount>0 THEN m.amount ELSE 0 END),0) d," +
                                 "COALESCE(SUM(CASE WHEN m.amount<0 THEN ABS(m.amount) ELSE 0 END),0) w," +
                                 "COALESCE(SUM(m.amount),0) net,COUNT(m.id) txns " +
                                 "FROM users u LEFT JOIN mysaving2 m ON u.id=m.user_id " +
                                 "GROUP BY u.id,u.username ORDER BY net DESC LIMIT 50")) {
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    final String username = rs.getString("username");
                    final int deposited = rs.getInt("d");
                    final int withdrawn = rs.getInt("w");
                    final int net = rs.getInt("net");
                    final int txns = rs.getInt("txns");

                    SwingUtilities.invokeLater(() -> {
                        model.addRow(new Object[]{username,
                                "Ksh " + deposited, "Ksh " + withdrawn,
                                "Ksh " + net, String.format("%.1f%%", Math.min(net/15000.0*100,100)),
                                txns});
                    });
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        bottom.add(darkScroll(buildTable(model)), BorderLayout.CENTER);
        outer.add(chartsRow, BorderLayout.CENTER);
        outer.add(bottom,    BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildBarChart() {
        return new JPanel() {
            String[] labels = {};
            int[] values = {};
            {
                setBackground(CARD_BG);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COL),
                        BorderFactory.createEmptyBorder(12, 12, 12, 12)));

                executor.submit(() -> {
                    java.util.List<String> lb = new java.util.ArrayList<>();
                    java.util.List<Integer> vs = new java.util.ArrayList<>();
                    try (Connection c = SecureDatabaseConnection.connect();
                         PreparedStatement p = c.prepareStatement(
                                 "SELECT u.username,COALESCE(SUM(m.amount),0) t FROM users u " +
                                         "LEFT JOIN mysaving2 m ON u.id=m.user_id GROUP BY u.id ORDER BY t DESC LIMIT 8")) {
                        ResultSet rs = p.executeQuery();
                        while (rs.next()) {
                            lb.add(rs.getString(1));
                            vs.add(rs.getInt(2));
                        }
                    } catch (SQLException ignored) {}
                    labels = lb.toArray(new String[0]);
                    values = vs.stream().mapToInt(i->i).toArray();
                    repaint();
                });
            }
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(), h=getHeight(), pL=55, pB=50, pT=38, pR=12;
                int cW=w-pL-pR, cH=h-pT-pB;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13)); g2.setColor(TEXT_BRIGHT);
                g2.drawString("Net Savings Per User (Ksh)", pL, 24);
                if (values.length == 0) {
                    g2.setColor(TEXT_DIM);
                    g2.drawString("Loading chart data...", pL + 50, pT + cH / 2);
                    return;
                }
                int max = 1;
                for (int v : values) if (v > max) max = v;
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                for (int i = 0; i <= 5; i++) {
                    int y = pT + cH - cH*i/5;
                    g2.setColor(BORDER_COL); g2.drawLine(pL, y, pL+cW, y);
                    g2.setColor(TEXT_DIM); g2.drawString(String.valueOf((max/5)*i), 2, y+4);
                }
                Color[] cols = {BLUE, GREEN, YELLOW, PURPLE, CYAN, RED,
                        new Color(255,140,0), new Color(0,180,140)};
                int barW = Math.max(10, (cW-(values.length+1)*6)/values.length);
                int gap  = (cW-values.length*barW)/(values.length+1);
                for (int i = 0; i < values.length; i++) {
                    int bH = Math.max(0, (int)((double)values[i]/max*cH));
                    int x  = pL+gap+i*(barW+gap), y = pT+cH-bH;
                    g2.setColor(cols[i%cols.length]);
                    g2.fillRoundRect(x, y, barW, bH, 4, 4);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 9)); g2.setColor(TEXT_DIM);
                    String lbl = labels.length>i ? (labels[i].length()>6 ? labels[i].substring(0,5)+"." : labels[i]) : "";
                    g2.drawString(lbl, x+barW/2-g2.getFontMetrics().stringWidth(lbl)/2, pT+cH+14);
                }
                g2.setColor(TEXT_DIM); g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.drawLine(pL, pT, pL, pT+cH); g2.drawLine(pL, pT+cH, pL+cW, pT+cH);
            }
        };
    }

    private JPanel buildPieChart() {
        return new JPanel() {
            int active = 0, inactive = 0;
            boolean loaded = false;
            {
                setBackground(CARD_BG);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COL),
                        BorderFactory.createEmptyBorder(12, 12, 12, 12)));

                executor.submit(() -> {
                    try (Connection c = SecureDatabaseConnection.connect(); Statement st = c.createStatement();
                         ResultSet rs = st.executeQuery("SELECT is_active FROM users")) {
                        while (rs.next()) {
                            if (rs.getBoolean(1)) active++;
                            else inactive++;
                        }
                        loaded = true;
                        repaint();
                    } catch (SQLException ignored) {}
                });
            }
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(), h=getHeight();
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13)); g2.setColor(TEXT_BRIGHT);
                g2.drawString("User Status Distribution", 15, 26);

                if (!loaded) {
                    g2.setColor(TEXT_DIM);
                    g2.drawString("Loading...", w/2 - 30, h/2);
                    return;
                }

                int total = active + inactive;
                if (total == 0) return;
                int cx=w/2, cy=h/2+8, r=Math.min(w,h)/2-55;
                int[] vals = {active, inactive};
                Color[] cols = {GREEN, RED};
                String[] lbs = {"Active", "Inactive"};
                double start = -90;
                for (int i = 0; i < vals.length; i++) {
                    double angle = 360.0*vals[i]/total;
                    g2.setColor(cols[i]); g2.fillArc(cx-r, cy-r, r*2, r*2, (int)start, (int)angle);
                    start += angle;
                }
                int hr = r/2; g2.setColor(CARD_BG); g2.fillOval(cx-hr, cy-hr, hr*2, hr*2);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22)); g2.setColor(TEXT_BRIGHT);
                FontMetrics fm = g2.getFontMetrics(); String tot = String.valueOf(total);
                g2.drawString(tot, cx-fm.stringWidth(tot)/2, cy+8);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10)); g2.setColor(TEXT_DIM);
                g2.drawString("users", cx-17, cy+22);
                int lx = 12, ly = h-60;
                for (int i = 0; i < lbs.length; i++) {
                    g2.setColor(cols[i]); g2.fillRoundRect(lx, ly, 12, 12, 3, 3);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 11)); g2.setColor(TEXT);
                    g2.drawString(lbs[i]+" ("+vals[i]+")", lx+16, ly+11); lx += 120;
                }
            }
        };
    }

    // =====================================================
    //  ANNOUNCEMENTS PANEL
    // =====================================================
    private JPanel buildAnnouncementsPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 18));
        outer.setBackground(BG);
        outer.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        outer.add(pageHeader("Announcements", "Send important messages to all users."), BorderLayout.NORTH);

        JPanel compose = new JPanel(new GridBagLayout());
        compose.setBackground(CARD_BG);
        compose.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7,7,7,7); gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel compTitle = new JLabel("Compose New Announcement");
        compTitle.setFont(F_HEAD); compTitle.setForeground(TEXT_BRIGHT);
        JTextField titleField = darkField("Announcement title...");
        JTextArea msgArea = new JTextArea(4, 40);
        msgArea.setBackground(new Color(22,27,34)); msgArea.setForeground(TEXT);
        msgArea.setCaretColor(TEXT); msgArea.setFont(F_BODY);
        msgArea.setLineWrap(true); msgArea.setWrapStyleWord(true);
        msgArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                BorderFactory.createEmptyBorder(8,10,8,10)));
        JComboBox<String> priBox = new JComboBox<>(new String[]{"LOW","MEDIUM","HIGH"});
        priBox.setSelectedIndex(1); priBox.setBackground(new Color(22,27,34));
        priBox.setForeground(TEXT); priBox.setFont(F_BODY);
        JButton sendBtn = buildBtn("Send to All Users", BLUE);

        DefaultListModel<String> histModel = new DefaultListModel<>();
        sendBtn.addActionListener(e -> {
            String t = titleField.getText().trim(), msg = msgArea.getText().trim();
            if (t.isEmpty() || msg.isEmpty()) { info("Title and message are required."); return; }
            sendAnnouncement(t, msg, (String) priBox.getSelectedItem());
            titleField.setText(""); msgArea.setText("");
            executor.submit(() -> refreshAnnouncements(histModel));
        });

        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; compose.add(compTitle, gbc);
        gbc.gridy=1; gbc.gridwidth=1; compose.add(fieldLbl("Title:"), gbc);
        gbc.gridx=1; compose.add(titleField, gbc);
        gbc.gridx=0; gbc.gridy=2; compose.add(fieldLbl("Message:"), gbc);
        gbc.gridx=1; compose.add(new JScrollPane(msgArea), gbc);
        gbc.gridx=0; gbc.gridy=3; compose.add(fieldLbl("Priority:"), gbc);
        gbc.gridx=1; compose.add(priBox, gbc);
        gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; compose.add(sendBtn, gbc);

        refreshAnnouncements(histModel);
        JList<String> histList = new JList<>(histModel);
        histList.setBackground(CARD_BG); histList.setForeground(TEXT);
        histList.setFont(F_MONO); histList.setFixedCellHeight(26);
        JScrollPane histScroll = new JScrollPane(histList);
        histScroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        histScroll.getViewport().setBackground(CARD_BG);
        histScroll.setPreferredSize(new Dimension(0, 180));

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setBackground(BG);
        bottom.add(sectionLabel("Past Announcements"), BorderLayout.NORTH);
        bottom.add(histScroll, BorderLayout.CENTER);

        outer.add(compose, BorderLayout.CENTER);
        outer.add(bottom,  BorderLayout.SOUTH);
        return outer;
    }

    private void sendAnnouncement(String title, String msg, String priority) {
        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect()) {
                try (PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO announcements(admin_id, title, message, priority) VALUES(?, ?, ?, ?)")) {
                    pst.setInt(1, Session.userId); pst.setString(2, title);
                    pst.setString(3, msg);         pst.setString(4, priority);
                    pst.executeUpdate();
                }

                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id FROM users WHERE is_admin = FALSE OR is_admin IS NULL")) {
                    while (rs.next()) {
                        try {
                            NotificationService.create(rs.getInt("id"),
                                    "[" + priority + "] " + title + ": " + msg,
                                    priority.equals("HIGH") ? NotificationService.ALERT : NotificationService.INFO);
                        } catch (Exception ignored) {}
                    }
                }
                SwingUtilities.invokeLater(() -> info("✓ Announcement sent to all users via notifications!"));
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> info("Error: " + ex.getMessage()));
            }
        });
    }

    private void refreshAnnouncements(DefaultListModel<String> model) {
        model.clear();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT a.priority,a.title,a.created_at,u.username " +
                             "FROM announcements a JOIN users u ON a.admin_id=u.id " +
                             "ORDER BY a.id DESC LIMIT 30")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addElement("["+rs.getString("priority")+"]  "+
                        rs.getString("title")+"  —  "+rs.getString("username")+
                        "  "+rs.getString("created_at"));
            }
        } catch (SQLException ex) {
            System.out.println("Annc: " + ex.getMessage());
        }
    }

    // =====================================================
    //  DEBTS PANEL (OPTIMIZED)
    // =====================================================
    private JPanel buildDebtsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(BG);
        top.add(pageHeader("Debts Monitor",
                "View all user debts and send SMS reminders for due dates."), BorderLayout.NORTH);

        JLabel lblTotalDebts = new JLabel("--"); JLabel lblTotalOwed = new JLabel("--");
        JLabel lblOverdue    = new JLabel("--");
        lblTotalDebts.setFont(F_STAT); lblTotalDebts.setForeground(BLUE);
        lblTotalOwed.setFont(F_STAT);  lblTotalOwed.setForeground(RED);
        lblOverdue.setFont(F_STAT);    lblOverdue.setForeground(YELLOW);

        JPanel summary = new JPanel(new GridLayout(1, 3, 14, 0));
        summary.setBackground(BG);
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        summary.add(statCard("Active Debts",  lblTotalDebts, "[D]", BLUE));
        summary.add(statCard("Total Owed",    lblTotalOwed,  "[$]", RED));
        summary.add(statCard("Overdue",       lblOverdue,    "[!]", YELLOW));
        summary.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setBackground(BG);
        JButton reminderBtn = buildBtn("Send Reminder", YELLOW);
        JButton remindAllBtn = buildBtn("Remind All Overdue", RED);
        JButton refBtn    = buildBtn("Refresh",         BLUE);

        reminderBtn.addActionListener(e -> sendDebtSMSReminder(false));
        remindAllBtn.addActionListener(e -> sendDebtSMSReminder(true));
        refBtn.addActionListener(e -> {
            loadDebtsTable();
            loadDebtSummary(lblTotalDebts,lblTotalOwed,lblOverdue);
        });

        actions.add(reminderBtn); actions.add(remindAllBtn); actions.add(refBtn);

        top.add(summary,  BorderLayout.CENTER);
        top.add(actions,  BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"ID","User","Phone","Person","Amount (Ksh)","Type",
                "Due Date","Status","Days Overdue"};
        debtsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        debtsTable = buildTable(debtsModel);
        debtsTable.getColumnModel().getColumn(7).setCellRenderer(debtStatusRenderer());
        debtsTable.getColumnModel().getColumn(8).setCellRenderer(overdueRenderer());
        panel.add(darkScroll(debtsTable), BorderLayout.CENTER);
        loadDebtSummary(lblTotalDebts, lblTotalOwed, lblOverdue);
        return panel;
    }

    private void loadDebtsTable() {
        if (debtsModel == null) return;
        debtsLoaded = false;

        executor.submit(() -> {
            debtsModel.setRowCount(0);
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT d.id,u.username,COALESCE(u.phone_number,'N/A'),d.person_name," +
                                 "d.amount,d.type,d.due_date,d.status," +
                                 "DATEDIFF(CURDATE(),d.due_date) as od " +
                                 "FROM debts d JOIN users u ON d.user_id=u.id " +
                                 "WHERE d.status!='PAID' ORDER BY d.due_date ASC LIMIT 100")) {
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    final int id = rs.getInt("id");
                    final String username = rs.getString("username");
                    final String phone = rs.getString(3);
                    final String person = rs.getString("person_name");
                    final double amount = rs.getDouble("amount");
                    final String type = rs.getString("type");
                    final String due = rs.getString("due_date");
                    final String status = rs.getString("status");
                    final int od = rs.getInt("od");

                    SwingUtilities.invokeLater(() -> {
                        debtsModel.addRow(new Object[]{id, username, phone, person,
                                String.format("Ksh %,.0f", amount),
                                type.equals("I_OWE") ? "OWES" : "OWED TO",
                                due != null ? due : "No date",
                                status,
                                due != null && od > 0 ? od + " days" : "—"});
                    });
                }
                debtsLoaded = true;
            } catch (SQLException ex) {
                System.out.println("Debts: " + ex.getMessage());
            }
        });
    }

    private void loadDebtSummary(JLabel c, JLabel o, JLabel ov) {
        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect(); Statement st = conn.createStatement()) {
                ResultSet r1 = st.executeQuery("SELECT COUNT(*),COALESCE(SUM(amount),0) FROM debts WHERE status!='PAID'");
                if (r1.next()) {
                    final int count = r1.getInt(1);
                    final int owed = r1.getInt(2);
                    SwingUtilities.invokeLater(() -> {
                        c.setText(String.valueOf(count));
                        o.setText("Ksh "+String.format("%,d", owed));
                    });
                }
                ResultSet r2 = st.executeQuery("SELECT COUNT(*) FROM debts WHERE status!='PAID' AND due_date<CURDATE()");
                if (r2.next()) {
                    final int overdue = r2.getInt(1);
                    SwingUtilities.invokeLater(() -> ov.setText(String.valueOf(overdue)));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void sendDebtSMSReminder(boolean allOverdue) {
        if (!allOverdue) {
            if (debtsTable == null || debtsTable.getSelectedRow() < 0) {
                info("Select a debt row first.");
                return;
            }
            int row = debtsTable.getSelectedRow();
            int debtId = (int) debtsModel.getValueAt(row, 0);
            String username = (String) debtsModel.getValueAt(row, 1);
            String person = (String) debtsModel.getValueAt(row, 3);
            String amtStr = (String) debtsModel.getValueAt(row, 4);
            String type = (String) debtsModel.getValueAt(row, 5);
            String due = (String) debtsModel.getValueAt(row, 6);

            double amount = 0;
            try {
                amount = Double.parseDouble(amtStr.replace("Ksh", "").replace(",", "").trim());
            } catch (NumberFormatException ignored) {}

            sendDebtNotification(username, person, amount, type, due);
            info("✓ Reminder notification sent to " + username);
        } else {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Send reminder notifications to ALL users with overdue debts?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            executor.submit(() -> {
                int sent = 0;
                try (Connection conn = SecureDatabaseConnection.connect();
                     PreparedStatement pst = conn.prepareStatement(
                             "SELECT d.id, d.user_id, u.username, d.person_name, d.amount, d.type, d.due_date " +
                                     "FROM debts d JOIN users u ON d.user_id = u.id " +
                                     "WHERE d.status != 'PAID' AND d.due_date < CURDATE()")) {
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        sendDebtNotification(
                                rs.getString("username"),
                                rs.getString("person_name"),
                                rs.getDouble("amount"),
                                rs.getString("type"),
                                rs.getString("due_date")
                        );
                        sent++;
                    }
                    final int finalSent = sent;
                    SwingUtilities.invokeLater(() -> info("✓ Sent reminder notifications to " + finalSent + " users."));
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> info("Error: " + ex.getMessage()));
                }
            });
        }
    }

    private void sendDebtNotification(String username, String person, double amount, String type, String dueDate) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("id");
                String message;
                if ("OWES".equals(type)) {
                    message = String.format("🔔 DEBT REMINDER: You owe %s Ksh %,.0f. Due: %s. Please clear your debt.",
                            person, amount, dueDate != null ? dueDate : "Not specified");
                } else {
                    message = String.format("🔔 DEBT REMINDER: %s owes you Ksh %,.0f. Due: %s. Follow up on this debt.",
                            person, amount, dueDate != null ? dueDate : "Not specified");
                }
                NotificationService.create(userId, message, NotificationService.WARNING);
                System.out.println("✓ Debt notification sent to: " + username);
            }
        } catch (SQLException e) {
            System.err.println("Failed to send debt notification: " + e.getMessage());
        }
    }

    // =====================================================
    //  TRANSACTIONS PANEL (OPTIMIZED)
    // =====================================================
    private JPanel buildTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(BG);
        top.add(pageHeader("Transaction Overview",
                "Monitor all transactions across every user account."), BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setBackground(BG);
        JTextField search = darkField("Filter by username...");
        search.setPreferredSize(new Dimension(200, 32));
        JButton filterBtn = buildBtn("Search",   BLUE);
        JButton allBtn    = buildBtn("Show All", GREEN);

        filterBtn.addActionListener(e -> {
            String q = search.getText().trim();
            if (!q.isEmpty()) loadTransactionsFiltered(q);
        });
        allBtn.addActionListener(e -> loadTransactionsTable());

        filters.add(search); filters.add(filterBtn); filters.add(allBtn);
        top.add(filters, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"ID","User","Week","Date","Amount","Type","Day"};
        txnsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        txnsTable = buildTable(txnsModel);
        colorizeCol(txnsTable, 4);
        panel.add(darkScroll(txnsTable), BorderLayout.CENTER);
        return panel;
    }

    private void loadTransactionsTable() {
        loadTransactionsFiltered(null);
    }

    private void loadTransactionsFiltered(String filter) {
        if (txnsModel == null) return;
        txnsLoaded = false;

        executor.submit(() -> {
            txnsModel.setRowCount(0);
            String sql = "SELECT m.id,u.username,m.weekNO,m.dateOfPayment,m.amount,m.day " +
                    "FROM mysaving2 m JOIN users u ON m.user_id=u.id ";
            if (filter != null) sql += "WHERE u.username LIKE ? ";
            sql += "ORDER BY m.id DESC LIMIT 200";

            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                if (filter != null) pst.setString(1, "%" + filter + "%");
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    final int id = rs.getInt("id");
                    final String username = rs.getString("username");
                    final int week = rs.getInt("weekNO");
                    final Date date = rs.getDate("dateOfPayment");
                    final int amt = rs.getInt("amount");
                    final String day = rs.getString("day");

                    SwingUtilities.invokeLater(() -> {
                        txnsModel.addRow(new Object[]{id, username, "Week "+week, date,
                                (amt < 0 ? "- " : "+ ") + "Ksh " + Math.abs(amt),
                                amt < 0 ? "WITHDRAWAL" : "DEPOSIT", day});
                    });
                }
                txnsLoaded = true;
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    // =====================================================
    //  VIDEO SUBMISSIONS PANEL (OPTIMIZED)
    // =====================================================
    private JPanel buildVideoPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setBackground(BG);
        outer.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBackground(BG);

        north.add(pageHeader("Video Submissions",
                "Review, approve, and manage user video submissions."));
        north.add(Box.createVerticalStrut(10));

        // Enhanced stats with 6 cards
        lblVideoPending  = new JLabel("--"); lblVideoPending.setFont(F_STAT);  lblVideoPending.setForeground(YELLOW);
        lblVideoApproved = new JLabel("--"); lblVideoApproved.setFont(F_STAT); lblVideoApproved.setForeground(GREEN);
        lblVideoEarned   = new JLabel("--"); lblVideoEarned.setFont(F_STAT);   lblVideoEarned.setForeground(BLUE);
        lblVideoViews    = new JLabel("--"); lblVideoViews.setFont(F_STAT);    lblVideoViews.setForeground(PURPLE);

        // NEW: Additional stats
        JLabel lblVideoLive = new JLabel("--"); lblVideoLive.setFont(F_STAT); lblVideoLive.setForeground(RED);
        JLabel lblTotalSubmissions = new JLabel("--"); lblTotalSubmissions.setFont(F_STAT); lblTotalSubmissions.setForeground(CYAN);

        // Store references
        this.lblVideoLive = lblVideoLive;
        this.lblTotalSubmissions = lblTotalSubmissions;

        JPanel statsRow = new JPanel(new GridLayout(2, 3, 14, 10));
        statsRow.setBackground(BG);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        statsRow.add(statCard("Pending",       lblVideoPending,  "[!]", YELLOW));
        statsRow.add(statCard("Approved",      lblVideoApproved, "[V]", GREEN));
        statsRow.add(statCard("Currently LIVE", lblVideoLive,    "[🔴]", RED));
        statsRow.add(statCard("Total Submissions", lblTotalSubmissions, "[📊]", CYAN));
        statsRow.add(statCard("Earned (Ksh)",  lblVideoEarned,   "[$]", BLUE));
        statsRow.add(statCard("Total Views",   lblVideoViews,    "[~]", PURPLE));
        north.add(statsRow);
        north.add(Box.createVerticalStrut(10));

        // Action Bar - Enhanced
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionBar.setBackground(BG);
        actionBar.setAlignmentX(0);

        JButton refreshBtn      = buildBtn("🔄 Refresh All",     BLUE);
        JButton instructionsBtn = buildBtn("📖 Instructions",     CYAN);
        JButton statsBtn        = buildBtn("📊 Reload Stats",    YELLOW);
        JButton exportBtn       = buildBtn("📄 Export Report",   PURPLE);
        JButton cleanupBtn      = buildBtn("🧹 Cleanup Old",     RED);

        refreshBtn.addActionListener(e -> refreshAllVideoTabs());
        statsBtn.addActionListener(e -> loadVideoStats());
        instructionsBtn.addActionListener(e -> showVideoInstructions());
        exportBtn.addActionListener(e -> exportVideoReport());
        cleanupBtn.addActionListener(e -> cleanupOldVideos());
        JButton featureMultipleBtn = buildBtn("⭐ Feature Multiple", YELLOW);
        featureMultipleBtn.addActionListener(e -> featureMultipleVideos());
        actionBar.add(featureMultipleBtn);

        actionBar.add(refreshBtn);
        actionBar.add(statsBtn);
        actionBar.add(instructionsBtn);
        actionBar.add(exportBtn);
        actionBar.add(cleanupBtn);
        north.add(actionBar);
        north.add(Box.createVerticalStrut(4));
        outer.add(north, BorderLayout.NORTH);

        // Tabs with Enhanced Tables
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(CARD_BG);
        tabs.setForeground(TEXT_BRIGHT);
        tabs.setFont(F_BODY);

        pendingModel  = new DefaultTableModel(videoColumns(), 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        approvedModel = new DefaultTableModel(videoColumns(), 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        liveModel     = new DefaultTableModel(videoColumns(), 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        rejectedModel = new DefaultTableModel(videoColumns(), 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tabs.addTab("⏳  Pending",  buildEnhancedVideoTab(pendingModel,  "PENDING"));
        tabs.addTab("✅  Approved", buildEnhancedVideoTab(approvedModel, "APPROVED"));
        tabs.addTab("🔴  Live",     buildEnhancedVideoTab(liveModel,     "LIVE"));
        tabs.addTab("❌  Rejected", buildEnhancedVideoTab(rejectedModel, "REJECTED"));
        tabs.addTab("📊  Analytics", buildVideoAnalyticsTab());

        outer.add(tabs, BorderLayout.CENTER);

        loadVideoStats();
        refreshAllVideoTabs();

        return outer;
    }
    private JPanel buildEnhancedVideoTab(DefaultTableModel model, String status) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTable table = buildTable(model);
        table.setRowHeight(38);

        // Better column sizing
        if (table.getColumnCount() > 5) {
            table.getColumnModel().getColumn(0).setPreferredWidth(40);  // ID
            table.getColumnModel().getColumn(1).setPreferredWidth(100); // User
            table.getColumnModel().getColumn(2).setPreferredWidth(120); // Business
            table.getColumnModel().getColumn(3).setPreferredWidth(180); // Title
            table.getColumnModel().getColumn(4).setPreferredWidth(80);  // Platform
            table.getColumnModel().getColumn(5).setPreferredWidth(200); // URL
            table.getColumnModel().getColumn(6).setPreferredWidth(80);  // Payment
            table.getColumnModel().getColumn(7).setPreferredWidth(80);  // Amount
            table.getColumnModel().getColumn(8).setPreferredWidth(140); // Submitted
        }

        // Action Panel - Enhanced
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actionPanel.setBackground(BG);
        actionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));

        JButton viewBtn   = buildBtn("▶ Open Video", BLUE);
        JButton copyBtn   = buildBtn("⧉ Copy URL",   CYAN);
        JButton viewUserBtn = buildBtn("👤 User Profile", PURPLE);
        JButton deleteVideoBtn = buildBtn("🗑 Delete Video", RED);  // ✅ ADD DELETE BUTTON

        viewBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { info("Select a video row first."); return; }
            String url = (String) model.getValueAt(row, 5);
            openUrl(url);
        });

        copyBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { info("Select a video row first."); return; }
            String url = (String) model.getValueAt(row, 5);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(url), null);
            info("URL copied to clipboard!");
        });

        viewUserBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { info("Select a video row first."); return; }
            String username = (String) model.getValueAt(row, 1);
            showUserTransactions(username);
        });

        // ✅ DELETE VIDEO ACTION
        deleteVideoBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { info("Select a video first."); return; }
            int videoId = (int) model.getValueAt(row, 0);
            String title = (String) model.getValueAt(row, 3);
            String user = (String) model.getValueAt(row, 1);

            int confirm = JOptionPane.showConfirmDialog(panel,
                    "⚠️ PERMANENT DELETION ⚠️\n\n" +
                            "Delete video '" + title + "'\n" +
                            "Submitted by: " + user + "\n\n" +
                            "This action CANNOT be undone!\n\n" +
                            "Are you sure?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                executor.submit(() -> {
                    boolean deleted = deleteVideoFromDatabase(videoId);
                    SwingUtilities.invokeLater(() -> {
                        if (deleted) {
                            info("✅ Video '" + title + "' deleted successfully!");
                            refreshAllVideoTabs();
                            loadVideoStats();
                        } else {
                            info("❌ Failed to delete video.");
                        }
                    });
                });
            }
        });

        actionPanel.add(viewBtn);
        actionPanel.add(copyBtn);
        actionPanel.add(viewUserBtn);
        actionPanel.add(deleteVideoBtn);  // ✅ ADD TO PANEL

        // Status-specific actions
        if ("PENDING".equals(status)) {
            JButton approveBtn = buildBtn("✓ Approve", GREEN);
            JButton rejectBtn  = buildBtn("✗ Reject",  RED);
            JButton viewDetailsBtn = buildBtn("📄 Details", YELLOW);

            approveBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { info("Select a video first."); return; }
                int videoId = (int) model.getValueAt(row, 0);
                String user = (String) model.getValueAt(row, 1);
                String title = (String) model.getValueAt(row, 3);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Approve video '" + title + "' by " + user + "?\nIt will be available for users to watch.",
                        "Confirm Approval", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    executor.submit(() -> {
                        approveVideo(videoId, user);
                        SwingUtilities.invokeLater(() -> {
                            refreshAllVideoTabs();
                            loadVideoStats();
                            info("✓ Video approved successfully!");
                        });
                    });
                }
            });

            rejectBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { info("Select a video first."); return; }
                int videoId = (int) model.getValueAt(row, 0);
                String reason = JOptionPane.showInputDialog(this,
                        "Enter rejection reason (will be sent to user):");
                if (reason != null && !reason.trim().isEmpty()) {
                    executor.submit(() -> {
                        rejectVideo(videoId, reason);
                        SwingUtilities.invokeLater(() -> {
                            refreshAllVideoTabs();
                            loadVideoStats();
                            info("Video rejected.");
                        });
                    });
                }
            });

            viewDetailsBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) return;
                String title = (String) model.getValueAt(row, 3);
                String business = (String) model.getValueAt(row, 2);
                String url = (String) model.getValueAt(row, 5);
                String submitted = String.valueOf(model.getValueAt(row, 8));

                String details = String.format(
                        "📹 VIDEO DETAILS\n\n" +
                                "Title: %s\n" +
                                "Business: %s\n" +
                                "URL: %s\n" +
                                "Submitted: %s\n\n" +
                                "Status: PENDING REVIEW",
                        title, business, url, submitted
                );
                JTextArea ta = new JTextArea(details);
                ta.setFont(F_MONO);
                ta.setBackground(CARD_BG);
                ta.setForeground(TEXT);
                ta.setEditable(false);
                JScrollPane sp = new JScrollPane(ta);
                sp.setPreferredSize(new Dimension(500, 300));
                JOptionPane.showMessageDialog(this, sp, "Video Details", JOptionPane.INFORMATION_MESSAGE);
            });

            actionPanel.add(approveBtn);
            actionPanel.add(rejectBtn);
            actionPanel.add(viewDetailsBtn);
        }

        if ("APPROVED".equals(status)) {
            JButton liveBtn = buildBtn("🔴 Set LIVE (1hr)", YELLOW);
            liveBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { info("Select a video first."); return; }
                int videoId = (int) model.getValueAt(row, 0);
                String title = (String) model.getValueAt(row, 3);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Make '" + title + "' LIVE for 1 hour?",
                        "Set Live", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    executor.submit(() -> {
                        setVideoLive(videoId);
                        SwingUtilities.invokeLater(() -> {
                            refreshAllVideoTabs();
                            loadVideoStats();
                            info("Video is now LIVE! Will auto-end in 1 hour.");
                        });
                    });
                }
            });
            actionPanel.add(liveBtn);
        }

        if ("LIVE".equals(status)) {
            JButton endBtn = buildBtn("⏹ End Live", RED);
            endBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { info("Select a video first."); return; }
                int videoId = (int) model.getValueAt(row, 0);
                String title = (String) model.getValueAt(row, 3);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "End live session for '" + title + "'?",
                        "End Live", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    executor.submit(() -> {
                        endLiveVideo(videoId);
                        SwingUtilities.invokeLater(() -> {
                            refreshAllVideoTabs();
                            loadVideoStats();
                            info("Live session ended.");
                        });
                    });
                }
            });
            actionPanel.add(endBtn);
        }

        if ("REJECTED".equals(status)) {
            JButton resubmitBtn = buildBtn("↺ Resubmit", YELLOW);
            resubmitBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { info("Select a video first."); return; }
                int videoId = (int) model.getValueAt(row, 0);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Move this video back to PENDING?",
                        "Resubmit", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    executor.submit(() -> {
                        updateVideoStatus(videoId, "PENDING", "Moved back to pending by admin.");
                        SwingUtilities.invokeLater(() -> {
                            refreshAllVideoTabs();
                            loadVideoStats();
                            info("Video moved to pending.");
                        });
                    });
                }
            });
            actionPanel.add(resubmitBtn);
        }

        panel.add(darkScroll(table), BorderLayout.CENTER);
        panel.add(actionPanel,       BorderLayout.SOUTH);
        return panel;
    }
    private boolean deleteVideoFromDatabase(int videoId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "DELETE FROM video_submissions WHERE id = ?")) {
            pst.setInt(1, videoId);
            int rows = pst.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Video " + videoId + " deleted successfully!");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Error deleting video: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private JPanel buildVideoAnalyticsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top Stats
        JPanel statsPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        statsPanel.setBackground(BG);

        JLabel totalVideos = new JLabel("0"); totalVideos.setFont(F_STAT); totalVideos.setForeground(BLUE);
        JLabel totalViews = new JLabel("0"); totalViews.setFont(F_STAT); totalViews.setForeground(PURPLE);
        JLabel avgViews = new JLabel("0"); avgViews.setFont(F_STAT); avgViews.setForeground(CYAN);
        JLabel totalEarned = new JLabel("Ksh 0"); totalEarned.setFont(F_STAT); totalEarned.setForeground(GREEN);
        JLabel pendingVideos = new JLabel("0"); pendingVideos.setFont(F_STAT); pendingVideos.setForeground(YELLOW);
        JLabel liveVideos = new JLabel("0"); liveVideos.setFont(F_STAT); liveVideos.setForeground(RED);
        JLabel approvedVideos = new JLabel("0"); approvedVideos.setFont(F_STAT); approvedVideos.setForeground(GREEN);
        JLabel rejectedVideos = new JLabel("0"); rejectedVideos.setFont(F_STAT); rejectedVideos.setForeground(RED);

        statsPanel.add(statCard("Total Videos", totalVideos, "[📹]", BLUE));
        statsPanel.add(statCard("Total Views", totalViews, "[👁]", PURPLE));
        statsPanel.add(statCard("Avg Views/Video", avgViews, "[📊]", CYAN));
        statsPanel.add(statCard("Total Earned", totalEarned, "[💰]", GREEN));
        statsPanel.add(statCard("Pending", pendingVideos, "[⏳]", YELLOW));
        statsPanel.add(statCard("Live Now", liveVideos, "[🔴]", RED));
        statsPanel.add(statCard("Approved", approvedVideos, "[✅]", GREEN));
        statsPanel.add(statCard("Rejected", rejectedVideos, "[❌]", RED));

        panel.add(statsPanel, BorderLayout.NORTH);

        // Load analytics data
        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect()) {
                Statement st = conn.createStatement();

                // Total videos
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM video_submissions");
                if (rs.next()) {
                    int count = rs.getInt(1);
                    SwingUtilities.invokeLater(() -> totalVideos.setText(String.valueOf(count)));
                }

                // Total views
                rs = st.executeQuery("SELECT COALESCE(SUM(views), 0) FROM video_submissions");
                if (rs.next()) {
                    int views = rs.getInt(1);
                    SwingUtilities.invokeLater(() -> totalViews.setText(String.format("%,d", views)));
                }

                // Average views
                rs = st.executeQuery("SELECT COALESCE(AVG(views), 0) FROM video_submissions");
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    SwingUtilities.invokeLater(() -> avgViews.setText(String.format("%.0f", avg)));
                }

                // Total earned
                rs = st.executeQuery("SELECT COALESCE(SUM(payment_amount), 0) FROM video_submissions WHERE payment_status = 'PAID'");
                if (rs.next()) {
                    double earned = rs.getDouble(1);
                    SwingUtilities.invokeLater(() -> totalEarned.setText("Ksh " + String.format("%,.0f", earned)));
                }

                // Status counts
                String[] statuses = {"PENDING", "LIVE", "APPROVED", "REJECTED"};
                JLabel[] labels = {pendingVideos, liveVideos, approvedVideos, rejectedVideos};

                for (int i = 0; i < statuses.length; i++) {
                    PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM video_submissions WHERE status = ?");
                    pst.setString(1, statuses[i]);
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        final int count = rs.getInt(1);
                        final int idx = i;
                        SwingUtilities.invokeLater(() -> labels[idx].setText(String.valueOf(count)));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // Popular Videos Table
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setBackground(BG);

        JLabel popularLabel = new JLabel("📊 Most Popular Videos");
        popularLabel.setFont(F_HEAD);
        popularLabel.setForeground(TEXT_BRIGHT);
        bottomPanel.add(popularLabel, BorderLayout.NORTH);

        String[] cols = {"Rank", "Title", "Business", "Views", "Status", "Submitted"};
        DefaultTableModel popularModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable popularTable = buildTable(popularModel);
        popularTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isSel, boolean hf, int row, int col) {
                JLabel l = new JLabel(String.valueOf(v));
                l.setFont(F_MONO);
                l.setOpaque(true);
                l.setForeground(PURPLE);
                l.setBackground(isSel ? new Color(31,111,235,60) : (row%2==0 ? CARD_BG : ROW_ALT));
                l.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
                return l;
            }
        });

        // Load popular videos
        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "SELECT video_title, business_name, views, status, submitted_at " +
                                 "FROM video_submissions WHERE status IN ('APPROVED', 'LIVE') " +
                                 "ORDER BY views DESC LIMIT 20")) {
                ResultSet rs = pst.executeQuery();
                int rank = 1;
                while (rs.next()) {
                    final int r = rank++;
                    final String title = rs.getString("video_title");
                    final String business = rs.getString("business_name");
                    final int views = rs.getInt("views");
                    final String status = rs.getString("status");
                    final String submitted = rs.getString("submitted_at");

                    SwingUtilities.invokeLater(() -> {
                        popularModel.addRow(new Object[]{r, title, business,
                                String.format("%,d", views), status, submitted});
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        bottomPanel.add(darkScroll(popularTable), BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.CENTER);

        return panel;
    }
    private void exportVideoReport() {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
        String filename = "VideoReport_" + timestamp + ".csv";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Export video submissions report to CSV?\n\n" +
                        "File: " + filename,
                "Export Report", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT vs.id, u.username, vs.business_name, vs.video_title, vs.platform, " +
                                 "vs.video_url, vs.status, vs.views, vs.payment_amount, vs.submitted_at " +
                                 "FROM video_submissions vs JOIN users u ON vs.user_id = u.id " +
                                 "ORDER BY vs.submitted_at DESC")) {

                try (FileWriter fw = new FileWriter(filename)) {
                    // CSV Header
                    fw.append("ID,Username,Business,Title,Platform,URL,Status,Views,Payment,Submitted\n");

                    while (rs.next()) {
                        fw.append(String.format("%d,%s,%s,%s,%s,%s,%s,%d,%.2f,%s\n",
                                rs.getInt("id"),
                                csvEscape(rs.getString("username")),
                                csvEscape(rs.getString("business_name")),
                                csvEscape(rs.getString("video_title")),
                                csvEscape(rs.getString("platform")),
                                rs.getString("video_url"),
                                rs.getString("status"),
                                rs.getInt("views"),
                                rs.getDouble("payment_amount"),
                                rs.getString("submitted_at")
                        ));
                    }

                    SwingUtilities.invokeLater(() -> {
                        info("✓ Report exported to:\n" + filename);
                        try {
                            Desktop.getDesktop().open(new File(filename));
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
                }
            } catch (SQLException | IOException e) {
                SwingUtilities.invokeLater(() -> info("Error exporting: " + e.getMessage()));
            }
        });
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        return value.replace(",", ";").replace("\"", "'");
    }
    private void cleanupOldVideos() {
        String[] options = {"Delete Rejected (>30 days)", "Delete Completed (>7 days)", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Select cleanup action:",
                "Video Cleanup",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 2) return; // Cancel

        String statusFilter = choice == 0 ? "REJECTED" : "COMPLETED";
        int daysOld = choice == 0 ? 30 : 7;

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Delete %s videos older than %d days?\n\nThis action cannot be undone!",
                        statusFilter, daysOld),
                "Confirm Cleanup",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 PreparedStatement pst = conn.prepareStatement(
                         "DELETE FROM video_submissions WHERE status = ? AND " +
                                 "submitted_at < DATE_SUB(NOW(), INTERVAL ? DAY)")) {
                pst.setString(1, statusFilter);
                pst.setInt(2, daysOld);
                int deleted = pst.executeUpdate();

                SwingUtilities.invokeLater(() -> {
                    info(String.format("✓ Deleted %d %s videos older than %d days.",
                            deleted, statusFilter, daysOld));
                    refreshAllVideoTabs();
                    loadVideoStats();
                });
            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> info("Error: " + e.getMessage()));
            }
        });
    }
    private void showVideoInstructions() {
        JDialog dialog = new JDialog(this, "Video Submission Instructions", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea instructions = new JTextArea();
        instructions.setBackground(BG);
        instructions.setForeground(TEXT);
        instructions.setFont(F_BODY);
        instructions.setEditable(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        instructions.setText(
                "📹 VIDEO SUBMISSION PROCESS\n" +
                        "═══════════════════════════════════════════════\n\n" +
                        "FOR USERS:\n" +
                        "───────────────────────────────────────────────\n" +
                        "1. Go to ATM Dashboard → Click '🎬 VIDEOS'\n" +
                        "2. Click '💰 SUBMIT YOUR VIDEO (Ksh 20)'\n" +
                        "3. Fill in:\n" +
                        "   • Video Title\n" +
                        "   • Business Name\n" +
                        "   • Platform (YouTube, TikTok, etc.)\n" +
                        "   • Video URL\n" +
                        "4. Pay Ksh 20 via M-Pesa\n" +
                        "5. Enter M-Pesa confirmation code\n" +
                        "6. Click SUBMIT\n\n" +
                        "ADMIN PROCESS:\n" +
                        "───────────────────────────────────────────────\n" +
                        "1. Review pending submissions in the '⏳ Pending' tab\n" +
                        "2. Verify:\n" +
                        "   • Video content is appropriate\n" +
                        "   • Business is legitimate\n" +
                        "   • M-Pesa payment is confirmed\n" +
                        "3. Approve or Reject with reason\n" +
                        "4. Approved videos go to '📹 VIDEOS' for users\n" +
                        "5. Set live for 1-hour featured exposure\n\n" +
                        "💰 EARNINGS: Ksh 20 per video submission\n" +
                        "👁 VIEW TRACKING: Each view is counted\n" +
                        "📊 ANALYTICS: Track performance in Analytics tab"
        );

        JScrollPane scroll = new JScrollPane(instructions);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        scroll.getViewport().setBackground(BG);

        JButton closeBtn = buildBtn("Close", BLUE);
        closeBtn.addActionListener(e -> dialog.dispose());

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(closeBtn, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private String[] videoColumns() {
        return new String[]{"ID","User","Business","Video Title","Platform",
                "Video URL","Payment","Amount","Submitted"};
    }

    private void loadVideoTab(DefaultTableModel model, String status) {
        model.setRowCount(0);
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT vs.id,u.username,COALESCE(vs.business_name,'—')," +
                             "vs.video_title,vs.platform,vs.video_url,vs.payment_status," +
                             "vs.payment_amount,vs.submitted_at " +
                             "FROM video_submissions vs JOIN users u ON vs.user_id=u.id " +
                             "WHERE vs.status=? ORDER BY vs.submitted_at DESC LIMIT 50")) {
            pst.setString(1, status);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), "Ksh " + rs.getDouble(8), rs.getTimestamp(9)
                });
            }
        } catch (SQLException ex) {
            System.out.println("Video tab ["+status+"]: " + ex.getMessage());
        }
    }
    private void refreshAllVideoTabs() {
        executor.submit(() -> {
            if (pendingModel != null) {
                SwingUtilities.invokeLater(() -> pendingModel.setRowCount(0));
                loadVideoTab(pendingModel, "PENDING");
            }
            if (approvedModel != null) {
                SwingUtilities.invokeLater(() -> approvedModel.setRowCount(0));
                loadVideoTab(approvedModel, "APPROVED");
            }
            if (liveModel != null) {
                SwingUtilities.invokeLater(() -> liveModel.setRowCount(0));
                loadVideoTab(liveModel, "LIVE");
            }
            if (rejectedModel != null) {
                SwingUtilities.invokeLater(() -> rejectedModel.setRowCount(0));
                loadVideoTab(rejectedModel, "REJECTED");
            }
        });
    }

    private void loadVideoStats() {
        if (lblVideoPending == null) return;
        executor.submit(() -> {
            try (Connection conn = SecureDatabaseConnection.connect();
                 Statement st = conn.createStatement()) {

                // Pending
                ResultSet r1 = st.executeQuery("SELECT COUNT(*) FROM video_submissions WHERE status='PENDING'");
                if (r1.next()) {
                    final int pending = r1.getInt(1);
                    SwingUtilities.invokeLater(() -> lblVideoPending.setText(String.valueOf(pending)));
                }

                // Approved (excluding LIVE)
                ResultSet r2 = st.executeQuery("SELECT COUNT(*) FROM video_submissions WHERE status='APPROVED'");
                if (r2.next()) {
                    final int approved = r2.getInt(1);
                    SwingUtilities.invokeLater(() -> lblVideoApproved.setText(String.valueOf(approved)));
                }

                // Live
                ResultSet rLive = st.executeQuery("SELECT COUNT(*) FROM video_submissions WHERE status='LIVE'");
                if (rLive.next()) {
                    final int live = rLive.getInt(1);
                    SwingUtilities.invokeLater(() -> lblVideoLive.setText(String.valueOf(live)));
                }

                // Total submissions
                ResultSet rTotal = st.executeQuery("SELECT COUNT(*) FROM video_submissions");
                if (rTotal.next()) {
                    final int total = rTotal.getInt(1);
                    SwingUtilities.invokeLater(() -> lblTotalSubmissions.setText(String.valueOf(total)));
                }

                // Earned
                ResultSet r3 = st.executeQuery("SELECT COALESCE(SUM(payment_amount),0) FROM video_submissions WHERE payment_status='PAID'");
                if (r3.next()) {
                    final double earned = r3.getDouble(1);
                    SwingUtilities.invokeLater(() -> lblVideoEarned.setText("Ksh " + String.format("%,.0f", earned)));
                }

                // Views
                ResultSet r4 = st.executeQuery("SELECT COALESCE(SUM(views),0) FROM video_submissions");
                if (r4.next()) {
                    final int views = r4.getInt(1);
                    SwingUtilities.invokeLater(() -> lblVideoViews.setText(String.format("%,d", views)));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void approveVideo(int videoId, String username) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE video_submissions SET status='APPROVED',approved_at=NOW()," +
                             "approved_by=? WHERE id=?")) {
            pst.setInt(1, Session.userId); pst.setInt(2, videoId);
            pst.executeUpdate();
            notifyVideoUser(videoId, "Your video has been APPROVED! It will go live soon.");
            SwingUtilities.invokeLater(() -> info("Video approved successfully."));
        } catch (SQLException ex) {
            SwingUtilities.invokeLater(() -> info("Error: " + ex.getMessage()));
        }
    }

    private void rejectVideo(int videoId, String reason) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE video_submissions SET status='REJECTED',admin_notes=? WHERE id=?")) {
            pst.setString(1, reason); pst.setInt(2, videoId);
            pst.executeUpdate();
            notifyVideoUser(videoId, "Your video submission was rejected. Reason: " + reason);
            SwingUtilities.invokeLater(() -> info("Video rejected."));
        } catch (SQLException ex) {
            SwingUtilities.invokeLater(() -> info("Error: " + ex.getMessage()));
        }
    }

    private void setVideoLive(int videoId) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            // Don't remove other live videos - just mark this one as live/featured
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE video_submissions SET status='LIVE', is_featured=TRUE, " +
                            "live_start_time=NOW(), live_end_time=DATE_ADD(NOW(), INTERVAL 1 HOUR) " +
                            "WHERE id=?");
            pst.setInt(1, videoId);
            pst.executeUpdate();

            notifyVideoUser(videoId, "🎬 Your video is now LIVE! It will be featured for 1 hour.");

            // Auto-end after 1 hour (but keep the video as APPROVED)
            new javax.swing.Timer(3_600_000, e -> {
                endLiveVideo(videoId);
                SwingUtilities.invokeLater(this::refreshAllVideoTabs);
            }) {{ setRepeats(false); start(); }};

            SwingUtilities.invokeLater(() -> info("✅ Video is now LIVE! Will auto-end in 1 hour."));
        } catch (SQLException ex) {
            SwingUtilities.invokeLater(() -> info("Error: " + ex.getMessage()));
        }
    }

    /**
     * End live session - video stays as APPROVED, not deleted
     */
    private void endLiveVideo(int videoId) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE video_submissions SET status='APPROVED', is_featured=FALSE, " +
                             "live_end_time=NOW() WHERE id=? AND status='LIVE'")) {
            pst.setInt(1, videoId);
            pst.executeUpdate();
            notifyVideoUser(videoId, "Your live session has ended. Thank you for sharing!");
            SwingUtilities.invokeLater(() -> info("Live session ended."));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    private void featureMultipleVideos() {
        JDialog dialog = new JDialog(this, "Feature Videos", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Select videos to feature (LIVE)", SwingConstants.CENTER);
        title.setFont(F_HEAD);
        title.setForeground(TEXT_BRIGHT);
        panel.add(title, BorderLayout.NORTH);

        // Get all approved videos
        DefaultListModel<String> listModel = new DefaultListModel<>();
        Map<String, Integer> videoMap = new HashMap<>();

        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, video_title, business_name FROM video_submissions WHERE status='APPROVED'")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String display = rs.getString("video_title") + " - " + rs.getString("business_name");
                listModel.addElement(display);
                videoMap.put(display, rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JList<String> videoList = new JList<>(listModel);
        videoList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        videoList.setBackground(CARD_BG);
        videoList.setForeground(TEXT);
        videoList.setFont(F_BODY);

        JScrollPane scroll = new JScrollPane(videoList);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton featureBtn = buildBtn("Feature Selected", GREEN);
        JButton cancelBtn = buildBtn("Cancel", RED);

        featureBtn.addActionListener(e -> {
            int[] selected = videoList.getSelectedIndices();
            if (selected.length == 0) {
                info("Select at least one video to feature.");
                return;
            }

            // Disable button to prevent double-clicking
            featureBtn.setEnabled(false);
            featureBtn.setText("⏳ Processing...");

            executor.submit(() -> {
                int featured = 0;
                try {
                    for (int idx : selected) {
                        String display = listModel.getElementAt(idx);
                        int videoId = videoMap.get(display);
                        setVideoLive(videoId);
                        featured++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Use a final copy of the count for the lambda
                final int finalFeaturedCount = featured;

                SwingUtilities.invokeLater(() -> {
                    // Now using finalFeaturedCount which is effectively final
                    info("✅ " + finalFeaturedCount + " videos are now LIVE!");
                    dialog.dispose();
                    refreshAllVideoTabs();
                    loadVideoStats();
                    featureBtn.setEnabled(true);
                    featureBtn.setText("Feature Selected");
                });
            });
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(featureBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void updateVideoStatus(int videoId, String status, String notes) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "UPDATE video_submissions SET status=?,admin_notes=? WHERE id=?")) {
            pst.setString(1, status); pst.setString(2, notes); pst.setInt(3, videoId);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void notifyVideoUser(int videoId, String message) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT user_id FROM video_submissions WHERE id=?")) {
            pst.setInt(1, videoId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) NotificationService.create(rs.getInt("user_id"),
                    message, NotificationService.INFO);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    private List<Map<String, Object>> getLiveVideos() {
        List<Map<String, Object>> videos = new ArrayList<>();
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, video_title, business_name, video_url, views, submitted_at, " +
                             "live_start_time, live_end_time " +
                             "FROM video_submissions WHERE status='LIVE' AND is_featured=TRUE " +
                             "ORDER BY live_start_time DESC")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Map<String, Object> video = new HashMap<>();
                video.put("id", rs.getInt("id"));
                video.put("video_title", rs.getString("video_title"));
                video.put("business_name", rs.getString("business_name"));
                video.put("video_url", rs.getString("video_url"));
                video.put("views", rs.getInt("views"));
                video.put("submitted_at", rs.getString("submitted_at"));
                video.put("live_start_time", rs.getString("live_start_time"));
                video.put("live_end_time", rs.getString("live_end_time"));
                videos.add(video);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return videos;
    }
    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            info("No URL available.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url.trim()));
        } catch (Exception ex) {
            info("Cannot open browser.\nURL: " + url);
        }
    }

    // =====================================================
    //  SHARED UI HELPERS
    // =====================================================
    private JPanel pageHeader(String title, String sub) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 4));
        p.setBackground(BG); p.setAlignmentX(0);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel t = new JLabel(title); t.setFont(F_TITLE); t.setForeground(TEXT_BRIGHT);
        JLabel s = new JLabel(sub);   s.setFont(F_BODY);  s.setForeground(TEXT_DIM);
        p.add(t); p.add(s); return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text); l.setFont(F_HEAD); l.setForeground(TEXT_BRIGHT);
        l.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));
        return l;
    }

    private JPanel statCard(String label, JLabel valLabel, String icon, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COL),
                        BorderFactory.createEmptyBorder(16, 18, 16, 18))));
        JLabel iconLbl = new JLabel(icon + "  " + label);
        iconLbl.setFont(F_SMALL); iconLbl.setForeground(TEXT_DIM);
        valLabel.setFont(F_STAT); valLabel.setForeground(accent);
        card.add(iconLbl,  BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    private JTable buildTable(DefaultTableModel model) {
        JTable t = new JTable(model) {
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row)
                        ? new Color(31,111,235,70)
                        : (row % 2 == 0 ? CARD_BG : ROW_ALT));
                c.setForeground(isRowSelected(row) ? TEXT_BRIGHT : TEXT);
                return c;
            }
        };
        t.setBackground(CARD_BG); t.setForeground(TEXT);
        t.setGridColor(BORDER_COL); t.setFont(F_BODY); t.setRowHeight(34);
        t.setShowVerticalLines(false); t.setFillsViewportHeight(true);
        t.setSelectionBackground(new Color(88,166,255,55));
        t.setSelectionForeground(TEXT_BRIGHT);
        t.setIntercellSpacing(new Dimension(0, 1));
        JTableHeader h = t.getTableHeader();
        h.setBackground(SIDEBAR_BG); h.setForeground(TEXT_DIM);
        h.setFont(new Font("Segoe UI", Font.BOLD, 12));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        h.setReorderingAllowed(false);
        return t;
    }

    private JScrollPane darkScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setAlignmentX(0);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        sp.getViewport().setBackground(CARD_BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JButton buildBtn(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(F_BTN);
        btn.setBackground(new Color(color.getRed(),color.getGreen(),color.getBlue(), 25));
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(color.getRed(),color.getGreen(),color.getBlue(),120), 1),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(color.getRed(),color.getGreen(),color.getBlue(), 50));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(color.getRed(),color.getGreen(),color.getBlue(), 25));
            }
        });
        return btn;
    }

    private JTextField darkField(String placeholder) {
        JTextField f = new JTextField();
        f.setBackground(new Color(22,27,34));
        f.setForeground(TEXT_DIM);
        f.setCaretColor(TEXT);
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                BorderFactory.createEmptyBorder(6,10,6,10)));
        f.setText(placeholder);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT); }
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BLUE,1),
                        BorderFactory.createEmptyBorder(5,9,5,9)));
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(TEXT_DIM); }
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COL),
                        BorderFactory.createEmptyBorder(6,10,6,10)));
            }
        });
        return f;
    }

    private JLabel fieldLbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(F_BODY);
        l.setForeground(TEXT_DIM);
        return l;
    }

    private TableCellRenderer statusRenderer() {
        return (t, v, isSel, hf, row, col) -> {
            JLabel l = new JLabel(String.valueOf(v));
            l.setFont(F_BODY);
            l.setOpaque(true);
            l.setForeground(String.valueOf(v).contains("Active") ? GREEN : RED);
            l.setBackground(isSel ? new Color(31,111,235,60) : (row%2==0 ? CARD_BG : ROW_ALT));
            l.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
            return l;
        };
    }

    private TableCellRenderer requestStatusRenderer() {
        return (t, v, isSel, hf, row, col) -> {
            JLabel l = new JLabel(" "+v+" ");
            l.setFont(new Font("Segoe UI",Font.BOLD,11));
            l.setOpaque(true);
            String sv = String.valueOf(v);
            l.setForeground(sv.equals("APPROVED") ? GREEN : sv.equals("REJECTED") ? RED : YELLOW);
            l.setBackground(isSel ? new Color(31,111,235,60) : (row%2==0 ? CARD_BG : ROW_ALT));
            return l;
        };
    }

    private TableCellRenderer debtStatusRenderer() {
        return (t, v, isSel, hf, row, col) -> {
            JLabel l = new JLabel(String.valueOf(v));
            l.setFont(F_BODY);
            l.setOpaque(true);
            String sv = String.valueOf(v);
            l.setForeground(sv.equals("PAID") ? GREEN : sv.equals("PARTIAL") ? YELLOW : RED);
            l.setBackground(isSel ? new Color(31,111,235,60) : (row%2==0 ? CARD_BG : ROW_ALT));
            l.setBorder(BorderFactory.createEmptyBorder(0,8,0,0));
            return l;
        };
    }

    private TableCellRenderer overdueRenderer() {
        return (t, v, isSel, hf, row, col) -> {
            JLabel l = new JLabel(String.valueOf(v));
            l.setFont(new Font("Segoe UI",Font.BOLD,12));
            l.setOpaque(true);
            l.setForeground("—".equals(v) ? TEXT_DIM : RED);
            l.setBackground(isSel ? new Color(31,111,235,60) : (row%2==0 ? CARD_BG : ROW_ALT));
            l.setBorder(BorderFactory.createEmptyBorder(0,8,0,0));
            return l;
        };
    }

    private void colorizeCol(JTable table, int col) {
        table.getColumnModel().getColumn(col).setCellRenderer(
                (t, v, isSel, hf, row, c) -> {
                    JLabel l = new JLabel(String.valueOf(v));
                    l.setFont(F_MONO);
                    l.setOpaque(true);
                    l.setForeground(String.valueOf(v).startsWith("- ") ? RED : GREEN);
                    l.setBackground(isSel ? new Color(31,111,235,60) : (row%2==0 ? CARD_BG : ROW_ALT));
                    l.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
                    return l;
                });
    }

    private int getUserId(String username) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement("SELECT id FROM users WHERE username=? LIMIT 1")) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── WrapLayout (for pictures grid) ──
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        public Dimension preferredLayoutSize(Container t) { return layoutSize(t, true); }
        public Dimension minimumLayoutSize(Container t) { return layoutSize(t, false); }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int w = target.getWidth();
                if (w == 0) w = Integer.MAX_VALUE;
                Insets ins = target.getInsets();
                int maxW = w-ins.left-ins.right-getHgap()*2, x=0, y=ins.top+getVgap(), rowH=0;
                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (x+d.width>maxW && x>0) { y+=rowH+getVgap(); x=0; rowH=0; }
                    x+=d.width+getHgap(); rowH=Math.max(rowH,d.height);
                }
                return new Dimension(w, y+rowH+getVgap()+ins.bottom);
            }
        }
    }

    private BufferedImage circularCrop(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
        g.drawImage(src.getScaledInstance(size, size, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return out;
    }

    private ImageIcon defaultAvatar(String username, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color[] palette = {BLUE,GREEN,YELLOW,PURPLE,CYAN,RED};
        g.setColor(palette[Math.abs(username.hashCode()) % palette.length]);
        g.fillOval(0, 0, size, size);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, size/3));
        FontMetrics fm = g.getFontMetrics();
        String ch = username.substring(0,1).toUpperCase();
        g.drawString(ch, (size-fm.stringWidth(ch))/2, (size+fm.getAscent()-fm.getDescent())/2);
        g.dispose();
        return new ImageIcon(img);
    }

    // =====================================================
    //  MAIN
    // =====================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminDashboard());
    }
}