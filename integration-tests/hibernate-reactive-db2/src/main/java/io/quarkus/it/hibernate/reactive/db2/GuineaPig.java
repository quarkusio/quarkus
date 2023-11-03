package io.quarkus.it.hibernate.reactive.db2;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Pig")
public class GuineaPig {

    @Id
    private Integer id;
    private String name;

    public GuineaPig() {
    }

    public GuineaPig(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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
        return id + ": " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GuineaPig guineaPig = (GuineaPig) o;
        return Objects.equals(name, guineaPig.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
