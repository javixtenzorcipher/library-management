package com.folio.library.domain;

import java.time.LocalDateTime;

public final class Book extends LibraryRecord {
    private static final long serialVersionUID = 1L;

    private String title;
    private String author;
    private String isbn;
    private String category;
    private int totalCopies;
    private int availableCopies;

    public Book(String id, String title, String author, String isbn, String category, int copies) {
        super(id);
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.totalCopies = copies;
        this.availableCopies = copies;
    }

    public Book(String id, String title, String author, String isbn, String category,
                int totalCopies, int availableCopies, LocalDateTime createdAt) {
        super(id, createdAt);
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public Book copy() {
        return new Book(getId(), title, author, isbn, category, totalCopies, availableCopies, getCreatedAt());
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getCategory() {
        return category;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public int getCheckedOutCopies() {
        return totalCopies - availableCopies;
    }

    public void updateDetails(String title, String author, String isbn, String category, int totalCopies) {
        if (totalCopies < getCheckedOutCopies()) {
            throw new IllegalArgumentException("Total copies cannot be fewer than copies currently on loan.");
        }
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.availableCopies += totalCopies - this.totalCopies;
        this.totalCopies = totalCopies;
    }

    public void checkOutCopy() {
        if (availableCopies == 0) {
            throw new IllegalStateException("No copies of this book are available.");
        }
        availableCopies--;
    }

    public void returnCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies of this book are already in the library.");
        }
        availableCopies++;
    }

    @Override
    public String getDisplayName() {
        return title;
    }
}
