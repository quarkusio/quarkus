package org.jboss.resteasy.reactive.common.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

public class EntityPartBuilderImpl implements EntityPart.Builder {

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    private final String name;
    private String fileName;
    private MediaType mediaType;
    private MultivaluedMap<String, String> headers;
    private Serialisers serialisers;

    private Object content;
    private Class<?> contentType;
    private Type contentGenericType;

    public EntityPartBuilderImpl(String name, Serialisers serialisers) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;
        this.serialisers = serialisers;
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
        this.contentType = null;
        this.contentGenericType = null;
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
        this.content = content;
        this.contentType = type;
        this.contentGenericType = type;
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
        this.content = content;
        this.contentType = type.getRawType();
        this.contentGenericType = type.getType();
        return this;
    }

    @SuppressWarnings("unchecked")
    private <T> InputStream serialiseContent() {
        T content = (T) this.content;
        Class<T> rawType = (Class<T>) contentType;
        Type genericType = contentGenericType;

        if (content instanceof String) {
            if (mediaType == MediaType.APPLICATION_OCTET_STREAM_TYPE) {
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            }
            return new ByteArrayInputStream(((String) content).getBytes(StandardCharsets.UTF_8));
        } else if (content instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) content);
        } else if (serialisers != null) {
            MessageBodyWriter<T> writer = null;
            List<MessageBodyWriter<?>> writers = serialisers.findWriters(null, rawType, mediaType);
            for (MessageBodyWriter<?> w : writers) {
                if (w.isWriteable(rawType, genericType, EMPTY_ANNOTATIONS, mediaType)) {
                    writer = (MessageBodyWriter<T>) w;
                    break;
                }
            }
            if (writer != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writer.writeTo(content, rawType, genericType, EMPTY_ANNOTATIONS, mediaType,
                            new QuarkusMultivaluedHashMap<>(), baos);
                    return new ByteArrayInputStream(baos.toByteArray());
                } catch (IOException e) {
                    throw new IllegalArgumentException("Failed to serialize content of type " + rawType.getName(), e);
                }
            } else {
                return new ByteArrayInputStream(content.toString().getBytes(StandardCharsets.UTF_8));
            }
        } else {
            return new ByteArrayInputStream(content.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public EntityPart build() throws IllegalStateException, IOException, WebApplicationException {
        if (content == null) {
            throw new IllegalStateException("No content has been set");
        }

        InputStream resolvedContent;
        if (content instanceof InputStream) {
            resolvedContent = (InputStream) content;
        } else {
            resolvedContent = serialiseContent();
        }

        MultivaluedMap<String, String> builtHeaders = new QuarkusMultivaluedHashMap<>();
        builtHeaders.putAll(headers);

        builtHeaders.putSingle("Content-Type", mediaType.toString());
        builtHeaders.putSingle("Content-Disposition", EntityPartImpl.buildContentDisposition(name, fileName));

        return new EntityPartImpl(name, fileName, builtHeaders, mediaType, resolvedContent, serialisers);
    }
}
