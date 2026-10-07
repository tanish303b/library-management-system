package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One issue of a book to a member. returnDate is null while the book is still out. */
public record Transaction(int id, int bookId, String bookTitle, int userId, String username,
                          LocalDate issueDate, LocalDate dueDate, LocalDate returnDate, BigDecimal fine) {

    public boolean isReturned() {
        return returnDate != null;
    }
}
