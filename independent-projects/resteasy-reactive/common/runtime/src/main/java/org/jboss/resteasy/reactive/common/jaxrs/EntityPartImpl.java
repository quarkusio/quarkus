package org.jboss.resteasy.reactive.common.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import org.jboss.resteasy.reactive.common.core.Serialisers;

public class EntityPartImpl implements EntityPart {

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    private final String name;
    private final String fileName;
    private final MultivaluedMap<String, String> headers;
    private final MediaType mediaType;
    private final InputStream content;
    private final Serialisers serialisers;
    private final AtomicBoolean contentConsumed = new AtomicBoolean(false);

    public EntityPartImpl(String name, String fileName, MultivaluedMap<String, String> headers,
            MediaType mediaType, InputStream content, Serialisers serialisers) {
        this.name = name;
        this.fileName = fileName;
        this.headers = headers;
        this.mediaType = mediaType;
        this.content = content;
        this.serialisers = serialisers;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<String> getFileName() {
        return Optional.ofNullable(fileName);
    }

    @Override
    public InputStream getContent() {
        if (!contentConsumed.compareAndSet(false, true)) {
            throw new IllegalStateException("getContent() has already been invoked");
        }
        return content;
    }

    @Override
    public <T> T getContent(Class<T> type)
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        if (!contentConsumed.compareAndSet(false, true)) {
            throw new IllegalStateException("getContent() has already been invoked");
        }
        return readContent(type, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getContent(GenericType<T> type)
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        if (!contentConsumed.compareAndSet(false, true)) {
            throw new IllegalStateException("getContent() has already been invoked");
        }
        return readContent((Class<T>) type.getRawType(), type.getType());
    }

    @SuppressWarnings("unchecked")
    private <T> T readContent(Class<T> rawType, Type genericType) throws IOException, WebApplicationException {
        if (rawType == InputStream.class) {
            return rawType.cast(content);
        }
        if (rawType == String.class) {
            return rawType.cast(new String(content.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
        if (rawType == byte[].class) {
            return rawType.cast(content.readAllBytes());
        }
        if (serialisers != null) {
            List<MessageBodyReader<?>> readers = serialisers.findReaders(null, rawType, mediaType);
            for (MessageBodyReader<?> r : readers) {
                if (r.isReadable(rawType, genericType, EMPTY_ANNOTATIONS, mediaType)) {
                    MessageBodyReader<T> reader = (MessageBodyReader<T>) r;
                    return reader.readFrom(rawType, genericType, EMPTY_ANNOTATIONS, mediaType, headers, content);
                }
            }
        }
        throw new IllegalArgumentException(
                "Unsupported type: " + rawType.getName() + ". Use InputStream, String, or byte[].");
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    public static boolean isEntityPartList(Type type) {
        if (type instanceof ParameterizedType pt) {
            if (pt.getRawType() == List.class) {
                Type[] args = pt.getActualTypeArguments();
                return args.length == 1 && args[0] == EntityPart.class;
            }
        }
        return false;
    }

    public static String buildContentDisposition(String name, String fileName) {
        StringBuilder cd = new StringBuilder("form-data; name=\"").append(escapeQuotes(name)).append("\"");
        if (fileName != null) {
            cd.append("; filename=\"").append(escapeQuotes(fileName)).append("\"");
        }
        return cd.toString();
    }

    private static String escapeQuotes(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
