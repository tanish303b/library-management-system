package com.library.model;

public record Book(int id, String isbn, String title, String author, String category,
                   int totalCopies, int availableCopies) {

    public int issuedCopies() {
        return totalCopies - availableCopies;
    }
}
