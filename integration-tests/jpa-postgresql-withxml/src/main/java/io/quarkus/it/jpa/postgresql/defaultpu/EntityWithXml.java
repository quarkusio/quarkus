package io.quarkus.it.jpa.postgresql.defaultpu;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Entity
public class EntityWithXml {
    @Id
    @GeneratedValue
    Long id;

    @JdbcTypeCode(SqlTypes.SQLXML)
    ToBeSerializedWithDateTime xml;

    public EntityWithXml() {
    }

    public EntityWithXml(ToBeSerializedWithDateTime data) {
        this.xml = data;
    }

    @Override
    public String toString() {
        return "EntityWithXml{" +
                "id=" + id +
                ", xml=" + xml +
                '}';
    }

    @RegisterForReflection
    @XmlRootElement
    public static class ToBeSerializedWithDateTime {
        @XmlElement
        @XmlJavaTypeAdapter(value = LocalDateXmlAdapter.class)
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

    @RegisterForReflection
    public static class LocalDateXmlAdapter extends XmlAdapter<String, LocalDate> {
        public LocalDate unmarshal(String string) {
            return string == null ? null : LocalDate.parse(string);
        }

        public String marshal(LocalDate localDate) {
            return localDate == null ? null : localDate.toString();
        }
    }
}
