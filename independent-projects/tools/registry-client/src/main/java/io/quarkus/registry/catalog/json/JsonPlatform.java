package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonPlatform extends JsonEntityWithAnySupport implements Platform.Mutable {

    private String platformKey;
    private String name;
    private Map<String, PlatformStream> streams;

    @Override
    public String getPlatformKey() {
        return platformKey;
    }

    public JsonPlatform setPlatformKey(String platformKey) {
        this.platformKey = platformKey;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public JsonPlatform setName(String name) {
        this.name = name;
        return this;
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

    @Override
    public JsonPlatform setStreams(Collection<PlatformStream> newValues) {
        for (PlatformStream s : newValues) {
            addStream(s);
        }
        return this;
    }

    @JsonIgnore
    public JsonPlatform addStream(PlatformStream stream) {
        if (streams == null) {
            streams = new LinkedHashMap<>();
        }
        streams.put(stream.getId(), stream);
        return this;
    }

    @Override
    public JsonPlatform setMetadata(Map<String, Object> metadata) {
        super.setMetadata(metadata);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonPlatform that = (JsonPlatform) o;
        return Objects.equals(platformKey, that.platformKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformKey);
    }

    @Override
    public String toString() {
        return platformKey + streams;
    }

    @Override
    public Mutable mutable() {
        return this;
    }

    @Override
    public JsonPlatform build() {
        return this;
    }
}
