package com.folio.library.ui;

import com.folio.library.service.LibraryService;
import com.folio.library.storage.FileLibraryRepository;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

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
            service.seedDemoDataIfEmpty();
            service.checkoutBook("BK-1001", "MB-1001");
            service.checkoutBook("BK-1002", "MB-1002");

            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new LibraryAppFrame(service);
                frame[0].setSize(1180, 780);
                frame[0].setLocationRelativeTo(null);
                frame[0].setVisible(true);
            });

            capture(frame[0], screenshotDirectory.resolve("01-dashboard.png"));
            List<String> pages = List.of("books", "members", "loans");
            for (int index = 0; index < pages.size(); index++) {
                String page = pages.get(index);
                SwingUtilities.invokeAndWait(() -> frame[0].showPage(page));
                Thread.sleep(400);
                capture(frame[0], screenshotDirectory.resolve(String.format("%02d-%s.png", index + 2, page)));
            }
            SwingUtilities.invokeAndWait(() -> {
                frame[0].showPage("dashboard");
                frame[0].setSize(860, 700);
                frame[0].validate();
            });
            Thread.sleep(300);
            capture(frame[0], screenshotDirectory.resolve("05-dashboard-compact.png"));
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
