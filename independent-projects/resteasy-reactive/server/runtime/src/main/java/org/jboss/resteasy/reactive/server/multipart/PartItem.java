package org.jboss.resteasy.reactive.server.multipart;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

/**
 * Represents part of a multipart output
 *
 * @see MultipartFormDataOutput
 */
public final class PartItem {
    private final MultivaluedMap<String, Object> headers;
    private final Object entity;
    private final String genericType;
    private final MediaType mediaType;
    private final String filename;

    public PartItem(Object entity, String genericType, MediaType mediaType) {
        this(entity, genericType, mediaType, null, new QuarkusMultivaluedHashMap<>());
    }

    public PartItem(Object entity, String genericType, MediaType mediaType, String filename) {
        this(entity, genericType, mediaType, filename, new QuarkusMultivaluedHashMap<>());
    }

    public PartItem(Object entity, String genericType, MediaType mediaType, MultivaluedMap<String, Object> headers) {
        this(entity, genericType, mediaType, null, headers);
    }

    public PartItem(Object entity, String genericType, MediaType mediaType, String filename,
            MultivaluedMap<String, Object> headers) {
        this.entity = entity;
        this.genericType = genericType;
        this.mediaType = mediaType;
        this.filename = filename;
        this.headers = headers != null ? headers : new QuarkusMultivaluedHashMap<>();
    }

    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    public Object getEntity() {
        return entity;
    }

    public String getGenericType() {
        return genericType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getFilename() {
        return filename;
    }
}
