package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JsonPlatform extends JsonEntityWithAnySupport implements Platform {

    private String platformKey;
    private String name;
    private Map<String, PlatformStream> streams;

    @Override
    public String getPlatformKey() {
        return platformKey;
    }

    public void setPlatformKey(String platformKey) {
        this.platformKey = platformKey;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setStreams(List<PlatformStream> streams) {
        for (PlatformStream s : streams) {
            addStream(s);
        }
    }

    public void addStream(PlatformStream stream) {
        if (streams == null) {
            streams = new LinkedHashMap<>();
        }
        streams.put(stream.getId(), stream);
    }

    @Override
    public String toString() {
        return platformKey + streams;
    }

}
