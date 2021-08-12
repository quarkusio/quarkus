package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

abstract class JsonEntityWithAnySupport {

    private Map<String, Object> metadata = new HashMap<>(0);

    @JsonAnyGetter
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @JsonAnySetter
    public void setAny(String name, Object value) {
        metadata.put(name, value);
    }
}
