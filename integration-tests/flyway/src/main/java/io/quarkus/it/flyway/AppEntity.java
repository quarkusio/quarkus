package io.quarkus.it.flyway;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity used within tests
 */
@Entity
@Table(name = "quarkus_table2", schema = "TEST_SCHEMA")
public class AppEntity {

    @Id
    private int id;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final AppEntity appEntity = (AppEntity) o;
        return id == appEntity.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
