-- Library Management System - table definitions
-- The application runs this file automatically at startup.
-- The database itself (library_db) is created by the JDBC URL in db.properties.

CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS books (
    book_id          INT AUTO_INCREMENT PRIMARY KEY,
    isbn             VARCHAR(20) UNIQUE,
    title            VARCHAR(200) NOT NULL,
    author           VARCHAR(100) NOT NULL,
    category         VARCHAR(50),
    total_copies     INT NOT NULL,
    available_copies INT NOT NULL,
    CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id        INT  NOT NULL,
    user_id        INT  NOT NULL,
    issue_date     DATE NOT NULL,
    due_date       DATE NOT NULL,
    return_date    DATE NULL,
    fine           DECIMAL(8, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_tx_book FOREIGN KEY (book_id) REFERENCES books (book_id),
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);
