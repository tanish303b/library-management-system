# Library Management System

A menu-driven console application for running a small library, written in Java with a MySQL database (JDBC). It keeps track of books, member accounts, and the issue and return of books, including due dates and late fines.

## Features

**Book records**
- Add, update, delete, list and search books (by title, author, category or ISBN)
- Tracks total and available copies of every book

**User accounts**
- Two roles: **librarian** (admin) and **member**
- Members can register themselves; the first librarian account is created on first run
- Passwords are stored as salted PBKDF2 hashes, never as plain text

**Transaction management**
- Issue a book to a member and return it
- 14-day loan period, at most 3 books per member at a time
- Late fine of Rs. 2 per day, calculated automatically on return
- Reports: books currently issued, overdue books, full transaction history
- Members can see their own issued books and borrowing history

Issuing a book changes two tables (the book's available copies and the transaction record). Both changes run inside one database transaction, so if anything fails neither change is kept.

## Requirements

- Java 17 or newer (JDK)
- MySQL Server 8.0 or newer
- MySQL Connector/J (the JDBC driver)

## Setup

1. **Get the JDBC driver.** Download `mysql-connector-j-9.1.0.jar` from
   [Maven Central](https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.1.0/mysql-connector-j-9.1.0.jar)
   and put it in the `lib` folder.
2. **Create the configuration file.** Copy `db.properties.example` to `db.properties` and enter your MySQL user and password:

   ```
   db.url=jdbc:mysql://localhost:3306/library_db?createDatabaseIfNotExist=true
   db.user=root
   db.password=your_mysql_password
   ```

   `db.properties` is listed in `.gitignore`, so the password is never committed.
3. **Make sure MySQL is running.** The database `library_db` and its tables are created automatically the first time the application starts.

## How to run

On Windows, run `run.bat` from the project folder, or:

```
javac -d out -sourcepath src src\com\library\Main.java
java -cp "out;lib\*" com.library.Main
```

On macOS / Linux use `/` instead of `\`, and `:` instead of `;` in the classpath.

On the first run the application asks you to create the librarian account and offers to load a few sample books.

## Menus

```
--------- Librarian Menu ---------
 1. Add book              7. Issue book
 2. Update book           8. Return book
 3. Delete book           9. Books currently issued
 4. View all books       10. Overdue books
 5. Search books         11. All transactions
 6. View members         12. Register member
 0. Logout
```

Members get a smaller menu: view and search books, see their issued books and history, and change their password.

## Database design

| Table | Purpose | Main columns |
| --- | --- | --- |
| `users` | Librarians and members | `user_id`, `username` (unique), `full_name`, `email`, `password_hash`, `role` |
| `books` | Book catalogue | `book_id`, `isbn` (unique), `title`, `author`, `category`, `total_copies`, `available_copies` |
| `transactions` | One row per issued book | `transaction_id`, `book_id` → books, `user_id` → users, `issue_date`, `due_date`, `return_date`, `fine` |

A transaction with an empty `return_date` means the book is still with the member. The full table definitions are in [sql/schema.sql](sql/schema.sql).

## Project structure

```
src/com/library/
├── Main.java                    Loads the configuration and starts the app
├── config/Database.java         Connection settings, runs .sql scripts
├── model/                       Book, User, Transaction, Role
├── dao/                         SQL for each table (BookDao, UserDao, TransactionDao)
├── service/LibraryService.java  Library rules: issue, return, fine calculation
├── util/PasswordUtil.java       Password hashing and checking
└── ui/ConsoleUI.java            Menus and input handling
sql/
├── schema.sql                   Table definitions
└── sample_data.sql              Optional sample books
```

The code is layered: the console menus call the service, the service applies the library rules, and only the DAO classes contain SQL. All queries use `PreparedStatement`, which protects against SQL injection.

## Possible improvements

- A graphical interface (Swing or JavaFX)
- Book reservations when no copy is available
- Recording fine payments
