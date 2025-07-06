package project3.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Utility for opening JDBC connections using .properties files in /props.
 */
public class DBConnectionUtil {

    /**
     * Connect using a single properties file that contains driver, url, user, and password.
     */
    public static Connection getConnection(String propsFilename) throws Exception {
        Properties p = new Properties();
        try (InputStream in = DBConnectionUtil.class.getResourceAsStream("/props/" + propsFilename)) {
            p.load(in);
        }
        Class.forName(p.getProperty("driver"));
        return DriverManager.getConnection(
                p.getProperty("url"),
                p.getProperty("user"),
                p.getProperty("password")
        );
    }

    /**
     * Connect to the DB in dbPropsFile (driver+url) but authenticate with creds in userPropsFile.
     */
    public static Connection getConnection(
            String dbPropsFile,
            String userPropsFile
    ) throws Exception {
        // Load DB‐only props (driver + url)
        Properties dbProps = new Properties();
        try (InputStream in = DBConnectionUtil.class.getResourceAsStream("/props/" + dbPropsFile)) {
            dbProps.load(in);
        }

        // Load user credentials
        Properties userProps = new Properties();
        try (InputStream in = DBConnectionUtil.class.getResourceAsStream("/props/" + userPropsFile)) {
            userProps.load(in);
        }

        Class.forName(dbProps.getProperty("driver"));
        return DriverManager.getConnection(
                dbProps.getProperty("url"),
                userProps.getProperty("user"),
                userProps.getProperty("password")
        );
    }
}
