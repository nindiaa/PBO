package com.smartkantin.config;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    public static Connection getConnection() { // HARUS ADA 'static'
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/db_smartkantin", "root", "");
        } catch (Exception e) {
            return null;
        }
    }
}