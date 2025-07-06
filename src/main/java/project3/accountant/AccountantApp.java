/*
Name: <your name goes here>
Course: CNT 4714 Summer 2025
Assignment title: Project 3 – A Specialized Accountant Application
Date: July 6, 2025
Class: AccountantApp
*/

package project3.accountant;

import project3.util.DBConnectionUtil;
import project3.util.ResultSetTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Properties;

/**
 * The specialized accountant application for Project 3.
 * This application is restricted to theaccountant user and operationslog database only.
 */
public class AccountantApp extends JFrame {
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
            AccountantApp app = new AccountantApp();
            app.setVisible(true);
        });
    }

    public AccountantApp() {
        super("Project 3 Accountant Application");
        initComponents();
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        // North: Fixed database info and login fields
        JPanel north = new JPanel(new BorderLayout(10, 10));

        // Top row: Fixed database information (no dropdowns)
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JLabel dbLabel = new JLabel("Database: operationslog (Fixed)");
        JLabel userLabel = new JLabel("Login as: theaccountant (Fixed)");
        dbLabel.setFont(dbLabel.getFont().deriveFont(Font.BOLD));
        userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD));
        infoPanel.add(dbLabel);
        infoPanel.add(Box.createHorizontalStrut(50));
        infoPanel.add(userLabel);

        // Bottom row: Username and password fields for credential verification
        JPanel credentialsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        userField = new JTextField(15);
        passField = new JPasswordField(15);
        credentialsPanel.add(new JLabel("Username:"));
        credentialsPanel.add(userField);
        credentialsPanel.add(new JLabel("Password:"));
        credentialsPanel.add(passField);

        north.add(infoPanel, BorderLayout.NORTH);
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

    private void updateConnectionStatus(String status, boolean isConnected) {
        statusLabel.setText("Status: " + status);
        if (isConnected) {
            statusLabel.setForeground(Color.GREEN);
        } else {
            statusLabel.setForeground(Color.RED);
        }
    }

    private void onConnect() {
        // Get credentials from fields
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

        // Verify credentials against theaccountant.properties
        Properties accountantProps = loadProps("theaccountant.properties");
        if (!accountantProps.getProperty("user").equals(username) ||
                !accountantProps.getProperty("password").equals(password)) {
            JOptionPane.showMessageDialog(this,
                    "Credentials do not match theaccountant.properties",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            updateConnectionStatus("Login Failed", false);
            return;
        }
        loginUsername = accountantProps.getProperty("user");

        // Open connection to operationslog database using theaccountant.properties
        try {
            conn = DBConnectionUtil.getConnection("theaccountant.properties");
            updateConnectionStatus("Connected as " + loginUsername + " to operationslog", true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Cannot connect to operationslog database:\n" + ex.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            updateConnectionStatus("Connection Failed", false);
            return;
        }

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

            // Only allow SELECT, SHOW, DESC queries for theaccountant
            if (List.of("select","show","desc").contains(verb)) {
                // QUERY with scrollable result set
                Statement stmt = conn.createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY
                );
                ResultSet rs = stmt.executeQuery(sql);
                resultTable.setModel(new ResultSetTableModel(rs));
                // No logging for theaccountant operations
            } else {
                // Reject non-query commands
                JOptionPane.showMessageDialog(this,
                        "Only SELECT, SHOW, and DESC commands are allowed for theaccountant user.",
                        "Operation Not Permitted", JOptionPane.ERROR_MESSAGE);
                // Restore original status
                statusLabel.setText(originalStatus);
                statusLabel.setForeground(Color.GREEN);
                return;
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