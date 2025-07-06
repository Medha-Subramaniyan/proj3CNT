/*
Name: Medha Subramaniyan
Course: CNT 4714 Summer 2025
Assignment title: Project 3 – A Specialized Accountant Application
Date: July 6, 2025
Class: DBConnectionUtil
*/

package project3.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Utility for opening JDBC connections using props files  !!!!!
 */
public class DBConnectionUtil {

    //connect using 1 file
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

    // Connect to the DB in dbPropsFile

    public static Connection getConnection(
            String dbPropsFile,
            String userPropsFile
    ) throws Exception {
        // Load DBprops
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
