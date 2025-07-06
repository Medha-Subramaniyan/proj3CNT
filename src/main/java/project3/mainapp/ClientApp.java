package project3.mainapp;

import project3.util.DBConnectionUtil;
import project3.util.ResultSetTableModel;
import javax.swing.table.DefaultTableModel;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.io.File;

/**
 * The main client application for Project 3.
 */
public class ClientApp extends JFrame {
    private JComboBox<String> dbCombo, userCombo;
    private JTextArea sqlArea;
    private JTable resultTable;
    private JButton connectBtn, disconnectBtn, executeBtn, clearSqlBtn, clearResultsBtn, closeBtn;
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
        pack();
        setSize(1000, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        // — North panel: two dropdowns —
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        dbCombo = new JComboBox<>(scanProps("project3", "bikedb", "operationslog"));
        userCombo = new JComboBox<>(scanProps("root", "client1", "client2"));
        north.add(new JLabel("Database:"));
        north.add(dbCombo);
        north.add(new JLabel("Login as:"));
        north.add(userCombo);

        // — Center: SQL text area + results table —
        sqlArea = new JTextArea(8, 60);
        resultTable = new JTable();
        JSplitPane center = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(sqlArea),
                new JScrollPane(resultTable)
        );
        center.setResizeWeight(0.3);

        // — South: buttons —
        JPanel south = new JPanel(new GridLayout(2, 4, 10, 10));
        connectBtn      = new JButton("Connect");
        disconnectBtn   = new JButton("Disconnect");
        executeBtn      = new JButton("Execute");
        clearSqlBtn     = new JButton("Clear SQL");
        clearResultsBtn = new JButton("Clear Results");
        closeBtn        = new JButton("Close App");

        south.add(connectBtn);
        south.add(disconnectBtn);
        south.add(executeBtn);
        south.add(clearSqlBtn);
        south.add(clearResultsBtn);
        south.add(closeBtn);

        // — Layout frame —
        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        // — Initial state —
        disconnectBtn.setEnabled(false);
        executeBtn.setEnabled(false);
        clearSqlBtn.setEnabled(false);
        clearResultsBtn.setEnabled(false);

        // — Wire button actions —
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

    /** Scan the resources/props folder for the given basenames + ".properties". */
    private String[] scanProps(String... basenames) {
        // Since listing inside a JAR is tricky, we'll assume
        // working dir is project root and props are under src/main/resources/props
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

    private void onConnect() {
        String dbPropsFile   = (String) dbCombo.getSelectedItem();
        String userPropsFile = (String) userCombo.getSelectedItem();

        // Prompt for credentials
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(2,2,5,5));
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        int result = JOptionPane.showConfirmDialog(
                this, panel, "Enter credentials", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        // Load the user props and verify
        Properties userProps = loadProps(userPropsFile);
        String user = userProps.getProperty("user");
        String pass = userProps.getProperty("password");
        if (!user.equals(userField.getText()) ||
                !pass.equals(new String(passField.getPassword()))) {
            JOptionPane.showMessageDialog(this,
                    "Credentials do not match " + userPropsFile,
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        loginUsername = user;

        try {
            // Open main DB connection
            conn = DBConnectionUtil.getConnection(dbPropsFile, userPropsFile);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Cannot connect to DB:\n" + ex.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Update UI state
        dbCombo.setEnabled(false);
        userCombo.setEnabled(false);
        connectBtn.setEnabled(false);
        disconnectBtn.setEnabled(true);
        executeBtn.setEnabled(true);
        clearSqlBtn.setEnabled(true);
        clearResultsBtn.setEnabled(true);
    }

    private void onDisconnect() {
        try { if (conn != null) conn.close(); }
        catch (Exception ignored) {}
        conn = null;
        dbCombo.setEnabled(true);
        userCombo.setEnabled(true);
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        executeBtn.setEnabled(false);
        clearSqlBtn.setEnabled(false);
        clearResultsBtn.setEnabled(false);
    }

    private void onExecute() {
        // 1) Grab raw text
        String raw = sqlArea.getText();
        if (raw == null || raw.trim().isEmpty()) {
            return;  // nothing to do
        }

        // 2) Trim and strip a single trailing semicolon if present
        String sql = raw.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

        // 3) Now reject only if there's still any other semicolons
        if (sql.contains(";")) {
            JOptionPane.showMessageDialog(this,
                    "Multi-statement SQL is not allowed.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4) Execute as before
        try {
            String verb = sql.split("\\s+")[0].toLowerCase();
            if (List.of("select","show","desc").contains(verb)) {
                // QUERY
                Statement stmt = conn.createStatement();
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
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(), "SQL Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    /** Insert or update the operationslog entry for this user. */
    private void logOperation(boolean isQuery) {
        try (Connection logConn = DBConnectionUtil.getConnection("project3app.properties")) {
            // check existing
            PreparedStatement ps = logConn.prepareStatement(
                    "SELECT num_queries, num_updates FROM operationscount WHERE login_username = ?");
            ps.setString(1, loginUsername);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                // insert
                ps = logConn.prepareStatement(
                        "INSERT INTO operationscount(login_username, num_queries, num_updates) VALUES(?,?,?)");
                ps.setString(1, loginUsername);
                ps.setInt(2, isQuery ? 1 : 0);
                ps.setInt(3, isQuery ? 0 : 1);
                ps.executeUpdate();
            } else {
                // update
                String col = isQuery ? "num_queries" : "num_updates";
                ps = logConn.prepareStatement(
                        "UPDATE operationscount SET " + col + " = " + col + " + 1 WHERE login_username = ?");
                ps.setString(1, loginUsername);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            // Logging errors are silent per spec
            System.err.println("Logging failed: " + ex.getMessage());
        }
    }

    /** Load a .properties file from classpath:/props */
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
