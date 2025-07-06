package project3.accountant;

import project3.util.DBConnectionUtil;
import project3.util.ResultSetTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * Accountant-only GUI:
 * - connects to operationslog via theaccountant.properties
 * - only allows SELECT queries
 * - displays results in a JTable
 */
public class AccountantApp extends JFrame {
    private JTextArea sqlArea;
    private JTable resultTable;
    private JButton connectBtn, disconnectBtn, executeBtn, clearSqlBtn, clearResultsBtn, closeBtn;
    private Connection conn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AccountantApp app = new AccountantApp();
            app.setVisible(true);
        });
    }

    public AccountantApp() {
        super("Project 3 — Accountant View");
        initComponents();
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        // Center: SQL text + results
        sqlArea     = new JTextArea(5, 60);
        resultTable = new JTable();
        JSplitPane center = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(sqlArea),
                new JScrollPane(resultTable)
        );
        center.setResizeWeight(0.3);

        // South: buttons
        JPanel south = new JPanel(new GridLayout(2, 3, 8, 8));
        connectBtn      = new JButton("Connect");
        disconnectBtn   = new JButton("Disconnect");
        executeBtn      = new JButton("Execute SQL");
        clearSqlBtn     = new JButton("Clear SQL");
        clearResultsBtn = new JButton("Clear Results");
        closeBtn        = new JButton("Close App");
        south.add(connectBtn);
        south.add(disconnectBtn);
        south.add(executeBtn);
        south.add(clearSqlBtn);
        south.add(clearResultsBtn);
        south.add(closeBtn);

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        disconnectBtn.setEnabled(false);
        executeBtn.setEnabled(false);
        clearSqlBtn.setEnabled(false);
        clearResultsBtn.setEnabled(false);

        connectBtn.addActionListener(e -> onConnect());
        disconnectBtn.addActionListener(e -> onDisconnect());
        executeBtn.addActionListener(e -> onExecute());
        clearSqlBtn.addActionListener(e -> sqlArea.setText(""));
        clearResultsBtn.addActionListener(e -> resultTable.setModel(new DefaultTableModel()));
        closeBtn.addActionListener(e -> {
            if (conn != null) try { conn.close(); } catch (Exception ignored){}
            System.exit(0);
        });
    }

    private void onConnect() {
        // prompt for credentials
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(2,2,5,5));
        p.add(new JLabel("Username:")); p.add(userField);
        p.add(new JLabel("Password:")); p.add(passField);

        if (JOptionPane.showConfirmDialog(this, p, "Accountant Login",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        // verify against theaccountant.properties
        Properties acctProps = loadProps("theaccountant.properties");
        if (!acctProps.getProperty("user").equals(userField.getText()) ||
                !acctProps.getProperty("password").equals(new String(passField.getPassword()))) {
            JOptionPane.showMessageDialog(this,
                    "Invalid credentials", "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // open operationslog connection
        try {
            conn = DBConnectionUtil.getConnection("theaccountant.properties");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Connection error:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        connectBtn.setEnabled(false);
        disconnectBtn.setEnabled(true);
        executeBtn.setEnabled(true);
        clearSqlBtn.setEnabled(true);
        clearResultsBtn.setEnabled(true);
    }

    private void onDisconnect() {
        try { if (conn != null) conn.close(); } catch (Exception ignored){}
        conn = null;
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        executeBtn.setEnabled(false);
        clearSqlBtn.setEnabled(false);
        clearResultsBtn.setEnabled(false);
    }

    private void onExecute() {
        String raw = sqlArea.getText();
        if (raw == null || raw.trim().isEmpty()) return;

        // strip trailing semicolon
        String sql = raw.trim();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length()-1).trim();

        // only allow SELECT
        String first = sql.split("\\s+")[0].toLowerCase();
        if (!"select".equals(first)) {
            JOptionPane.showMessageDialog(this,
                    "Permission denied: only SELECT allowed",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // scrollable so our table model can read metadata
            Statement stmt = conn.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );
            ResultSet rs = stmt.executeQuery(sql);
            TableModel model = new ResultSetTableModel(rs);
            resultTable.setModel(model);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Properties loadProps(String filename) {
        try (InputStream in = getClass().getResourceAsStream("/props/" + filename)) {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load props: " + filename, e);
        }
    }
}
