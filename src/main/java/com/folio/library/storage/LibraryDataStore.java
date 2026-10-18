package com.folio.library.storage;

import com.folio.library.domain.Book;
import com.folio.library.domain.Loan;
import com.folio.library.domain.Member;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LibraryDataStore implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LinkedHashMap<String, Book> books;
    private final LinkedHashMap<String, Member> members;
    private final LinkedHashMap<String, Loan> loans;

    public LibraryDataStore() {
        this(Map.of(), Map.of(), Map.of());
    }

    public LibraryDataStore(Map<String, Book> books, Map<String, Member> members, Map<String, Loan> loans) {
        this.books = new LinkedHashMap<>(books);
        this.members = new LinkedHashMap<>(members);
        this.loans = new LinkedHashMap<>(loans);
    }

    public Map<String, Book> getBooks() {
        return new LinkedHashMap<>(books);
    }

    public Map<String, Member> getMembers() {
        return new LinkedHashMap<>(members);
    }

    public Map<String, Loan> getLoans() {
        return new LinkedHashMap<>(loans);
    }
}
