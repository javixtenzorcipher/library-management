package com.folio.library.storage;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileLibraryRepository implements LibraryRepository {
    private final Path dataFile;

    public FileLibraryRepository(Path dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    public LibraryDataStore load() throws IOException {
        if (Files.notExists(dataFile)) {
            return new LibraryDataStore();
        }

        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(dataFile))) {
            input.setObjectInputFilter(FileLibraryRepository::filterSerializedClass);
            Object data = input.readObject();
            if (!(data instanceof LibraryDataStore store)) {
                throw new IOException("The library data file has an unsupported format.");
            }
            return store;
        } catch (ClassNotFoundException exception) {
            throw new IOException("A class needed to read the library data file is missing.", exception);
        }
    }

    @Override
    public void save(LibraryDataStore data) throws IOException {
        Path parent = dataFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "library-", ".tmp");
        try {
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporaryFile))) {
                output.writeObject(data);
            }
            try {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static ObjectInputFilter.Status filterSerializedClass(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 20 || info.references() > 100_000 || info.arrayLength() > 100_000) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        if (type.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        if (type.isArray()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        String name = type.getName();
        boolean allowed = name.equals(LibraryDataStore.class.getName())
                || name.startsWith("com.folio.library.domain.")
                || name.equals("java.util.HashMap")
                || name.equals("java.util.LinkedHashMap")
                || name.equals("java.time.Ser")
                || name.equals("java.time.LocalDate")
                || name.equals("java.time.LocalDateTime")
                || name.equals("java.lang.Object")
                || name.equals("java.lang.String");
        return allowed ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
    }
}
