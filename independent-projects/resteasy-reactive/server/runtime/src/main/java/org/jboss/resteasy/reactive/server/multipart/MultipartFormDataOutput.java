package org.jboss.resteasy.reactive.server.multipart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

/**
 * Used when a Resource method needs to return a multipart output
 */
public final class MultipartFormDataOutput {
    private final Map<String, List<PartItem>> parts = new LinkedHashMap<>();

    /**
     * @deprecated use {@link #getAllFormData()} instead
     */
    @Deprecated(forRemoval = true)
    public Map<String, PartItem> getFormData() {
        Map<String, PartItem> result = new LinkedHashMap<>();
        for (var entry : parts.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            // use the last item inserted as this is the old behavior
            int lastIndex = entry.getValue().size() - 1;
            result.put(entry.getKey(), entry.getValue().get(lastIndex));
        }
        return Collections.unmodifiableMap(result);
    }

    public Map<String, List<PartItem>> getAllFormData() {
        return Collections.unmodifiableMap(parts);
    }

    public PartItem addFormData(String key, Object entity, MediaType mediaType) {
        return addFormData(key, entity, null, mediaType, new QuarkusMultivaluedHashMap<>());
    }

    public PartItem addFormData(String key, Object entity, MediaType mediaType, MultivaluedMap<String, Object> headers) {
        return addFormData(key, entity, null, mediaType, headers);
    }

    public PartItem addFormData(String key, Object entity, String genericType, MediaType mediaType) {
        return addFormData(key, entity, genericType, mediaType, new QuarkusMultivaluedHashMap<>());
    }

    public PartItem addFormData(String key, Object entity, String genericType, MediaType mediaType,
            MultivaluedMap<String, Object> headers) {
        PartItem part = new PartItem(entity, genericType, mediaType, null, headers);
        return addFormData(key, part);
    }

    public PartItem addFormData(String key, Object entity, MediaType mediaType, String filename) {
        return addFormData(key, entity, mediaType, filename, new QuarkusMultivaluedHashMap<>());
    }

    public PartItem addFormData(String key, Object entity, MediaType mediaType, String filename,
            MultivaluedMap<String, Object> headers) {
        PartItem part = new PartItem(entity, null, mediaType, filename, headers);
        return addFormData(key, part);
    }

    private PartItem addFormData(String key, PartItem part) {
        List<PartItem> items = parts.computeIfAbsent(key, k -> new ArrayList<>());
        items.add(part);
        return part;
    }
}
