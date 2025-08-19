package com.studycoAchl.hackaton.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateSubject {
    private UUID uuid;
    private String name;
    private LocalDateTime createdAt;

    public CreateSubject() {}

    public CreateSubject(UUID uuid, String name, LocalDateTime createdAt) {
        this.uuid = uuid;
        this.name = name;
        this.createdAt = createdAt;
    }

    // getters and setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
