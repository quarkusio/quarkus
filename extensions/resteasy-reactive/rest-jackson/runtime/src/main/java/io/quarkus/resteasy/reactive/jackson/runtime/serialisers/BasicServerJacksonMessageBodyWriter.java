package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.doLegacyWrite;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.arc.impl.LazyValue;
import io.quarkus.resteasy.reactive.jackson.runtime.mappers.JacksonMapperUtil;

public class BasicServerJacksonMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private final LazyValue<ObjectWriter> defaultWriter;
    private final Map<JavaType, ObjectWriter> genericWriters = new ConcurrentHashMap<>();

    // used by Arc
    public BasicServerJacksonMessageBodyWriter() {
        defaultWriter = null;
    }

    @Inject
    public BasicServerJacksonMessageBodyWriter(Instance<ObjectMapper> mapper) {
        this.defaultWriter = new LazyValue<>(new Supplier<>() {
            @Override
            public ObjectWriter get() {
                return createDefaultWriter(mapper.get());
            }
        });
    }

    private ObjectWriter getWriter(Type genericType, Object value) {
        // make sure we properly handle polymorphism in generic collections
        if (value != null && genericType != null) {
            JavaType rootType = JacksonMapperUtil.getGenericRootType(genericType, defaultWriter.get());
            // Check that the determined root type is really assignable from the given entity.
            // A mismatch can happen, if a ServerResponseFilter replaces the response entity with another object
            // that does not match the original signature of the method (see HalServerResponseFilter for an example)
            if (rootType != null && rootType.isTypeOrSuperTypeOf(value.getClass())) {
                ObjectWriter writer = genericWriters.get(rootType);
                if (writer == null) {
                    // No cached writer for that type. Compute it once.
                    writer = genericWriters.computeIfAbsent(rootType, new Function<>() {
                        @Override
                        public ObjectWriter apply(JavaType type) {
                            return defaultWriter.get().forType(type);
                        }
                    });
                }
                return writer;
            }
        }

        // no generic type given, or the generic type is just a class. Use the default writer.
        return this.defaultWriter.get();
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        OutputStream stream = context.getOrCreateOutputStream();
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes(StandardCharsets.UTF_8));
        } else {
            getWriter(genericType, o).writeValue(stream, o);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        stream.close();
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, getWriter(genericType, o));
    }

}
