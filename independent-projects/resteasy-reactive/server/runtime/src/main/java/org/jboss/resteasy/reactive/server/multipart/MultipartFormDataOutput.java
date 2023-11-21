package org.jboss.resteasy.reactive.server.multipart;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

/**
 * Used when a Resource method needs to return a multipart output
 */
public final class MultipartFormDataOutput {
    private final Map<String, PartItem> parts = new LinkedHashMap<>();

    public Map<String, PartItem> getFormData() {
        return Collections.unmodifiableMap(parts);
    }

    public PartItem addFormData(String key, Object entity, MediaType mediaType) {
        return addFormData(key, entity, null, mediaType);
    }

    public PartItem addFormData(String key, Object entity, String genericType, MediaType mediaType) {
        PartItem part = new PartItem(entity, genericType, mediaType);
        return addFormData(key, part);
    }

    public PartItem addFormData(String key, Object entity, MediaType mediaType, String filename) {
        PartItem part = new PartItem(entity, null, mediaType, filename);
        return addFormData(key, part);
    }

    private PartItem addFormData(String key, PartItem part) {
        parts.put(key, part);
        return part;
    }
}
