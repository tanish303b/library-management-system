package com.library.service;

/** Thrown when a library rule is broken (no copies left, borrow limit reached, ...). */
public class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}
