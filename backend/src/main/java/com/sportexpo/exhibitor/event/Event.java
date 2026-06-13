package com.sportexpo.exhibitor.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "name")
    private String name;
    @Column(name = "year")
    private int year;
    @Column(name = "status")
    private String status;
    @Column(name = "default_lock_minutes")
    private int defaultLockMinutes;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected Event() {
    }

    public Event(UUID id, String name, int year, String status, int defaultLockMinutes,
                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.status = status;
        this.defaultLockMinutes = defaultLockMinutes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getYear() { return year; }
    public String getStatus() { return status; }
    public int getDefaultLockMinutes() { return defaultLockMinutes; }
}
