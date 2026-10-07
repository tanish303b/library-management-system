package com.library.dao;

import com.library.config.Database;
import com.library.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    private static final String SELECT =
            "SELECT t.transaction_id, t.book_id, b.title, t.user_id, u.username, "
                    + "t.issue_date, t.due_date, t.return_date, t.fine "
                    + "FROM transactions t "
                    + "JOIN books b ON b.book_id = t.book_id "
                    + "JOIN users u ON u.user_id = t.user_id ";

    public int insert(Connection con, int bookId, int userId, LocalDate issueDate, LocalDate dueDate)
            throws SQLException {
        String sql = "INSERT INTO transactions (book_id, user_id, issue_date, due_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bookId);
            ps.setInt(2, userId);
            ps.setDate(3, Date.valueOf(issueDate));
            ps.setDate(4, Date.valueOf(dueDate));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Marks an open transaction as returned. Returns false if it was already returned. */
    public boolean markReturned(Connection con, int transactionId, LocalDate returnDate, BigDecimal fine)
            throws SQLException {
        String sql = "UPDATE transactions SET return_date = ?, fine = ? "
                + "WHERE transaction_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(returnDate));
            ps.setBigDecimal(2, fine);
            ps.setInt(3, transactionId);
            return ps.executeUpdate() == 1;
        }
    }

    public int countActiveByUser(Connection con, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean hasActive(Connection con, int userId, int bookId) throws SQLException {
        String sql = "SELECT 1 FROM transactions WHERE user_id = ? AND book_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Transaction findById(int id) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE t.transaction_id = ?")) {
            ps.setInt(1, id);
            List<Transaction> rows = list(ps);
            return rows.isEmpty() ? null : rows.get(0);
        }
    }

    public List<Transaction> findAll() throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY t.transaction_id DESC")) {
            return list(ps);
        }
    }

    /** Books that are currently out. */
    public List<Transaction> findActive() throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SELECT + "WHERE t.return_date IS NULL ORDER BY t.due_date")) {
            return list(ps);
        }
    }

    /** Books that are still out although their due date has passed. */
    public List<Transaction> findOverdue(LocalDate today) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SELECT + "WHERE t.return_date IS NULL AND t.due_date < ? ORDER BY t.due_date")) {
            ps.setDate(1, Date.valueOf(today));
            return list(ps);
        }
    }

    public List<Transaction> findByUser(int userId, boolean activeOnly) throws SQLException {
        String where = "WHERE t.user_id = ? " + (activeOnly ? "AND t.return_date IS NULL " : "");
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + where + "ORDER BY t.transaction_id DESC")) {
            ps.setInt(1, userId);
            return list(ps);
        }
    }

    private List<Transaction> list(PreparedStatement ps) throws SQLException {
        List<Transaction> result = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date returned = rs.getDate("return_date");
                result.add(new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getDate("issue_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        returned == null ? null : returned.toLocalDate(),
                        rs.getBigDecimal("fine")));
            }
        }
        return result;
    }
}
