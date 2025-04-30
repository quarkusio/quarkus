package io.quarkus.registry.catalog;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.registry.json.JsonBuilder;
import io.quarkus.registry.json.JsonEntityWithAnySupport;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see Platform#mutable() creates a builder from an existing Platform
 * @see Platform#builder() creates a builder
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "platformKey", "name", "streams", "metadata" })
public class PlatformImpl extends JsonEntityWithAnySupport implements Platform {

    private final String platformKey;
    private final String name;
    private final Map<String, PlatformStream> streams;

    private PlatformImpl(Builder builder) {
        super(builder);
        this.platformKey = builder.platformKey;
        this.name = builder.name;
        this.streams = JsonBuilder.buildUnmodifiableMap(builder.streams, LinkedHashMap::new);
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
    @JsonSerialize(contentAs = PlatformStreamImpl.class)
    public Collection<PlatformStream> getStreams() {
        return streams.values();
    }

    @Override
    @JsonIgnore
    public PlatformStream getStream(String id) {
        return streams.get(id);
    }

    @Override
    public boolean equals(Object o) {
        return platformImplEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformKey);
    }

    @Override
    public String toString() {
        return platformToString(this);
    }

    /**
     * Builder.
     */
    public static class Builder extends JsonEntityWithAnySupport.Builder
            implements Platform.Mutable {
        private String platformKey;
        private String name;
        private Map<String, PlatformStream> streams;

        public Builder() {
        }

        public Builder(Platform source) {
            this.platformKey = source.getPlatformKey();
            this.name = source.getName();
            setStreams(source.getStreams());
            setMetadata(source.getMetadata());
        }

        @Override
        public String getPlatformKey() {
            return platformKey;
        }

        public Builder setPlatformKey(String platformKey) {
            this.platformKey = platformKey;
            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Collection<PlatformStream> getStreams() {
            return streams == null ? Collections.emptyList() : streams.values();
        }

        @Override
        @JsonIgnore
        public PlatformStream getStream(String id) {
            return streams == null ? null : streams.get(id);
        }

        @JsonDeserialize(contentAs = PlatformStreamImpl.Builder.class)
        public Builder setStreams(Collection<PlatformStream> newStreams) {
            for (PlatformStream s : newStreams) {
                addStream(s);
            }
            return this;
        }

        @JsonIgnore
        public Builder addStream(PlatformStream stream) {
            if (streams == null) {
                streams = new LinkedHashMap<>();
            }
            streams.put(stream.getId(), stream);
            return this;
        }

        @Override
        public Builder setMetadata(Map<String, Object> metadata) {
            super.setMetadata(metadata);
            return this;
        }

        @Override
        public Builder setMetadata(String name, Object value) {
            super.setMetadata(name, value);
            return this;
        }

        @Override
        public Builder removeMetadata(String key) {
            super.removeMetadata(key);
            return this;
        }

        @Override
        public PlatformImpl build() {
            return new PlatformImpl(this);
        }

        @Override
        public boolean equals(Object o) {
            return platformImplEquals(this, o);
        }

        @Override
        public int hashCode() {
            return Objects.hash(platformKey);
        }

        @Override
        public String toString() {
            return platformToString(this);
        }
    }

    static final boolean platformImplEquals(Platform p, Object o) {
        if (p == o) {
            return true;
        }
        if (!(p instanceof Platform)) {
            return false;
        }
        Platform that = (Platform) o;
        return Objects.equals(p.getPlatformKey(), that.getPlatformKey());
    }

    static final String platformToString(Platform p) {
        return p.getClass().getSimpleName() +
                "{platformKey='" + p.getPlatformKey() + '\'' +
                ", name='" + p.getName() + '\'' +
                ", streams=" + p.getStreams() +
                '}';
    }
}
