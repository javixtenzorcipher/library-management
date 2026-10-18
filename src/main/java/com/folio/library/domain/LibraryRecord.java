package com.folio.library.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class LibraryRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final LocalDateTime createdAt;

    protected LibraryRecord(String id) {
        this(id, LocalDateTime.now());
    }

    protected LibraryRecord(String id, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public final String getId() {
        return id;
    }

    public final LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public abstract String getDisplayName();

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', displayName='" + getDisplayName() + "'}";
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        LibraryRecord record = (LibraryRecord) other;
        return id.equals(record.id);
    }

    @Override
    public final int hashCode() {
        return 31 * getClass().hashCode() + id.hashCode();
    }
}
