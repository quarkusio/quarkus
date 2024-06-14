package org.jboss.resteasy.reactive.server.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;

import org.jboss.resteasy.reactive.common.util.UnmodifiableMultivaluedMap;

public class EntityPartImpl implements EntityPart {
    private final String name;
    private final MultivaluedMap<String, String> headers;
    private final MediaType mediaType;
    private final String fileName;
    private final InputStream content;

    public EntityPartImpl(FormValue value, String controlName) {
        this.name = controlName;
        this.headers = new UnmodifiableMultivaluedMap(value.getHeaders());
        this.mediaType = MediaType.valueOf(value.getHeaders().getFirst("Content-Type"));
        this.fileName = value.getFileName();
        this.content = null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Optional<String> getFileName() {
        return Optional.ofNullable(this.fileName);
    }

    @Override
    public InputStream getContent() {
        return null;
    }

    @Override
    public <T> T getContent(Class<T> type)
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        return null;
    }

    @Override
    public <T> T getContent(GenericType<T> type)
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return this.headers;
    }

    @Override
    public MediaType getMediaType() {
        return this.mediaType;
    }
}
