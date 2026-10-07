package com.library.service;

import com.library.config.Database;
import com.library.dao.BookDao;
import com.library.dao.TransactionDao;
import com.library.dao.UserDao;
import com.library.model.Book;
import com.library.model.Role;
import com.library.model.Transaction;
import com.library.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** The library rules: who may borrow, for how long, and what a late return costs. */
public class LibraryService {
    public static final int LOAN_DAYS = 14;
    public static final int MAX_BOOKS_PER_MEMBER = 3;
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("2.00");

    private final UserDao users;
    private final BookDao books;
    private final TransactionDao transactions;

    public LibraryService(UserDao users, BookDao books, TransactionDao transactions) {
        this.users = users;
        this.books = books;
        this.transactions = transactions;
    }

    /**
     * Issues a book to a member. Reducing the available copies and creating the
     * transaction record run in one database transaction: either both happen or neither.
     */
    public Transaction issueBook(int bookId, int userId, LocalDate today) throws SQLException, LibraryException {
        Book book = books.findById(bookId);
        if (book == null) {
            throw new LibraryException("No book with id " + bookId + ".");
        }
        User member = users.findById(userId);
        if (member == null || member.role() != Role.MEMBER) {
            throw new LibraryException("No member with id " + userId + ".");
        }

        int transactionId;
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (transactions.hasActive(con, userId, bookId)) {
                    throw new LibraryException(member.username() + " already has this book.");
                }
                if (transactions.countActiveByUser(con, userId) >= MAX_BOOKS_PER_MEMBER) {
                    throw new LibraryException("Borrow limit reached (" + MAX_BOOKS_PER_MEMBER + " books).");
                }
                if (!books.takeCopy(con, bookId)) {
                    throw new LibraryException("No copies of \"" + book.title() + "\" are available right now.");
                }
                transactionId = transactions.insert(con, bookId, userId, today, today.plusDays(LOAN_DAYS));
                con.commit();
            } catch (SQLException | LibraryException e) {
                con.rollback();
                throw e;
            }
        }
        return transactions.findById(transactionId);
    }

    /** Returns a book and gives back the fine that was charged (zero if returned on time). */
    public BigDecimal returnBook(int transactionId, LocalDate today) throws SQLException, LibraryException {
        Transaction tx = transactions.findById(transactionId);
        if (tx == null) {
            throw new LibraryException("No transaction with id " + transactionId + ".");
        }
        if (tx.isReturned()) {
            throw new LibraryException("This book was already returned on " + tx.returnDate() + ".");
        }

        BigDecimal fine = calculateFine(tx.dueDate(), today);
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!transactions.markReturned(con, transactionId, today, fine)) {
                    throw new LibraryException("This book was already returned.");
                }
                books.putCopyBack(con, tx.bookId());
                con.commit();
            } catch (SQLException | LibraryException e) {
                con.rollback();
                throw e;
            }
        }
        return fine;
    }

    public static BigDecimal calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
        return daysLate <= 0 ? BigDecimal.ZERO.setScale(2) : FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate));
    }
}
