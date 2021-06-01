package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformStream;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JsonPlatform extends JsonEntityWithAnySupport implements Platform {

    private String platformKey;
    private String name;
    private List<PlatformStream> streams;

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
    public List<PlatformStream> getStreams() {
        return streams == null ? Collections.emptyList() : streams;
    }

    public void setStreams(List<PlatformStream> streams) {
        this.streams = streams;
    }

    @Override
    public String toString() {
        return platformKey + streams;
    }
}
