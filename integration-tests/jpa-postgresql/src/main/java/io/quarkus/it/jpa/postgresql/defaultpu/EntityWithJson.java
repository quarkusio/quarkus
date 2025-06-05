package io.quarkus.it.jpa.postgresql.defaultpu;

import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Entity
public class EntityWithJson {
    @Id
    @GeneratedValue
    Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    ToBeSerializedWithDateTime json;

    @Embedded
    SomeEmbeddable someEmbeddable;

    @ElementCollection
    Set<SomeEmbeddable> collection;

    public EntityWithJson() {
    }

    public EntityWithJson(ToBeSerializedWithDateTime data,
            SomeEmbeddable someEmbeddable) {
        this.json = data;
        this.someEmbeddable = someEmbeddable;
        this.collection = Set.of(someEmbeddable);
    }

    @Override
    public String toString() {
        return "EntityWithJson{" +
                "id=" + id +
                ", json=" + json +
                '}';
    }

    @RegisterForReflection
    public static class ToBeSerializedWithDateTime {
        @JsonProperty
        LocalDate date;

        public ToBeSerializedWithDateTime() {
        }

        public ToBeSerializedWithDateTime(LocalDate date) {
            this.date = date;
        }

        @Override
        public String toString() {
            return "ToBeSerializedWithDateTime{" +
                    "date=" + date +
                    '}';
        }
    }
}
