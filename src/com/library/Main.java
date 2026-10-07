package com.library;

import com.library.config.Database;
import com.library.ui.ConsoleUI;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            Database.load(Path.of("db.properties"));
            Database.runScript(Path.of("sql", "schema.sql"));
        } catch (NoSuchFileException e) {
            System.out.println("Missing file: " + e.getFile());
            System.out.println("Run the application from the project folder (see README).");
            return;
        } catch (IOException e) {
            System.out.println("Could not read configuration: " + e.getMessage());
            return;
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL: " + e.getMessage());
            System.out.println("Check that MySQL is running and db.properties has the right password.");
            return;
        }
        new ConsoleUI().start();
    }
}
