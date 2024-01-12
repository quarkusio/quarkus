package io.quarkus.hibernate.orm.runtime.customized;

import jakarta.json.bind.Jsonb;

import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.hibernate.type.format.jakartajson.JsonBJsonFormatMapper;
import org.hibernate.type.format.jaxb.JaxbXmlFormatMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;

public enum FormatMapperKind {
    JACKSON {
        @Override
        public FormatMapper create() {
            // NOTE: we are not creating a Jackson based XML mapper since that one
            // requires an additional lib (jackson-dataformat-xml-2.15.2) being available
            // as well as an XmlMapper instance instead of an ObjectMapper...
            return new JacksonJsonFormatMapper(Arc.container().instance(ObjectMapper.class).get());
        }
    },
    JSONB {
        @Override
        public FormatMapper create() {
            return new JsonBJsonFormatMapper(Arc.container().instance(Jsonb.class).get());
        }
    },
    JAXB {
        @Override
        public FormatMapper create() {
            return new JaxbXmlFormatMapper();
        }
    };

    public abstract FormatMapper create();
}
