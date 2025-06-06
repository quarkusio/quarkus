package io.quarkus.it.jpa.postgresql.defaultpu;

import java.time.LocalDate;

import jakarta.persistence.Embeddable;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Embeddable
public class SomeEmbeddable {

    public int someNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    public ToBeSerializedWithDateTime someOtherJson;

    public SomeEmbeddable() {
    }

    public SomeEmbeddable(int someNumber, LocalDate date) {
        this.someNumber = someNumber;
        this.someOtherJson = new ToBeSerializedWithDateTime(date);
    }

    @RegisterForReflection
    public static class ToBeSerializedWithDateTime {
        @JsonProperty
        LocalDate date;

        @JsonProperty
        int year;

        public ToBeSerializedWithDateTime() {
        }

        public ToBeSerializedWithDateTime(LocalDate date) {
            this.date = date;
            this.year = date.getYear();
        }

        @Override
        public String toString() {
            return "ToBeSerializedWithDateTime{" +
                    "date=" + date +
                    '}';
        }
    }
}
