package com.folio.library.service;

import com.folio.library.domain.Book;
import com.folio.library.domain.LibraryRecord;
import com.folio.library.domain.Loan;
import com.folio.library.domain.Member;
import com.folio.library.storage.LibraryDataStore;
import com.folio.library.storage.LibraryRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class LibraryService {
    private static final int MAX_ACTIVE_LOANS = 3;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ISBN_PATTERN = Pattern.compile("^(?:\\d{9}[\\dX]|\\d{13})$");

    private final LibraryRepository repository;
    private Map<String, Book> books;
    private Map<String, Member> members;
    private Map<String, Loan> loans;

    public LibraryService(LibraryRepository repository) throws IOException {
        this.repository = repository;
        LibraryDataStore data = repository.load();
        books = data.getBooks();
        members = data.getMembers();
        loans = data.getLoans();
    }

    public void seedDemoDataIfEmpty() throws LibraryException {
        if (!books.isEmpty() || !members.isEmpty() || !loans.isEmpty()) {
            return;
        }
        Map<String, Book> nextBooks = new LinkedHashMap<>();
        Map<String, Member> nextMembers = new LinkedHashMap<>();
        nextBooks.put("BK-1001", new Book("BK-1001", "The Midnight Library", "Matt Haig",
                "9780525559474", "Fiction", 4));
        nextBooks.put("BK-1002", new Book("BK-1002", "Atomic Habits", "James Clear",
                "9780735211292", "Personal growth", 3));
        nextBooks.put("BK-1003", new Book("BK-1003", "The Design of Everyday Things", "Don Norman",
                "9780465050659", "Design", 2));
        nextMembers.put("MB-1001", new Member("MB-1001", "Olivia Bennett", "olivia@example.com", "09171234567"));
        nextMembers.put("MB-1002", new Member("MB-1002", "Ethan Carter", "ethan@example.com", "09181234567"));
        persist(nextBooks, nextMembers, loans);
    }

    public List<Book> getBooks() {
        return books.values().stream().map(Book::copy)
                .sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Member> getMembers() {
        return members.values().stream().map(Member::copy)
                .filter(member -> !member.isArchived())
                .sorted(Comparator.comparing(Member::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Loan> getLoans() {
        return loans.values().stream().map(Loan::copy)
                .sorted(Comparator.comparing(Loan::getCheckoutDate).reversed()).toList();
    }

    public List<LibraryRecord> getRecords() {
        List<LibraryRecord> records = new ArrayList<>();
        records.addAll(getBooks());
        records.addAll(getMembers());
        records.addAll(getLoans());
        return List.copyOf(records);
    }

    public List<Book> searchBooks(String query) {
        String term = normalizeSearch(query);
        return getBooks().stream().filter(book -> matches(book.getTitle(), term)
                || matches(book.getAuthor(), term) || matches(book.getIsbn(), term)
                || matches(book.getCategory(), term) || matches(book.getId(), term)).toList();
    }

    public List<Member> searchMembers(String query) {
        String term = normalizeSearch(query);
        return getMembers().stream().filter(member -> matches(member.getFullName(), term)
                || matches(member.getEmail(), term) || matches(member.getId(), term)).toList();
    }

    public Book getBook(String id) throws LibraryException {
        Book book = books.get(id);
        if (book == null) {
            throw new LibraryException("The selected book no longer exists.");
        }
        return book.copy();
    }

    public Member getMember(String id) throws LibraryException {
        Member member = members.get(id);
        if (member == null) {
            throw new LibraryException("The selected member no longer exists.");
        }
        return member.copy();
    }

    public long getActiveLoanCount() {
        return loans.values().stream().filter(Loan::isActive).count();
    }

    public long getOverdueLoanCount() {
        return loans.values().stream().filter(Loan::isOverdue).count();
    }

    public long getActiveMemberCount() {
        return members.values().stream().filter(Member::isActive).count();
    }

    public long getAvailableCopyCount() {
        return books.values().stream().mapToLong(Book::getAvailableCopies).sum();
    }

    public Book addBook(String title, String author, String isbn, String category, int copies)
            throws LibraryException {
        validateBook(title, author, isbn, category, copies, null);
        String id = nextId("BK-", books);
        Book book = new Book(id, clean(title), clean(author), normalizeIsbn(isbn), clean(category), copies);
        Map<String, Book> nextBooks = new LinkedHashMap<>(books);
        nextBooks.put(id, book);
        persist(nextBooks, members, loans);
        return book.copy();
    }

    public void updateBook(String id, String title, String author, String isbn, String category, int copies)
            throws LibraryException {
        Book book = books.get(id);
        if (book == null) {
            throw new LibraryException("The selected book no longer exists.");
        }
        validateBook(title, author, isbn, category, copies, id);
        if (copies < book.getCheckedOutCopies()) {
            throw new LibraryException("Total copies cannot be fewer than copies currently on loan.");
        }
        Book updated = new Book(id, clean(title), clean(author), normalizeIsbn(isbn), clean(category),
                copies, copies - book.getCheckedOutCopies(), book.getCreatedAt());
        Map<String, Book> nextBooks = new LinkedHashMap<>(books);
        nextBooks.put(id, updated);
        persist(nextBooks, members, loans);
    }

    public void deleteBook(String id) throws LibraryException {
        Book book = books.get(id);
        if (book == null) {
            throw new LibraryException("The selected book no longer exists.");
        }
        boolean hasLoanHistory = loans.values().stream().anyMatch(loan -> loan.getBookId().equals(id));
        if (book.getCheckedOutCopies() > 0 || hasLoanHistory) {
            throw new LibraryException("This book has loan history and cannot be deleted. Update its details instead.");
        }
        Map<String, Book> nextBooks = new LinkedHashMap<>(books);
        nextBooks.remove(id);
        persist(nextBooks, members, loans);
    }

    public Member addMember(String fullName, String email, String phone) throws LibraryException {
        validateMember(fullName, email, null);
        String id = nextId("MB-", members);
        Member member = new Member(id, clean(fullName), clean(email), clean(phone));
        Map<String, Member> nextMembers = new LinkedHashMap<>(members);
        nextMembers.put(id, member);
        persist(books, nextMembers, loans);
        return member.copy();
    }

    public void updateMember(String id, String fullName, String email, String phone) throws LibraryException {
        Member member = members.get(id);
        if (member == null || member.isArchived()) {
            throw new LibraryException("The selected member no longer exists.");
        }
        validateMember(fullName, email, id);
        Member updated = new Member(id, clean(fullName), clean(email), clean(phone), member.isActive(),
                member.isArchived(), member.getCreatedAt());
        Map<String, Member> nextMembers = new LinkedHashMap<>(members);
        nextMembers.put(id, updated);
        persist(books, nextMembers, loans);
    }

    public void deleteMember(String id) throws LibraryException {
        Member member = members.get(id);
        if (member == null || member.isArchived()) {
            throw new LibraryException("The selected member no longer exists.");
        }
        boolean hasLoanHistory = loans.values().stream().anyMatch(loan -> loan.getMemberId().equals(id));
        boolean hasActiveLoans = loans.values().stream()
                .anyMatch(loan -> loan.isActive() && loan.getMemberId().equals(id));
        if (hasActiveLoans) {
            throw new LibraryException("Return this member's active loans before removing them from the directory.");
        }
        Map<String, Member> nextMembers = new LinkedHashMap<>(members);
        if (hasLoanHistory) {
            Member archived = member.copy();
            archived.archive();
            nextMembers.put(id, archived);
        } else {
            nextMembers.remove(id);
        }
        persist(books, nextMembers, loans);
    }

    public void setMemberActive(String id, boolean active) throws LibraryException {
        Member member = members.get(id);
        if (member == null || member.isArchived()) {
            throw new LibraryException("The selected member no longer exists.");
        }
        if (member.isActive() == active) {
            return;
        }
        boolean hasActiveLoans = loans.values().stream()
                .anyMatch(loan -> loan.isActive() && loan.getMemberId().equals(id));
        if (!active && hasActiveLoans) {
            throw new LibraryException("Return this member's active loans before deactivating the account.");
        }
        Member updated = new Member(id, member.getFullName(), member.getEmail(), member.getPhone(), active,
                member.isArchived(), member.getCreatedAt());
        Map<String, Member> nextMembers = new LinkedHashMap<>(members);
        nextMembers.put(id, updated);
        persist(books, nextMembers, loans);
    }

    public Loan checkoutBook(String bookId, String memberId) throws LibraryException {
        Book book = books.get(bookId);
        if (book == null) {
            throw new LibraryException("The selected book no longer exists.");
        }
        Member member = members.get(memberId);
        if (member == null) {
            throw new LibraryException("The selected member no longer exists.");
        }
        if (!member.isActive()) {
            throw new LibraryException("An inactive member cannot check out books.");
        }
        if (book.getAvailableCopies() < 1) {
            throw new LibraryException("No copies of this book are available.");
        }
        long memberLoans = loans.values().stream()
                .filter(loan -> loan.isActive() && loan.getMemberId().equals(memberId)).count();
        if (memberLoans >= MAX_ACTIVE_LOANS) {
            throw new LibraryException("A member may have no more than " + MAX_ACTIVE_LOANS + " active loans.");
        }
        LocalDate today = LocalDate.now();
        Loan loan = new Loan(nextId("LN-", loans), bookId, memberId, today, today.plusDays(14));
        Book checkedOutBook = book.copy();
        checkedOutBook.checkOutCopy();
        Map<String, Book> nextBooks = new LinkedHashMap<>(books);
        Map<String, Loan> nextLoans = new LinkedHashMap<>(loans);
        nextBooks.put(bookId, checkedOutBook);
        nextLoans.put(loan.getId(), loan);
        persist(nextBooks, members, nextLoans);
        return loan.copy();
    }

    public void returnBook(String loanId) throws LibraryException {
        Loan loan = loans.get(loanId);
        if (loan == null) {
            throw new LibraryException("The selected loan no longer exists.");
        }
        if (!loan.isActive()) {
            throw new LibraryException("This book has already been returned.");
        }
        Book book = books.get(loan.getBookId());
        if (book == null) {
            throw new LibraryException("The book for this loan no longer exists.");
        }
        Book returnedBook = book.copy();
        returnedBook.returnCopy();
        Loan returnedLoan = new Loan(loan.getId(), loan.getBookId(), loan.getMemberId(),
                loan.getCheckoutDate(), loan.getDueDate(), LocalDate.now(), loan.getCreatedAt());
        Map<String, Book> nextBooks = new LinkedHashMap<>(books);
        Map<String, Loan> nextLoans = new LinkedHashMap<>(loans);
        nextBooks.put(book.getId(), returnedBook);
        nextLoans.put(loanId, returnedLoan);
        persist(nextBooks, members, nextLoans);
    }

    public Book findBookById(String id) {
        Book book = books.get(id);
        return book == null ? null : book.copy();
    }

    public Member findMemberById(String id) {
        Member member = members.get(id);
        return member == null ? null : member.copy();
    }

    private void validateBook(String title, String author, String isbn, String category, int copies, String currentId)
            throws LibraryException {
        requireText(title, "Book title");
        requireText(author, "Author");
        requireText(category, "Category");
        String normalizedIsbn = normalizeIsbn(isbn);
        if (!ISBN_PATTERN.matcher(normalizedIsbn).matches()) {
            throw new LibraryException("Enter a valid 10- or 13-character ISBN.");
        }
        if (copies < 1 || copies > 10_000) {
            throw new LibraryException("Copies must be between 1 and 10,000.");
        }
        boolean duplicate = books.values().stream().anyMatch(book -> !book.getId().equals(currentId)
                && book.getIsbn().equals(normalizedIsbn));
        if (duplicate) {
            throw new LibraryException("A book with this ISBN is already in the catalog.");
        }
    }

    private void validateMember(String fullName, String email, String currentId) throws LibraryException {
        requireText(fullName, "Member name");
        if (clean(fullName).length() < 2) {
            throw new LibraryException("Member name must be at least 2 characters.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(clean(email)).matches()) {
            throw new LibraryException("Enter a valid email address.");
        }
        boolean duplicate = members.values().stream().anyMatch(member -> !member.isArchived()
                && !member.getId().equals(currentId)
                && member.getEmail().equalsIgnoreCase(clean(email)));
        if (duplicate) {
            throw new LibraryException("A member with this email already exists.");
        }
    }

    private void persist(Map<String, Book> nextBooks, Map<String, Member> nextMembers, Map<String, Loan> nextLoans)
            throws LibraryException {
        try {
            repository.save(new LibraryDataStore(nextBooks, nextMembers, nextLoans));
            books = nextBooks;
            members = nextMembers;
            loans = nextLoans;
        } catch (IOException exception) {
            throw new LibraryException("Could not save the library data. Check that the data folder is writable.",
                    exception);
        }
    }

    private static String normalizeSearch(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(String value, String term) {
        return term.isEmpty() || value.toLowerCase(Locale.ROOT).contains(term);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static void requireText(String value, String label) throws LibraryException {
        if (clean(value).isEmpty()) {
            throw new LibraryException(label + " is required.");
        }
    }

    private static String normalizeIsbn(String isbn) {
        return isbn == null ? "" : isbn.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private static String nextId(String prefix, Map<String, ?> existing) {
        String id;
        do {
            id = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (existing.containsKey(id));
        return id;
    }
}
