package org.jboss.resteasy.reactive.server.multipart;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;

import org.jboss.resteasy.reactive.common.util.UnmodifiableMultivaluedMap;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartSupport;

public class EntityPartImpl implements EntityPart {
    private final ResteasyReactiveRequestContext context;
    private final String name;
    private final MultivaluedMap<String, String> headers;
    private final MediaType mediaType;
    private final String fileName;
    private final Content content;

    private boolean hasBeenConverted = false;

    public EntityPartImpl(ResteasyReactiveRequestContext context, FormValue value, String controlName) {
        this.context = context;
        this.name = controlName;
        this.headers = new UnmodifiableMultivaluedMap(value.getHeaders());
        this.mediaType = MediaType.valueOf(value.getHeaders().getFirst("Content-Type"));
        this.fileName = value.getFileName();

        if (value.isFileItem()) {
            this.content = new Content(value.getFileItem().getFile().toFile());
        } else {
            this.content = new Content(value.getValue().getBytes(getCharset()));
        }
    }

    protected EntityPartImpl(ResteasyReactiveRequestContext context, String name, MultivaluedMap<String, String> headers,
            MediaType mediaType, String fileName, Content content) {
        this.context = context;
        this.name = name;
        this.headers = headers;
        this.mediaType = mediaType;
        this.fileName = fileName;
        this.content = content;
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
        hasBeenConverted = true;
        return content.getInputStream();
    }

    @Override
    public <T> T getContent(Class<T> type)
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        if (hasBeenConverted) {
            throw new IllegalStateException("getContent() has already been called");
        }
        hasBeenConverted = true;

        // fast-paths
        if (type == String.class || type == CharSequence.class) {
            return (T) new String(content.getBytes(), getCharset());
        } else if (type == byte[].class) {
            return (T) content.getBytes();
        }

        return (T) MultipartSupport.getConvertedFormAttribute(name, type, null, mediaType, context);
    }

    @Override
    public <T> T getContent(GenericType<T> type)
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        if (hasBeenConverted) {
            throw new IllegalStateException("getContent() has already been called");
        }
        hasBeenConverted = true;

        // fast-paths
        if (type.getType() == String.class || type.getType() == CharSequence.class) {
            return (T) new String(content.getBytes(), getCharset());
        } else if (type.getType() == byte[].class) {
            return (T) content.getBytes();
        }

        return (T) MultipartSupport.getConvertedFormAttribute(name, type.getRawType(), type.getType(), mediaType, context);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return this.headers;
    }

    @Override
    public MediaType getMediaType() {
        return this.mediaType;
    }

    private Charset getCharset() {

        if (mediaType.getParameters().containsKey("charset")) {
            try {
                return Charset.forName(mediaType.getParameters().get("charset"));
            } catch (Exception ignored) {
            }
        }

        return StandardCharsets.UTF_8;
    }

    private static class Content {
        private final Object underlying;

        public Content(File file) {
            this.underlying = file;
        }

        public Content(byte[] bytes) {
            this.underlying = bytes;
        }

        public InputStream getInputStream() {
            if (underlying instanceof File) {
                try {
                    return new FileInputStream((File) underlying);
                } catch (FileNotFoundException e) {
                    throw new MultipartPartReadingException(e);
                }
            } else if (underlying instanceof byte[]) {
                return new ByteArrayInputStream((byte[]) underlying);
            } else {
                throw new IllegalStateException("Cannot get InputStream from content");
            }
        }

        public byte[] getBytes() {
            if (underlying instanceof File) {
                try (InputStream stream = new FileInputStream((File) underlying)) {
                    return stream.readAllBytes();
                } catch (IOException e) {
                    throw new MultipartPartReadingException(e);
                }
            } else if (underlying instanceof byte[]) {
                return (byte[]) underlying;
            } else {
                throw new IllegalStateException("Cannot get byte[] from content");
            }
        }
    }

    public static class Builder implements EntityPart.Builder {
        private final ResteasyReactiveRequestContext context;
        private final String name;
        private MultivaluedMap<String, String> headers;
        private MediaType mediaType;
        private String fileName;
        private Content content;

        public Builder(ResteasyReactiveRequestContext context, String controlName) {
            this.context = context;
            this.name = controlName;

            mediaType = null;
            fileName = null;
            headers = new MultivaluedHashMap<>();
        }

        @Override
        public EntityPart.Builder mediaType(MediaType mediaType) throws IllegalArgumentException {
            if (mediaType == null) {
                throw new IllegalArgumentException("mediaType cannot be null");
            }

            this.mediaType = mediaType;

            return this;
        }

        @Override
        public EntityPart.Builder mediaType(String mediaTypeString) throws IllegalArgumentException {
            try {
                return mediaType(MediaType.valueOf(mediaTypeString));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid media type: " + mediaTypeString, e);
            }
        }

        @Override
        public EntityPart.Builder header(String headerName, String... headerValues) throws IllegalArgumentException {
            if (headerName == null) {
                throw new IllegalArgumentException("headerName cannot be null");
            }

            headers.put(headerName, List.of(headerValues));

            return this;
        }

        @Override
        public EntityPart.Builder headers(MultivaluedMap<String, String> newHeaders) throws IllegalArgumentException {
            if (newHeaders == null) {
                throw new IllegalArgumentException("newHeaders cannot be null");
            }

            headers.putAll(newHeaders);

            return this;
        }

        @Override
        public EntityPart.Builder fileName(String fileName) throws IllegalArgumentException {
            if (fileName == null) {
                throw new IllegalArgumentException("fileName cannot be null");
            }

            if (mediaType == null) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            }

            this.fileName = fileName;

            return this;
        }

        @Override
        public EntityPart.Builder content(InputStream content) throws IllegalArgumentException {
            if (content == null) {
                throw new IllegalArgumentException("content cannot be null");
            }

            // we cannot just store the stream, as it may be read multiple times
            try {
                this.content = new Content(content.readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return this;
        }

        @Override
        public <T> EntityPart.Builder content(T content, Class<? extends T> type) throws IllegalArgumentException {
            // FIXME: implement this
            return null;
        }

        @Override
        public <T> EntityPart.Builder content(T content, GenericType<T> type) throws IllegalArgumentException {
            // FIXME: implement this
            return null;
        }

        @Override
        public EntityPart build() {
            return new EntityPartImpl(context, name, headers, mediaType, fileName, content);
        }

    }
}
