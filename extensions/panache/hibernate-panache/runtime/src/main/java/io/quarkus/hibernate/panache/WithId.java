package io.quarkus.hibernate.panache;

import java.util.UUID;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class WithId<IdType> {
    /**
     * The auto-generated ID field. This field is set by Hibernate ORM when this entity
     * is persisted.
     */
    @Id
    @GeneratedValue
    public IdType id;

    @Override
    public java.lang.String toString() {
        return this.getClass().getSimpleName() + "<" + id + ">";
    }

    public abstract static class AutoLong extends WithId<java.lang.Long> {
    }

    public abstract static class AutoString extends WithId<java.lang.String> {
    }

    public abstract static class AutoUUID extends WithId<java.util.UUID> {
    }

}
