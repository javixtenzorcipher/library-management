package com.folio.library.service;

import com.folio.library.domain.Book;
import com.folio.library.domain.LibraryRecord;
import com.folio.library.domain.Loan;
import com.folio.library.domain.Member;
import com.folio.library.storage.FileLibraryRepository;
import com.folio.library.storage.LibraryDataStore;
import com.folio.library.storage.LibraryRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LibraryServiceTest {
    private LibraryServiceTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("library-service-test-");
        Path dataFile = directory.resolve("library-data.ser");
        try {
            verifyPersistenceFailureDoesNotChangeState();
            exerciseLibraryRules(dataFile);
            System.out.println("LibraryServiceTest: all checks passed.");
        } finally {
            Files.deleteIfExists(dataFile);
            Files.deleteIfExists(directory);
        }
    }

    private static void exerciseLibraryRules(Path dataFile) throws Exception {
        LibraryService service = new LibraryService(new FileLibraryRepository(dataFile));
        check(service.getBooks().isEmpty(), "A new library should start empty.");
        expectLibraryError(() -> service.addBook(" ", "Author", "9780061120084", "Fiction", 1),
                "Empty titles must be rejected.");
        expectLibraryError(() -> service.addBook("Book", "Author", "bad-isbn", "Fiction", 1),
                "Invalid ISBNs must be rejected.");
        expectLibraryError(() -> service.addMember("Reader", "not-an-email", ""),
                "Invalid emails must be rejected.");

        Book firstBook = service.addBook("The Hobbit", "J. R. R. Tolkien", "9780547928227", "Fantasy", 2);
        Book secondBook = service.addBook("Dune", "Frank Herbert", "9780441172719", "Science fiction", 1);
        Book thirdBook = service.addBook("Piranesi", "Susanna Clarke", "9781635575637", "Fantasy", 1);
        Book fourthBook = service.addBook("Kindred", "Octavia E. Butler", "9780807083697", "Fiction", 1);
        Member member = service.addMember("Avery Reader", "avery@example.com", "");
        verifyRecordObjectMethods(firstBook, member);
        expectLibraryError(() -> service.addBook("Duplicate", "Author", firstBook.getIsbn(), "Fiction", 1),
                "Duplicate ISBNs must be rejected.");
        expectLibraryError(() -> service.addMember("Other Reader", "AVERY@example.com", ""),
                "Duplicate member emails must be rejected.");

        Loan firstLoan = service.checkoutBook(firstBook.getId(), member.getId());
        check(service.getBook(firstBook.getId()).getAvailableCopies() == 1,
                "Checkout should decrement the available copy count.");
        Loan secondLoan = service.checkoutBook(secondBook.getId(), member.getId());
        Loan thirdLoan = service.checkoutBook(thirdBook.getId(), member.getId());
        expectLibraryError(() -> service.checkoutBook(fourthBook.getId(), member.getId()),
                "A member may not exceed three active loans.");
        expectLibraryError(() -> service.deleteBook(firstBook.getId()),
                "Books with loan history cannot be deleted.");
        expectLibraryError(() -> service.deleteMember(member.getId()),
                "Members with active loans cannot be removed from the directory.");
        expectLibraryError(() -> service.setMemberActive(member.getId(), false),
                "Members with active loans cannot be deactivated.");

        service.returnBook(firstLoan.getId());
        check(service.getBook(firstBook.getId()).getAvailableCopies() == 2,
                "Returning should restore the available copy count.");
        expectLibraryError(() -> service.returnBook(firstLoan.getId()),
                "A completed loan cannot be returned twice.");
        service.returnBook(secondLoan.getId());
        service.returnBook(thirdLoan.getId());
        service.setMemberActive(member.getId(), false);
        expectLibraryError(() -> service.checkoutBook(firstBook.getId(), member.getId()),
                "Inactive members cannot check out books.");
        service.deleteMember(member.getId());
        check(service.getMembers().isEmpty(),
                "A member with only returned loans should be removed from the directory.");
        check("Avery Reader".equals(service.findMemberById(member.getId()).getFullName()),
                "Removing a member should preserve their name for loan history.");
        Member replacement = service.addMember("New Reader", "avery@example.com", "");
        check(replacement != null, "An archived member's email may be reused for a new reader.");

        LibraryService reopened = new LibraryService(new FileLibraryRepository(dataFile));
        check(reopened.getBooks().size() == 4, "Books should persist between application launches.");
        check(reopened.getMembers().size() == 1 && "New Reader".equals(reopened.getMembers().get(0).getFullName()),
                "Active directory entries should persist while archived members stay hidden.");
        check("Avery Reader".equals(reopened.findMemberById(member.getId()).getFullName()),
                "Archived member details should persist for existing loan history.");
        check(reopened.getLoans().size() == 3, "Loan history should persist between application launches.");
        List<String> displayNames = reopened.getRecords().stream()
                .map(LibraryRecord::getDisplayName).toList();
        check(displayNames.contains("New Reader") && displayNames.contains("The Hobbit")
                        && !displayNames.contains("Avery Reader"),
                "Record queries should include visible members and retain subtype display names.");
        check(reopened.searchBooks("hobbit").size() == 1, "Catalog search should be case-insensitive.");
        check(reopened.getActiveLoanCount() == 0, "Returned loans should not count as active.");
        check(reopened.getActiveMemberCount() == 1, "Active members should be included in the dashboard.");
        Book detachedCopy = reopened.findBookById(firstBook.getId());
        detachedCopy.checkOutCopy();
        check(reopened.findBookById(firstBook.getId()).getAvailableCopies() == 2,
                "Callers must not be able to mutate stored books through query results.");
    }

    private static void verifyRecordObjectMethods(Book book, Member member) {
        Book sameBook = book.copy();
        check(book.equals(sameBook) && sameBook.equals(book),
                "Copies of the same record should compare equal in both directions.");
        check(book.hashCode() == sameBook.hashCode(),
                "Equal records must have the same hash code.");
        check(!book.equals(new Book("different-id", book.getTitle(), book.getAuthor(),
                        book.getIsbn(), book.getCategory(), book.getTotalCopies())),
                "Different record IDs must not compare equal.");
        check(!book.equals(new Member(book.getId(), member.getFullName(), member.getEmail(), member.getPhone())),
                "Different record types must not compare equal even when their IDs match.");
        check(book.toString().contains("Book") && book.toString().contains(book.getTitle()),
                "A book's string representation should include its type and display name.");
        check(member.toString().contains("Member") && member.toString().contains(member.getFullName()),
                "A member's string representation should include its type and display name.");
    }

    private static void verifyPersistenceFailureDoesNotChangeState() throws Exception {
        LibraryRepository failingRepository = new LibraryRepository() {
            @Override
            public LibraryDataStore load() {
                return new LibraryDataStore();
            }

            @Override
            public void save(LibraryDataStore data) throws IOException {
                throw new IOException("Simulated write failure");
            }
        };
        LibraryService service = new LibraryService(failingRepository);
        expectLibraryError(() -> service.addBook("Unsaved book", "Author",
                "9780061120084", "Fiction", 1), "Persistence errors must be reported.");
        check(service.getBooks().isEmpty(), "A failed save must not alter the in-memory catalog.");
    }

    private static void expectLibraryError(CheckedAction action, String message) throws Exception {
        try {
            action.run();
        } catch (LibraryException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws LibraryException, IOException;
    }
}
