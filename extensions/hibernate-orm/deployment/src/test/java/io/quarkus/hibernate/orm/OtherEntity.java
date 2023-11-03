package io.quarkus.hibernate.orm;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class OtherEntity {
    public static final String ENTITY_NAME_TOO_LONG = "entity name too long";
    public static final String ENTITY_NAME_CANNOT_BE_EMPTY = "entity name cannot be empty";
    private long id;

    @NotNull
    @NotEmpty(message = ENTITY_NAME_CANNOT_BE_EMPTY)
    @Size(max = 50, message = ENTITY_NAME_TOO_LONG)
    private String name;

    public OtherEntity() {
    }

    public OtherEntity(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "myEntitySeq")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "OtherEntity:" + name;
    }
}
