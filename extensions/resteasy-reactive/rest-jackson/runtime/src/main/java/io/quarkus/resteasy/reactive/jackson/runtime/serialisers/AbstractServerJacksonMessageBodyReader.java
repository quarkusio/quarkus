package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.common.util.EmptyInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.arc.impl.LazyValue;

public abstract class AbstractServerJacksonMessageBodyReader extends AbstractJsonMessageBodyReader {

    protected final LazyValue<ObjectReader> defaultReader;

    // used by Arc
    protected AbstractServerJacksonMessageBodyReader() {
        defaultReader = null;
    }

    public AbstractServerJacksonMessageBodyReader(Instance<ObjectMapper> mapper) {
        this.defaultReader = new LazyValue<>(new Supplier<>() {
            @Override
            public ObjectReader get() {
                return mapper.get().reader();
            }
        });
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
            WebApplicationException {
        return doReadFrom(type, genericType, entityStream);
    }

    protected ObjectReader getEffectiveReader() {
        return defaultReader.get();
    }

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) throws IOException {
        if (entityStream instanceof EmptyInputStream) {
            return null;
        }
        ObjectReader reader = getEffectiveReader();
        return reader.forType(reader.getTypeFactory().constructType(genericType != null ? genericType : type))
                .readValue(entityStream);
    }
}
