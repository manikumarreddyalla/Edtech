package com.collegeops.training;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "training_skills")
public class TrainingSkill {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
