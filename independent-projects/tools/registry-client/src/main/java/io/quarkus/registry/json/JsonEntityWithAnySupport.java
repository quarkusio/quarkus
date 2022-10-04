package io.quarkus.registry.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
public abstract class JsonEntityWithAnySupport {
    private final Map<String, Object> metadata;

    protected JsonEntityWithAnySupport(Builder builder) {
        this.metadata = builder.metadata == null || builder.metadata.isEmpty()
                ? Collections.emptyMap()
                : Map.copyOf(builder.metadata);
    }

    @JsonAnyGetter
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    protected static abstract class Builder {
        protected Map<String, Object> metadata;

        @JsonAnyGetter
        public Map<String, Object> getMetadata() {
            return metadata == null ? metadata = new HashMap<>() : metadata;
        }

        public Builder setMetadata(Map<String, Object> newValues) {
            metadata = JsonBuilder.modifiableMapOrNull(newValues, HashMap::new);
            return this;
        }

        @JsonAnySetter
        public Builder setMetadata(String key, Object value) {
            getMetadata().put(key, value);
            return this;
        }

        public Builder removeMetadata(String key) {
            getMetadata().remove(key);
            return this;
        }
    }
}
