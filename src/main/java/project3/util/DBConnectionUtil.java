package project3.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConnectionUtil {

    /**
     * Original: connect using a single props file that contains driver, url, user, password
     */
    public static Connection getConnection(String propsFilename) throws Exception {
        Properties p = new Properties();
        try (InputStream in = DBConnectionUtil.class
                .getResourceAsStream("/props/" + propsFilename)) {
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
     * New: connect to the DB in dbPropsFile but authenticate with the credentials in userPropsFile
     */
    public static Connection getConnection(
            String dbPropsFile,
            String userPropsFile
    ) throws Exception {
        // Load DB‐only props (driver + url)
        Properties dbProps = new Properties();
        try (InputStream in = DBConnectionUtil.class
                .getResourceAsStream("/props/" + dbPropsFile)) {
            dbProps.load(in);
        }

        // Load user credentials
        Properties userProps = new Properties();
        try (InputStream in = DBConnectionUtil.class
                .getResourceAsStream("/props/" + userPropsFile)) {
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
