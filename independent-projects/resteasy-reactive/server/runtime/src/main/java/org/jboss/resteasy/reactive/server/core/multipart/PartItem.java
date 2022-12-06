package org.jboss.resteasy.reactive.server.core.multipart;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

public final class PartItem {
    private MultivaluedMap<String, Object> headers;
    private Object entity;
    private String genericType;
    private MediaType mediaType;
    private String filename;

    public PartItem(Object entity, String genericType, MediaType mediaType) {
        this(entity, genericType, mediaType, null);
    }

    public PartItem(Object entity, String genericType, MediaType mediaType, String filename) {
        this.headers = new QuarkusMultivaluedHashMap<>();
        this.entity = entity;
        this.genericType = genericType;
        this.mediaType = mediaType;
        this.filename = filename;
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
