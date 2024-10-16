package io.quarkus.it.elasticsearch.java;

import java.util.Objects;

public class Fruit {
    public String id;
    public String name;
    public String color;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Fruit fruit))
            return false;
        return Objects.equals(id, fruit.id) && Objects.equals(name, fruit.name) && Objects.equals(color, fruit.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color);
    }
}
