package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(builder = CatalogMetadata.Builder.class)
class CatalogMetadata {
    protected final Map<String, Object> metadata;

    protected CatalogMetadata(Builder builder) {
        this.metadata = Collections.unmodifiableMap(builder.getMetadata());
    }

    @JsonAnyGetter
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    static class Builder {
        protected Map<String, Object> metadata = new HashMap<>(0);

        public Builder() {
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            this.getMetadata().clear();
            if (metadata != null) {
                this.getMetadata().putAll(metadata);
            }
            return this;
        }

        @JsonAnySetter
        public Builder setMetadata(String name, Object value) {
            metadata.put(name, value);
            return this;
        }

        public Builder removeMetadata(String key) {
            this.getMetadata().remove(key);
            return this;
        }

        @JsonAnyGetter
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}
