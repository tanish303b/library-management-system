package com.library.dao;

import com.library.config.Database;
import com.library.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookDao {
    private static final String COLUMNS = "book_id, isbn, title, author, category, total_copies, available_copies";

    /** Inserts a new book (all copies available) and returns the generated id. */
    public int add(String isbn, String title, String author, String category, int copies) throws SQLException {
        String sql = "INSERT INTO books (isbn, title, author, category, total_copies, available_copies) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, isbn);
            ps.setString(2, title);
            ps.setString(3, author);
            ps.setString(4, category);
            ps.setInt(5, copies);
            ps.setInt(6, copies);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public List<Book> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM books ORDER BY title";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            return list(ps);
        }
    }

    public Book findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM books WHERE book_id = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Case-insensitive search in title, author, category and ISBN. */
    public List<Book> search(String keyword) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM books "
                + "WHERE title LIKE ? OR author LIKE ? OR category LIKE ? OR isbn LIKE ? ORDER BY title";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, pattern);
            }
            return list(ps);
        }
    }

    /** Saves edited details. available_copies must already be consistent with total_copies. */
    public void update(Book book) throws SQLException {
        String sql = "UPDATE books SET isbn = ?, title = ?, author = ?, category = ?, "
                + "total_copies = ?, available_copies = ? WHERE book_id = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, book.isbn());
            ps.setString(2, book.title());
            ps.setString(3, book.author());
            ps.setString(4, book.category());
            ps.setInt(5, book.totalCopies());
            ps.setInt(6, book.availableCopies());
            ps.setInt(7, book.id());
            ps.executeUpdate();
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM books WHERE book_id = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Takes one copy off the shelf. Returns false if no copy was available.
     * The check and the update happen in one statement, so two issues at the
     * same moment can never both take the last copy.
     */
    public boolean takeCopy(Connection con, int bookId) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies - 1 "
                + "WHERE book_id = ? AND available_copies > 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() == 1;
        }
    }

    public void putCopyBack(Connection con, int bookId) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.executeUpdate();
        }
    }

    private List<Book> list(PreparedStatement ps) throws SQLException {
        List<Book> result = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    private Book map(ResultSet rs) throws SQLException {
        return new Book(
                rs.getInt("book_id"),
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("category"),
                rs.getInt("total_copies"),
                rs.getInt("available_copies"));
    }
}
