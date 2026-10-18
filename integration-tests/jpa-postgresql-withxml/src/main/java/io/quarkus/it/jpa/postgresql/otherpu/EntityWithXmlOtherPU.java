
package io.quarkus.it.jpa.postgresql.otherpu;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Entity
public class EntityWithXmlOtherPU {
    @Id
    @GeneratedValue
    Long id;

    @JdbcTypeCode(SqlTypes.SQLXML)
    ToBeSerializedWithDateTime xml;

    public EntityWithXmlOtherPU() {
    }

    public EntityWithXmlOtherPU(ToBeSerializedWithDateTime data) {
        this.xml = data;
    }

    @Override
    public String toString() {
        return "EntityWithXmlOtherPU{" +
                "id=" + id +
                ", xml" + xml +
                '}';
    }

    @RegisterForReflection
    public static class ToBeSerializedWithDateTime {
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
