
package io.quarkus.it.jpa.postgresql.otherpu;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Entity
public class EntityWithJsonOtherPU {
    @Id
    @GeneratedValue
    Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    ToBeSerializedWithDateTime json;

    public EntityWithJsonOtherPU() {
    }

    public EntityWithJsonOtherPU(ToBeSerializedWithDateTime data) {
        this.json = data;
    }

    @Override
    public String toString() {
        return "EntityWithJsonOtherPU{" +
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
