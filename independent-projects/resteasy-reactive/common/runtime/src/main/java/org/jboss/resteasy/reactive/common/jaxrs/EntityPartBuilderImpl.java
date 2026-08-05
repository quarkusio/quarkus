package org.jboss.resteasy.reactive.common.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

public class EntityPartBuilderImpl implements EntityPart.Builder {

    private final String name;
    private String fileName;
    private MediaType mediaType;
    private MultivaluedMap<String, String> headers;
    private InputStream content;

    public EntityPartBuilderImpl(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;
        this.mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        this.headers = new QuarkusMultivaluedHashMap<>();
    }

    @Override
    public EntityPart.Builder mediaType(MediaType mediaType) throws IllegalArgumentException {
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType must not be null");
        }
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public EntityPart.Builder mediaType(String mediaTypeString) throws IllegalArgumentException {
        if (mediaTypeString == null) {
            throw new IllegalArgumentException("mediaType must not be null");
        }
        this.mediaType = MediaType.valueOf(mediaTypeString);
        return this;
    }

    @Override
    public EntityPart.Builder header(String headerName, String... headerValues) throws IllegalArgumentException {
        if (headerName == null) {
            throw new IllegalArgumentException("headerName must not be null");
        }
        if (headerValues == null) {
            throw new IllegalArgumentException("headerValues must not be null");
        }
        for (String value : headerValues) {
            headers.add(headerName, value);
        }
        return this;
    }

    @Override
    public EntityPart.Builder headers(MultivaluedMap<String, String> newHeaders) throws IllegalArgumentException {
        if (newHeaders == null) {
            throw new IllegalArgumentException("newHeaders must not be null");
        }
        this.headers = new QuarkusMultivaluedHashMap<>();
        this.headers.putAll(newHeaders);
        return this;
    }

    @Override
    public EntityPart.Builder fileName(String fileName) throws IllegalArgumentException {
        this.fileName = fileName;
        return this;
    }

    @Override
    public EntityPart.Builder content(InputStream content) throws IllegalArgumentException {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        this.content = content;
        return this;
    }

    @Override
    public <T> EntityPart.Builder content(T content, Class<? extends T> type) throws IllegalArgumentException {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (content instanceof InputStream) {
            this.content = (InputStream) content;
        } else if (content instanceof String) {
            if (mediaType == MediaType.APPLICATION_OCTET_STREAM_TYPE) {
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            }
            this.content = new ByteArrayInputStream(((String) content).getBytes(StandardCharsets.UTF_8));
        } else if (content instanceof byte[]) {
            this.content = new ByteArrayInputStream((byte[]) content);
        } else {
            this.content = new ByteArrayInputStream(content.toString().getBytes(StandardCharsets.UTF_8));
        }
        return this;
    }

    @Override
    public <T> EntityPart.Builder content(T content, GenericType<T> type) throws IllegalArgumentException {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return content(content, (Class<T>) type.getRawType());
    }

    @Override
    public EntityPart build() throws IllegalStateException, IOException, WebApplicationException {
        if (content == null) {
            throw new IllegalStateException("No content has been set");
        }

        MultivaluedMap<String, String> builtHeaders = new QuarkusMultivaluedHashMap<>();
        builtHeaders.putAll(headers);

        builtHeaders.putSingle("Content-Type", mediaType.toString());

        StringBuilder cd = new StringBuilder("form-data; name=\"").append(name).append("\"");
        if (fileName != null) {
            cd.append("; filename=\"").append(fileName).append("\"");
        }
        builtHeaders.putSingle("Content-Disposition", cd.toString());

        return new EntityPartImpl(name, fileName, builtHeaders, mediaType, content);
    }
}
