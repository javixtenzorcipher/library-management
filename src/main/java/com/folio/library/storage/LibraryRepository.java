package com.folio.library.storage;

import java.io.IOException;

public interface LibraryRepository {
    LibraryDataStore load() throws IOException;

    void save(LibraryDataStore data) throws IOException;
}
