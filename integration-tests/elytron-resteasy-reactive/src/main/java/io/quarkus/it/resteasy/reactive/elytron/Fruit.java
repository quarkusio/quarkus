package io.quarkus.it.resteasy.reactive.elytron;

import java.util.Objects;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class Fruit {

    static final Fruit APPLE = new Fruit(1, "Apple", "Winter fruit");
    static final Fruit PINEAPPLE = new Fruit(2, "Pineapple", "Tropical fruit");

    private Integer id;
    private String name;
    private String description;

    public Fruit() {
    }

    public Fruit(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @SecureField(rolesAllowed = { "managers", "employees" })
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @SecureField(rolesAllowed = "managers")
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Fruit fruit = (Fruit) o;
        return Objects.equals(id, fruit.id) && Objects.equals(name, fruit.name)
                && Objects.equals(description, fruit.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description);
    }

    @Override
    public String toString() {
        return "Fruit{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
