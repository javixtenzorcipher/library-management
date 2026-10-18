package com.folio.library.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Loan extends LibraryRecord {
    private static final long serialVersionUID = 1L;

    private final String bookId;
    private final String memberId;
    private final LocalDate checkoutDate;
    private final LocalDate dueDate;
    private LocalDate returnedDate;

    public Loan(String id, String bookId, String memberId, LocalDate checkoutDate, LocalDate dueDate) {
        super(id);
        this.bookId = bookId;
        this.memberId = memberId;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
    }

    public Loan(String id, String bookId, String memberId, LocalDate checkoutDate, LocalDate dueDate,
                LocalDate returnedDate, LocalDateTime createdAt) {
        super(id, createdAt);
        this.bookId = bookId;
        this.memberId = memberId;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
        this.returnedDate = returnedDate;
    }

    public Loan copy() {
        return new Loan(getId(), bookId, memberId, checkoutDate, dueDate, returnedDate, getCreatedAt());
    }

    public String getBookId() {
        return bookId;
    }

    public String getMemberId() {
        return memberId;
    }

    public LocalDate getCheckoutDate() {
        return checkoutDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnedDate() {
        return returnedDate;
    }

    public boolean isActive() {
        return returnedDate == null;
    }

    public boolean isOverdue() {
        return isActive() && dueDate.isBefore(LocalDate.now());
    }

    public void markReturned() {
        if (!isActive()) {
            throw new IllegalStateException("This loan has already been returned.");
        }
        returnedDate = LocalDate.now();
    }

    @Override
    public String getDisplayName() {
        return "Loan " + getId();
    }
}
