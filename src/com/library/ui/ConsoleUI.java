package com.library.ui;

import com.library.config.Database;
import com.library.dao.BookDao;
import com.library.dao.TransactionDao;
import com.library.dao.UserDao;
import com.library.model.Book;
import com.library.model.Role;
import com.library.model.Transaction;
import com.library.model.User;
import com.library.service.LibraryException;
import com.library.service.LibraryService;
import com.library.util.PasswordUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Menu-driven console screens for the librarian (admin) and for members. */
public class ConsoleUI {
    private final Scanner in = new Scanner(System.in);
    private final UserDao users = new UserDao();
    private final BookDao books = new BookDao();
    private final TransactionDao transactions = new TransactionDao();
    private final LibraryService library = new LibraryService(users, books, transactions);

    /** A menu action that may fail with a database or library-rule error. */
    private interface Action {
        void run() throws SQLException, LibraryException;
    }

    public void start() {
        System.out.println("=====================================");
        System.out.println("      LIBRARY MANAGEMENT SYSTEM      ");
        System.out.println("=====================================");
        try {
            if (users.countByRole(Role.ADMIN) == 0) {
                firstRunSetup();
            }
            boolean running = true;
            while (running) {
                System.out.println("\n1. Login");
                System.out.println("2. Register as a member");
                System.out.println("0. Exit");
                switch (readInt("Choose: ")) {
                    case 1 -> attempt(this::login);
                    case 2 -> attempt(() -> registerUser(Role.MEMBER));
                    case 0 -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            }
        } catch (NoSuchElementException e) {
            // Input was closed (Ctrl+Z / end of input) - just exit.
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        System.out.println("Goodbye!");
    }

    // ---------------------------------------------------------------- accounts

    private void firstRunSetup() throws SQLException {
        System.out.println("\nFirst run: no librarian account exists yet. Let's create one.");
        while (users.countByRole(Role.ADMIN) == 0) {
            registerUser(Role.ADMIN);
        }
        if (books.findAll().isEmpty() && readYesNo("Load sample books? (y/n): ")) {
            try {
                Database.runScript(Path.of("sql", "sample_data.sql"));
                System.out.println("Sample books loaded.");
            } catch (IOException e) {
                System.out.println("Could not read sql/sample_data.sql: " + e.getMessage());
            }
        }
    }

    private void registerUser(Role role) throws SQLException {
        String username = readRequired("Username: ");
        String fullName = readRequired("Full name: ");
        String email = readLine("Email (optional): ");
        String password = readPassword("Password (min 6 characters): ");
        try {
            int id = users.create(username, fullName, email.isEmpty() ? null : email,
                    PasswordUtil.hash(password), role);
            System.out.println("Account created. " + (role == Role.ADMIN ? "Librarian" : "Member") + " id: " + id);
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("That username is already taken.");
        }
    }

    private void login() throws SQLException {
        String username = readRequired("Username: ");
        String password = readLine("Password: ");
        User user = users.findByUsername(username);
        if (user == null || !PasswordUtil.verify(password, user.passwordHash())) {
            System.out.println("Wrong username or password.");
            return;
        }
        System.out.println("\nWelcome, " + user.fullName() + "!");
        if (user.role() == Role.ADMIN) {
            adminMenu();
        } else {
            memberMenu(user);
        }
    }

    private void changePassword(User user) throws SQLException {
        String current = readLine("Current password: ");
        User fresh = users.findById(user.id());
        if (!PasswordUtil.verify(current, fresh.passwordHash())) {
            System.out.println("Current password is wrong.");
            return;
        }
        users.updatePassword(user.id(), PasswordUtil.hash(readPassword("New password (min 6 characters): ")));
        System.out.println("Password changed.");
    }

    // ---------------------------------------------------------------- menus

    private void adminMenu() {
        while (true) {
            System.out.println("\n--------- Librarian Menu ---------");
            System.out.println(" 1. Add book              7. Issue book");
            System.out.println(" 2. Update book           8. Return book");
            System.out.println(" 3. Delete book           9. Books currently issued");
            System.out.println(" 4. View all books       10. Overdue books");
            System.out.println(" 5. Search books         11. All transactions");
            System.out.println(" 6. View members         12. Register member");
            System.out.println(" 0. Logout");
            switch (readInt("Choose: ")) {
                case 1 -> attempt(this::addBook);
                case 2 -> attempt(this::updateBook);
                case 3 -> attempt(this::deleteBook);
                case 4 -> attempt(() -> printBooks(books.findAll()));
                case 5 -> attempt(this::searchBooks);
                case 6 -> attempt(() -> printMembers(users.findByRole(Role.MEMBER)));
                case 7 -> attempt(this::issueBook);
                case 8 -> attempt(this::returnBook);
                case 9 -> attempt(() -> printTransactions(transactions.findActive()));
                case 10 -> attempt(() -> printTransactions(transactions.findOverdue(LocalDate.now())));
                case 11 -> attempt(() -> printTransactions(transactions.findAll()));
                case 12 -> attempt(() -> registerUser(Role.MEMBER));
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void memberMenu(User member) {
        while (true) {
            System.out.println("\n---------- Member Menu ----------");
            System.out.println("1. View all books");
            System.out.println("2. Search books");
            System.out.println("3. My issued books");
            System.out.println("4. My borrowing history");
            System.out.println("5. Change password");
            System.out.println("0. Logout");
            switch (readInt("Choose: ")) {
                case 1 -> attempt(() -> printBooks(books.findAll()));
                case 2 -> attempt(this::searchBooks);
                case 3 -> attempt(() -> printTransactions(transactions.findByUser(member.id(), true)));
                case 4 -> attempt(() -> printTransactions(transactions.findByUser(member.id(), false)));
                case 5 -> attempt(() -> changePassword(member));
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    /** Runs a menu action and reports errors without leaving the menu. */
    private void attempt(Action action) {
        try {
            action.run();
        } catch (LibraryException e) {
            System.out.println("Not allowed: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- books

    private void addBook() throws SQLException {
        String title = readRequired("Title: ");
        String author = readRequired("Author: ");
        String category = readLine("Category: ");
        String isbn = readLine("ISBN (optional): ");
        int copies = readPositiveInt("Number of copies: ");
        try {
            int id = books.add(isbn.isEmpty() ? null : isbn, title, author, category, copies);
            System.out.println("Book added with id " + id + ".");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("A book with this ISBN already exists.");
        }
    }

    private void updateBook() throws SQLException {
        Book book = books.findById(readInt("Book id: "));
        if (book == null) {
            System.out.println("No such book.");
            return;
        }
        System.out.println("Press Enter to keep the current value.");
        String title = readOrKeep("Title [" + book.title() + "]: ", book.title());
        String author = readOrKeep("Author [" + book.author() + "]: ", book.author());
        String category = readOrKeep("Category [" + book.category() + "]: ", book.category());
        String copiesText = readLine("Total copies [" + book.totalCopies() + "]: ");

        int total = book.totalCopies();
        if (!copiesText.isEmpty()) {
            try {
                total = Integer.parseInt(copiesText);
            } catch (NumberFormatException e) {
                System.out.println("Not a number - book not updated.");
                return;
            }
        }
        // Copies that are out with members cannot be removed from the total.
        if (total < book.issuedCopies() || total < 1) {
            System.out.println("Total copies cannot be less than 1 or less than the number of copies "
                    + "currently issued (" + book.issuedCopies() + ").");
            return;
        }
        int available = total - book.issuedCopies();
        books.update(new Book(book.id(), book.isbn(), title, author, category, total, available));
        System.out.println("Book updated.");
    }

    private void deleteBook() throws SQLException {
        int id = readInt("Book id: ");
        try {
            System.out.println(books.delete(id) ? "Book deleted." : "No such book.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("This book has issue records, so it cannot be deleted.");
        }
    }

    private void searchBooks() throws SQLException {
        printBooks(books.search(readRequired("Search (title / author / category / ISBN): ")));
    }

    // ---------------------------------------------------------------- issue / return

    private void issueBook() throws SQLException, LibraryException {
        int bookId = readInt("Book id: ");
        int memberId = readInt("Member id: ");
        Transaction tx = library.issueBook(bookId, memberId, LocalDate.now());
        System.out.println("Issued \"" + tx.bookTitle() + "\" to " + tx.username()
                + ". Transaction id: " + tx.id() + ", due on " + tx.dueDate() + ".");
    }

    private void returnBook() throws SQLException, LibraryException {
        int transactionId = readInt("Transaction id: ");
        BigDecimal fine = library.returnBook(transactionId, LocalDate.now());
        if (fine.signum() > 0) {
            System.out.println("Book returned late. Fine to collect: Rs. " + fine);
        } else {
            System.out.println("Book returned on time. No fine.");
        }
    }

    // ---------------------------------------------------------------- tables

    private void printBooks(List<Book> list) {
        if (list.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        String format = "%-4s %-32s %-22s %-18s %-14s %s%n";
        System.out.printf("%n" + format, "ID", "Title", "Author", "Category", "ISBN", "Available");
        System.out.println("-".repeat(104));
        for (Book b : list) {
            System.out.printf(format, b.id(), cut(b.title(), 32), cut(b.author(), 22), cut(b.category(), 18),
                    cut(b.isbn(), 14), b.availableCopies() + " of " + b.totalCopies());
        }
    }

    private void printMembers(List<User> list) {
        if (list.isEmpty()) {
            System.out.println("No members registered yet.");
            return;
        }
        String format = "%-4s %-18s %-26s %s%n";
        System.out.printf("%n" + format, "ID", "Username", "Full name", "Email");
        System.out.println("-".repeat(76));
        for (User u : list) {
            System.out.printf(format, u.id(), cut(u.username(), 18), cut(u.fullName(), 26), cut(u.email(), 28));
        }
    }

    private void printTransactions(List<Transaction> list) {
        if (list.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        String format = "%-4s %-30s %-16s %-11s %-11s %-11s %s%n";
        System.out.printf("%n" + format, "ID", "Book", "Member", "Issued", "Due", "Returned", "Fine (Rs.)");
        System.out.println("-".repeat(98));
        LocalDate today = LocalDate.now();
        for (Transaction t : list) {
            // For a book that is still out, show the fine it would cost if returned today.
            BigDecimal fine = t.isReturned() ? t.fine() : LibraryService.calculateFine(t.dueDate(), today);
            System.out.printf(format, t.id(), cut(t.bookTitle(), 30), cut(t.username(), 16), t.issueDate(),
                    t.dueDate(), t.isReturned() ? t.returnDate() : "-", fine);
        }
    }

    private static String cut(String text, int max) {
        if (text == null || text.isEmpty()) {
            return "-";
        }
        return text.length() <= max ? text : text.substring(0, max - 2) + "..";
    }

    // ---------------------------------------------------------------- input helpers

    private String readLine(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    private String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    private String readOrKeep(String prompt, String current) {
        String value = readLine(prompt);
        return value.isEmpty() ? current : value;
    }

    private String readPassword(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.length() >= 6) {
                return value;
            }
            System.out.println("Password must be at least 6 characters.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("Please enter a number greater than 0.");
        }
    }

    private boolean readYesNo(String prompt) {
        return readLine(prompt).toLowerCase().startsWith("y");
    }
}
