package project3.mainapp;

import project3.util.DBConnectionUtil;
import project3.util.ResultSetTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * The main client application for Project 3.
 */
public class ClientApp extends JFrame {
    private JComboBox<String> dbCombo, userCombo;
    private JTextField userField;
    private JPasswordField passField;
    private JTextArea sqlArea;
    private JTable resultTable;
    private JButton connectBtn, disconnectBtn, executeBtn, clearSqlBtn, clearResultsBtn, closeBtn;
    private JLabel statusLabel;
    private Connection conn;
    private String loginUsername;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientApp app = new ClientApp();
            app.setVisible(true);
        });
    }

    public ClientApp() {
        super("Project 3 SQL Client");
        initComponents();
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        // North: DB and user dropdowns with persistent login fields
        JPanel north = new JPanel(new BorderLayout(10, 10));

        // Top row: Database and user selection
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        dbCombo = new JComboBox<>(scanProps("project3", "bikedb", "operationslog"));
        userCombo = new JComboBox<>(scanProps("root", "client1", "client2"));
        selectionPanel.add(new JLabel("Database:"));
        selectionPanel.add(dbCombo);
        selectionPanel.add(new JLabel("Login as:"));
        selectionPanel.add(userCombo);

        // Bottom row: Always visible username and password fields
        JPanel credentialsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        userField = new JTextField(15);
        passField = new JPasswordField(15);
        credentialsPanel.add(new JLabel("Username:"));
        credentialsPanel.add(userField);
        credentialsPanel.add(new JLabel("Password:"));
        credentialsPanel.add(passField);

        north.add(selectionPanel, BorderLayout.NORTH);
        north.add(credentialsPanel, BorderLayout.SOUTH);

        // Center: SQL text area + results table
        sqlArea = new JTextArea(8, 60);
        resultTable = new JTable();
        JSplitPane center = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(sqlArea),
                new JScrollPane(resultTable)
        );
        center.setResizeWeight(0.3);

        // South: buttons and status
        JPanel south = new JPanel(new BorderLayout(10, 10));

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statusPanel.add(statusLabel);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");
        executeBtn = new JButton("Execute");
        clearSqlBtn = new JButton("Clear SQL");
        clearResultsBtn = new JButton("Clear Results");
        closeBtn = new JButton("Close App");
        buttonPanel.add(connectBtn);
        buttonPanel.add(disconnectBtn);
        buttonPanel.add(executeBtn);
        buttonPanel.add(clearSqlBtn);
        buttonPanel.add(clearResultsBtn);
        buttonPanel.add(closeBtn);

        south.add(statusPanel, BorderLayout.NORTH);
        south.add(buttonPanel, BorderLayout.SOUTH);

        // Frame layout
        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        // Initial states
        disconnectBtn.setEnabled(false);
        executeBtn.setEnabled(false);
        clearSqlBtn.setEnabled(false);
        clearResultsBtn.setEnabled(false);

        // Actions
        connectBtn.addActionListener(e -> onConnect());
        disconnectBtn.addActionListener(e -> onDisconnect());
        executeBtn.addActionListener(e -> onExecute());
        clearSqlBtn.addActionListener(e -> sqlArea.setText(""));
        clearResultsBtn.addActionListener(e -> resultTable.setModel(new DefaultTableModel()));
        closeBtn.addActionListener(e -> {
            if (conn != null) try { conn.close(); } catch (Exception ignored){}
            System.exit(0);
        });

        // Allow Enter key to trigger connection
        userField.addActionListener(e -> onConnect());
        passField.addActionListener(e -> onConnect());
    }

    private String[] scanProps(String... basenames) {
        File propsDir = new File("src/main/resources/props");
        if (!propsDir.isDirectory()) return new String[0];
        return Arrays.stream(propsDir.list())
                .filter(f -> f.endsWith(".properties"))
                .filter(f -> {
                    for (String b : basenames)
                        if (f.startsWith(b + ".properties")) return true;
                    return false;
                })
                .toArray(String[]::new);
    }

    private void updateConnectionStatus(String status, boolean isConnected) {
        statusLabel.setText("Status: " + status);
        if (isConnected) {
            statusLabel.setForeground(Color.GREEN);
        } else {
            statusLabel.setForeground(Color.RED);
        }
    }

    private void onConnect() {
        String dbPropsFile = (String) dbCombo.getSelectedItem();
        String userPropsFile = (String) userCombo.getSelectedItem();

        // Get credentials from always-visible fields
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Missing Credentials", JOptionPane.WARNING_MESSAGE);
            return;
        }

        updateConnectionStatus("Connecting...", false);
        statusLabel.setForeground(Color.ORANGE);

        // Verify credentials
        Properties userProps = loadProps(userPropsFile);
        if (!userProps.getProperty("user").equals(username) ||
                !userProps.getProperty("password").equals(password)) {
            JOptionPane.showMessageDialog(this,
                    "Credentials do not match " + userPropsFile,
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            updateConnectionStatus("Login Failed", false);
            return;
        }
        loginUsername = userProps.getProperty("user");

        // Open main connection
        try {
            conn = DBConnectionUtil.getConnection(dbPropsFile, userPropsFile);
            updateConnectionStatus("Connected as " + loginUsername + " to " + dbPropsFile, true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Cannot connect to DB:\n" + ex.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            updateConnectionStatus("Connection Failed", false);
            return;
        }

        dbCombo.setEnabled(false);
        userCombo.setEnabled(false);
        connectBtn.setEnabled(false);
        disconnectBtn.setEnabled(true);
        executeBtn.setEnabled(true);
        clearSqlBtn.setEnabled(true);
        clearResultsBtn.setEnabled(true);
    }

    private void onDisconnect() {
        try {
            if (conn != null) conn.close();
        } catch (Exception ignored){}

        conn = null;
        updateConnectionStatus("Disconnected", false);

        dbCombo.setEnabled(true);
        userCombo.setEnabled(true);
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        executeBtn.setEnabled(false);
        clearSqlBtn.setEnabled(false);
        clearResultsBtn.setEnabled(false);
    }

    private void onExecute() {
        String raw = sqlArea.getText();
        if (raw == null || raw.trim().isEmpty()) return;

        // Strip one trailing semicolon
        String sql = raw.trim();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length()-1).trim();
        if (sql.contains(";")) {
            JOptionPane.showMessageDialog(this,
                    "Multi-statement SQL is not allowed.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Update status to show execution
        String originalStatus = statusLabel.getText();
        updateConnectionStatus("Executing SQL...", true);
        statusLabel.setForeground(Color.ORANGE);

        try {
            String verb = sql.split("\\s+")[0].toLowerCase();
            if (List.of("select","show","desc").contains(verb)) {
                // QUERY with scrollable result set
                Statement stmt = conn.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY
                );
                ResultSet rs = stmt.executeQuery(sql);
                resultTable.setModel(new ResultSetTableModel(rs));
                if (!"theaccountant".equals(loginUsername)) logOperation(true);
            } else {
                // UPDATE
                Statement stmt = conn.createStatement();
                int count = stmt.executeUpdate(sql);
                JOptionPane.showMessageDialog(this,
                        count + " row(s) affected.", "Update Result",
                        JOptionPane.INFORMATION_MESSAGE);
                if (!"theaccountant".equals(loginUsername)) logOperation(false);
            }

            // Restore original status
            statusLabel.setText(originalStatus);
            statusLabel.setForeground(Color.GREEN);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);

            // Restore original status
            statusLabel.setText(originalStatus);
            statusLabel.setForeground(Color.GREEN);
        }
    }

    private void logOperation(boolean isQuery) {
        try (Connection logConn = DBConnectionUtil.getConnection("project3app.properties")) {
            PreparedStatement ps = logConn.prepareStatement(
                    "SELECT num_queries, num_updates FROM operationscount WHERE login_username = ?");
            ps.setString(1, loginUsername);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps = logConn.prepareStatement(
                        "INSERT INTO operationscount(login_username, num_queries, num_updates) VALUES(?,?,?)");
                ps.setString(1, loginUsername);
                ps.setInt(2, isQuery ? 1 : 0);
                ps.setInt(3, isQuery ? 0 : 1);
                ps.executeUpdate();
            } else {
                String col = isQuery ? "num_queries" : "num_updates";
                ps = logConn.prepareStatement(
                        "UPDATE operationscount SET " + col + " = " + col + " + 1 WHERE login_username = ?");
                ps.setString(1, loginUsername);
                ps.executeUpdate();
            }
        } catch (Exception ignore) {}
    }

    private Properties loadProps(String filename) {
        try (InputStream in = getClass().getResourceAsStream("/props/" + filename)) {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load properties: " + filename, e);
        }
    }
}