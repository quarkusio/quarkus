package io.quarkus.hibernate.panache;

import java.util.UUID;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.UuidGenerator;

// This is now a dumb placeholder because @GeneratedValue only works with Long
@MappedSuperclass
public abstract class WithId<Identifier> {

    @Id
    public Identifier id;

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "<" + id + ">";
    }

    @MappedSuperclass
    public abstract static class AutoLong {
        @Id
        @GeneratedValue
        public Long id;

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "<" + id + ">";
        }
    }

    @MappedSuperclass
    public abstract static class AutoString {
        @Id
        @UuidGenerator
        public String id;

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "<" + id + ">";
        }
    }

    @MappedSuperclass
    public abstract static class AutoUUID {
        @Id
        @UuidGenerator
        public UUID id;

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "<" + id + ">";
        }
    }

}
