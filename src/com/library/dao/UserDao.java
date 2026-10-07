package com.library.dao;

import com.library.config.Database;
import com.library.model.Role;
import com.library.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final String COLUMNS = "user_id, username, full_name, email, password_hash, role";

    /** Inserts a new user and returns the generated id. */
    public int create(String username, String fullName, String email, String passwordHash, Role role)
            throws SQLException {
        String sql = "INSERT INTO users (username, full_name, email, password_hash, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, passwordHash);
            ps.setString(5, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE user_id = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> findByRole(Role role) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE role = ? ORDER BY user_id";
        List<User> result = new ArrayList<>();
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public int countByRole(Role role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")));
    }
}
