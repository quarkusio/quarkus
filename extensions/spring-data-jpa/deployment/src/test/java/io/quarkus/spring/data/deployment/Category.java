package io.quarkus.spring.data.deployment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Entity where a field name matches the entity class name (case-insensitive).
 * This reproduces the scenario from https://github.com/quarkusio/quarkus/issues/48032
 */
@Entity
public class Category {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String category;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
