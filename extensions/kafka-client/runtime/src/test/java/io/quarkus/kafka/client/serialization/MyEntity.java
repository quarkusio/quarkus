package io.quarkus.kafka.client.serialization;

import java.util.Objects;

public class MyEntity {
    public long id;
    public String name;

    // used by deserializers
    public MyEntity() {
    }

    public MyEntity(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MyEntity myEntity = (MyEntity) o;
        return id == myEntity.id && Objects.equals(name, myEntity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
