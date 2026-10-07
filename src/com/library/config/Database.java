package com.library.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/** Holds the MySQL connection settings and hands out connections. */
public final class Database {
    private static String url;
    private static String user;
    private static String password;

    private Database() {
    }

    public static void load(Path propertiesFile) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propertiesFile)) {
            props.load(in);
        }
        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        password = props.getProperty("db.password");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Runs every statement of a .sql file (statements are separated by ';'). */
    public static void runScript(Path sqlFile) throws IOException, SQLException {
        StringBuilder sql = new StringBuilder();
        for (String line : Files.readAllLines(sqlFile)) {
            if (!line.trim().startsWith("--")) {
                sql.append(line).append('\n');
            }
        }
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            for (String statement : sql.toString().split(";")) {
                if (!statement.isBlank()) {
                    st.execute(statement);
                }
            }
        }
    }
}
