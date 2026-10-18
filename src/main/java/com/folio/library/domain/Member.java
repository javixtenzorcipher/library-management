package com.folio.library.domain;

import java.time.LocalDateTime;

public final class Member extends LibraryRecord {
    private static final long serialVersionUID = 1L;

    private String fullName;
    private String email;
    private String phone;
    private boolean active;
    private boolean archived;

    public Member(String id, String fullName, String email, String phone) {
        super(id);
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.active = true;
    }

    public Member(String id, String fullName, String email, String phone, boolean active,
                  LocalDateTime createdAt) {
        this(id, fullName, email, phone, active, false, createdAt);
    }

    public Member(String id, String fullName, String email, String phone, boolean active, boolean archived,
                  LocalDateTime createdAt) {
        super(id, createdAt);
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.active = active && !archived;
        this.archived = archived;
    }

    public Member copy() {
        return new Member(getId(), fullName, email, phone, active, archived, getCreatedAt());
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isArchived() {
        return archived;
    }

    public void updateDetails(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public void setActive(boolean active) {
        if (archived && active) {
            throw new IllegalStateException("An archived member cannot be activated.");
        }
        this.active = active;
    }

    public void archive() {
        active = false;
        archived = true;
    }

    @Override
    public String getDisplayName() {
        return fullName;
    }
}
