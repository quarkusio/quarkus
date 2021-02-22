package io.quarkus.hibernate.orm;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
public class MyEntity {
    public static final String ENTITY_NAME_TOO_LONG = "entity name too long";
    public static final String ENTITY_NAME_CANNOT_BE_EMPTY = "entity name cannot be empty";
    private long id;

    @NotNull
    @NotEmpty(message = ENTITY_NAME_CANNOT_BE_EMPTY)
    @Size(max = 50, message = ENTITY_NAME_TOO_LONG)
    private String name;

    public MyEntity() {
    }

    public MyEntity(String name) {
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
        return "MyEntity:" + name;
    }
}
