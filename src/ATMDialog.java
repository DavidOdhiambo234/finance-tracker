import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ATMDialog {

    // ── ATM Theme ──
    private static final Color BG       = new Color(10,  10,  10);
    private static final Color SCREEN   = new Color(15,  30,  15);
    private static final Color BORDER   = new Color(0,   200, 50);
    private static final Color GREEN    = new Color(0,   255, 70);
    private static final Color AMBER    = new Color(255, 176, 0);
    private static final Color RED      = new Color(220, 50,  50);
    private static final Color BLUE     = new Color(0,   180, 255);
    private static final Color DIM      = new Color(0,   180, 50);
    private static final Color INPUT_BG = new Color(5,   20,  5);
    private static final Font  FONT     = new Font("Courier New", Font.PLAIN,  12);
    private static final Font  FONT_B   = new Font("Courier New", Font.BOLD,   13);

    // =====================================================
    //  INPUT DIALOG
    // =====================================================
    public static String input(Component parent, String message) {
        return input(parent, message, "INPUT", false);
    }

    public static String passwordInput(Component parent, String message) {
        return input(parent, message, "SECURE INPUT", true);
    }

    private static String input(Component parent, String message,
                                String title, boolean password) {
        JDialog dlg = createDialog(parent, title, 440, 210);
        final String[] result = {null};

        JPanel panel = styledPanel(BORDER);

        JLabel msg = wrap(message, GREEN);
        panel.add(msg, BorderLayout.NORTH);

        JTextField field;
        if (password) {
            JPasswordField pf = new JPasswordField();
            pf.setEchoChar('●');
            field = pf;
        } else {
            field = new JTextField();
        }
        styleField(field);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(SCREEN);
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        center.add(field, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        JPanel btns = buttonRow();
        JButton ok     = atmBtn("OK",     BORDER);
        JButton cancel = atmBtn("CANCEL", RED);

        ok.addActionListener(e -> {
            result[0] = password
                    ? new String(((JPasswordField) field).getPassword())
                    : field.getText();
            dlg.dispose();
        });
        cancel.addActionListener(e -> dlg.dispose());
        field.addActionListener(e -> ok.doClick());

        btns.add(ok);
        btns.add(cancel);
        panel.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(panel);
        dlg.getRootPane().setDefaultButton(ok);
        field.requestFocusInWindow();
        dlg.setVisible(true);

        return result[0];
    }

    // =====================================================
    //  CONFIRM DIALOG
    // =====================================================
    public static boolean confirm(Component parent, String message) {
        return confirm(parent, message, "CONFIRM");
    }

    public static boolean confirm(Component parent, String message,
                                  String title) {
        JDialog dlg = createDialog(parent, title, 440, 200);
        final boolean[] result = {false};

        JPanel panel = styledPanel(BORDER);

        JLabel msg = wrap(message, GREEN);
        msg.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(msg, BorderLayout.CENTER);

        JPanel btns = buttonRow();
        JButton yes = atmBtn("YES", GREEN);
        JButton no  = atmBtn("NO",  RED);

        yes.addActionListener(e -> { result[0] = true; dlg.dispose(); });
        no.addActionListener(e  -> dlg.dispose());

        btns.add(yes);
        btns.add(no);
        panel.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(panel);
        dlg.setVisible(true);
        return result[0];
    }

    // =====================================================
    //  MESSAGE DIALOGS
    // =====================================================
    public static void info(Component parent, String message) {
        message(parent, message, "INFO", GREEN);
    }

    public static void success(Component parent, String message, String exportComplete) {
        message(parent, message, "SUCCESS", GREEN);
    }

    public static void error(Component parent, String message) {
        message(parent, message, "ERROR", RED);
    }

    public static void warning(Component parent, String message) {
        message(parent, message, "WARNING", AMBER);
    }

    public static void message(Component parent, String message,
                               String title, Color color) {
        JDialog dlg = createDialog(parent, title, 440, 190);

        JPanel panel = styledPanel(color);

        JLabel msg = wrap(message, color);
        msg.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(msg, BorderLayout.CENTER);

        JPanel btns = buttonRow();
        JButton ok = atmBtn("OK", color);
        ok.addActionListener(e -> dlg.dispose());
        btns.add(ok);
        panel.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(panel);
        dlg.getRootPane().setDefaultButton(ok);
        dlg.setVisible(true);
    }

    // =====================================================
    //  COMBO CHOOSER (for request types, etc.)
    // =====================================================
    public static String choose(Component parent, String message,
                                String title, String[] options) {
        JDialog dlg = createDialog(parent, title, 440, 220);
        final String[] result = {null};

        JPanel panel = styledPanel(AMBER);

        JLabel msg = wrap(message, AMBER);
        panel.add(msg, BorderLayout.NORTH);

        JComboBox<String> combo = new JComboBox<>(options);
        combo.setBackground(INPUT_BG);
        combo.setForeground(GREEN);
        combo.setFont(FONT);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(SCREEN);
        center.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        center.add(combo, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        JPanel btns = buttonRow();
        JButton ok     = atmBtn("OK",     BORDER);
        JButton cancel = atmBtn("CANCEL", RED);

        ok.addActionListener(e -> {
            result[0] = (String) combo.getSelectedItem();
            dlg.dispose();
        });
        cancel.addActionListener(e -> dlg.dispose());

        btns.add(ok);
        btns.add(cancel);
        panel.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(panel);
        dlg.setVisible(true);
        return result[0];
    }

    // =====================================================
    //  TEXT AREA INPUT (for long descriptions)
    // =====================================================
    public static String textArea(Component parent, String message,
                                  String title) {
        JDialog dlg = createDialog(parent, title, 460, 300);
        final String[] result = {null};

        JPanel panel = styledPanel(BORDER);

        JLabel msg = wrap(message, DIM);
        panel.add(msg, BorderLayout.NORTH);

        JTextArea area = new JTextArea(5, 35);
        area.setBackground(INPUT_BG);
        area.setForeground(GREEN);
        area.setCaretColor(GREEN);
        area.setFont(FONT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(null);
        sp.getViewport().setBackground(INPUT_BG);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(SCREEN);
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        center.add(sp, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        JPanel btns = buttonRow();
        JButton ok     = atmBtn("SUBMIT", BORDER);
        JButton cancel = atmBtn("CANCEL", RED);
        ok.addActionListener(e -> {
            result[0] = area.getText().trim();
            dlg.dispose();
        });
        cancel.addActionListener(e -> dlg.dispose());
        btns.add(ok);
        btns.add(cancel);
        panel.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(panel);
        dlg.setVisible(true);
        return result[0];
    }

    // =====================================================
    //  HELPERS
    // =====================================================
    private static JDialog createDialog(Component parent, String title,
                                        int w, int h) {
        Window window = parent != null
                ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog dlg;
        if (window instanceof Frame)
            dlg = new JDialog((Frame) window, title, true);
        else if (window instanceof Dialog)
            dlg = new JDialog((Dialog) window, title, true);
        else
            dlg = new JDialog((Frame) null, title, true);

        dlg.setSize(w, h);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(parent);
        dlg.getContentPane().setBackground(SCREEN);
        return dlg;
    }

    private static JPanel styledPanel(Color accentColor) {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(SCREEN);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 2),
                BorderFactory.createEmptyBorder(20, 24, 18, 24)
        ));
        return p;
    }

    private static JLabel wrap(String text, Color color) {
        JLabel l = new JLabel(
                "<html>" + text.replace("\n", "<br>") + "</html>");
        l.setFont(FONT_B);
        l.setForeground(color);
        return l;
    }

    private static JPanel buttonRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        p.setBackground(SCREEN);
        return p;
    }

    private static void styleField(JTextField f) {
        f.setBackground(INPUT_BG);
        f.setForeground(GREEN);
        f.setCaretColor(GREEN);
        f.setFont(new Font("Courier New", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
    }

    private static JButton atmBtn(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_B);
        btn.setBackground(new Color(5, 20, 5));
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(8, 22, 8, 22)
        ));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(
                        color.getRed() / 6,
                        color.getGreen() / 6,
                        color.getBlue() / 6));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(5, 20, 5));
            }
        });
        return btn;
    }
}