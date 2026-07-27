package org.jboss.resteasy.reactive.common.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

public class EntityPartImpl implements EntityPart {

    private final String name;
    private final String fileName;
    private final MultivaluedMap<String, String> headers;
    private final MediaType mediaType;
    private final InputStream content;
    private final AtomicBoolean contentConsumed = new AtomicBoolean(false);

    public EntityPartImpl(String name, String fileName, MultivaluedMap<String, String> headers,
            MediaType mediaType, InputStream content) {
        this.name = name;
        this.fileName = fileName;
        this.headers = headers;
        this.mediaType = mediaType;
        this.content = content;
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
        if (type == InputStream.class) {
            return type.cast(content);
        }
        if (type == String.class) {
            return type.cast(new String(content.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
        if (type == byte[].class) {
            return type.cast(content.readAllBytes());
        }
        throw new IllegalArgumentException(
                "Unsupported type: " + type.getName() + ". Use InputStream, String, or byte[].");
    }

    @Override
    public <T> T getContent(GenericType<T> type)
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        return getContent((Class<T>) type.getRawType());
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }
}
