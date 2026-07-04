package com.recommender.db.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UserEntity() {}

    public UserEntity(String name) {
        this.name = name;
    }

    public UUID    getId()                { return id; }
    public void    setId(UUID id)         { this.id = id; }

    public String  getName()              { return name; }
    public void    setName(String name)   { this.name = name; }

    public Instant getCreatedAt()         { return createdAt; }
}
