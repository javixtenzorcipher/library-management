package com.folio.library.ui;

import com.folio.library.service.LibraryService;
import com.folio.library.storage.FileLibraryRepository;

import javax.imageio.ImageIO;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class LibraryScreenshotGenerator {
    private LibraryScreenshotGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Screenshots require a graphical desktop session.");
        }
        Path screenshotDirectory = args.length == 0
                ? Path.of("screenshots")
                : Path.of(args[0]);
        Files.createDirectories(screenshotDirectory);
        Path temporaryHome = Files.createTempDirectory("library-screenshots-");
        LibraryAppFrame[] frame = new LibraryAppFrame[1];
        try {
            System.setProperty("user.home", temporaryHome.toString());
            LibraryService service = new LibraryService(new FileLibraryRepository(
                    temporaryHome.resolve(".library-management").resolve("library-data.ser")));
            var effectiveJava = service.addBook("Effective Java", "Joshua Bloch", "9780134685991",
                    "Java", 3);
            service.addBook("Head First Java", "Kathy Sierra", "9781491910771", "Java", 3);
            var javaSwing = service.addBook("Java Swing",
                    "Marc Loy, James Elliott, David Wood, Brian Cole", "9780596004088", "GUI", 2);
            service.addBook("Object-Oriented Thought Process", "Matt Weisfeld", "9780135181966",
                    "OOP", 2);
            var atomicHabits = service.addBook("Atomic Habits", "James Clear", "9780735211292",
                    "Personal development", 3);
            var jordan = service.addMember("Jordan Lee", "jordan.lee@example.com", "09170000001");
            var casey = service.addMember("Casey Morgan", "casey.morgan@example.com", "09170000002");
            var ethan = service.addMember("Ethan Carter", "ethan.carter@example.com", "09170000003");
            var returnedLoan = service.checkoutBook(atomicHabits.getId(), ethan.getId());
            service.returnBook(returnedLoan.getId());
            service.deleteMember(ethan.getId());
            service.checkoutBook(effectiveJava.getId(), jordan.getId());
            service.checkoutBook(javaSwing.getId(), casey.getId());

            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new LibraryAppFrame(service);
                frame[0].setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
                frame[0].setLocationRelativeTo(null);
                frame[0].setVisible(true);
            });

            capture(frame[0], screenshotDirectory.resolve("01-dashboard.png"));
            capturePage(frame[0], "books", screenshotDirectory.resolve("02-books.png"));
            SwingUtilities.invokeAndWait(() -> {
                if (!setSearchQuery(frame[0].getContentPane(),
                        "Search title, author, ISBN, or category", "Java")) {
                    throw new IllegalStateException("Could not find the book catalog search field.");
                }
            });
            capture(frame[0], screenshotDirectory.resolve("03-book-search.png"));
            capturePage(frame[0], "members", screenshotDirectory.resolve("04-members.png"));
            capturePage(frame[0], "loans", screenshotDirectory.resolve("05-loans.png"));
        } finally {
            if (frame[0] != null) {
                SwingUtilities.invokeAndWait(frame[0]::dispose);
            }
            Path dataDirectory = temporaryHome.resolve(".library-management");
            if (Files.exists(dataDirectory)) {
                try (var paths = Files.walk(dataDirectory)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(path);
                    }
                }
            }
            Files.deleteIfExists(temporaryHome);
        }
    }

    private static void capturePage(LibraryAppFrame frame, String page, Path destination) throws Exception {
        SwingUtilities.invokeAndWait(() -> frame.showPage(page));
        Thread.sleep(300);
        capture(frame, destination);
    }

    private static boolean setSearchQuery(Container container, String accessibleName, String query) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTextField textField
                    && accessibleName.equals(textField.getAccessibleContext().getAccessibleName())) {
                textField.setText(query);
                return true;
            }
            if (component instanceof Container child
                    && setSearchQuery(child, accessibleName, query)) {
                return true;
            }
        }
        return false;
    }

    private static void capture(LibraryAppFrame frame, Path destination) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                frame.printAll(graphics);
                ImageIO.write(image, "png", destination.toFile());
            } catch (IOException exception) {
                throw new java.io.UncheckedIOException(exception);
            } finally {
                graphics.dispose();
            }
        });
    }
}
