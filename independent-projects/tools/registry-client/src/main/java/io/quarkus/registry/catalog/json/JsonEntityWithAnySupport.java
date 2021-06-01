package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

abstract class JsonEntityWithAnySupport {

    private Map<String, Object> metadata;

    @JsonAnyGetter
    public Map<String, Object> getMetadata() {
        return metadata == null ? Collections.emptyMap() : metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @JsonAnySetter
    public void setAny(String name, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(name, value);
    }
}
