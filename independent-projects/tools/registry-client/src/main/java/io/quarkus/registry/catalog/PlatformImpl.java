package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.registry.catalog.json.JsonPlatformStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = PlatformImpl.Builder.class)
public class PlatformImpl extends CatalogMetadata implements Platform {

    private final String platformKey;
    private final String name;
    private final Map<String, PlatformStream> streams;

    private PlatformImpl(Builder builder) {
        super(builder);
        this.platformKey = builder.platformKey;
        this.name = builder.name;
        this.streams = builder.streams == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.streams);
    }

    @Override
    public String getPlatformKey() {
        return platformKey;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @JsonDeserialize(contentAs = PlatformStreamImpl.class)
    public Collection<PlatformStream> getStreams() {
        return streams == null ? Collections.emptyList() : streams.values();
    }

    @Override
    @JsonIgnore
    public PlatformStream getStream(String id) {
        return streams == null ? null : streams.get(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends CatalogMetadata.Builder implements Platform {
        private String platformKey;
        private String name;
        private Map<String, PlatformStream> streams;

        public Builder() {
        }

        public Builder withPlatformKey(String platformKey) {
            this.platformKey = platformKey;
            return this;
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            super.withMetadata(metadata);
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withStreams(List<PlatformStream> streams) {
            for (PlatformStream s : streams) {
                addStream(s);
            }
            return this;
        }

        public Builder addStream(PlatformStream stream) {
            if (streams == null) {
                streams = new LinkedHashMap<>();
            }
            streams.put(stream.getId(), stream);
            return this;
        }

        @Override
        public String getPlatformKey() {
            return platformKey;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        @JsonDeserialize(contentAs = JsonPlatformStream.class)
        public Collection<PlatformStream> getStreams() {
            return streams == null ? Collections.emptyList() : streams.values();
        }

        @Override
        @JsonIgnore
        public PlatformStream getStream(String id) {
            return streams == null ? null : streams.get(id);
        }

        public PlatformImpl build() {
            return new PlatformImpl(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Platform.class.isInstance(o)) {
            return false;
        }
        Platform that = (Platform) o;
        return Objects.equals(platformKey, that.getPlatformKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformKey);
    }

    @Override
    public String toString() {
        return platformKey + streams;
    }
}
