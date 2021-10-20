package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Deprecated
abstract class JsonEntityWithAnySupport {

    private Map<String, Object> metadata = new HashMap<>(0);

    @JsonAnyGetter
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public JsonEntityWithAnySupport setMetadata(Map<String, Object> newValues) {
        if (newValues != null && newValues != Collections.EMPTY_MAP) { // don't keep the empty map
            metadata = newValues;
        }
        return this;
    }

    @JsonAnySetter
    public void setAny(String key, Object value) {
        metadata.put(key, value);
    }

    protected JsonEntityWithAnySupport setMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    protected JsonEntityWithAnySupport removeMetadata(String key) {
        metadata.remove(key);
        return this;
    }
}
