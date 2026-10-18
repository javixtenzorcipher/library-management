package com.folio.library;

import com.folio.library.service.LibraryException;
import com.folio.library.service.LibraryService;
import com.folio.library.storage.FileLibraryRepository;
import com.folio.library.ui.LibraryAppFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Path dataFile = Path.of(System.getProperty("user.home"), ".library-management", "library-data.ser");
            LibraryService service = new LibraryService(new FileLibraryRepository(dataFile));
            service.seedDemoDataIfEmpty();
            new LibraryAppFrame(service).setVisible(true);
        } catch (IOException | LibraryException | ReflectiveOperationException
                 | javax.swing.UnsupportedLookAndFeelException exception) {
            JOptionPane.showMessageDialog(null,
                    "The Library Desk could not start.\n" + exception.getMessage(),
                    "Startup error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
