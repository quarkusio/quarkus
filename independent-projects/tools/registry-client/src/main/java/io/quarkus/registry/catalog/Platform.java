package io.quarkus.registry.catalog;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.registry.json.JsonBuilder;

public interface Platform {

    String getPlatformKey();

    String getName();

    Collection<PlatformStream> getStreams();

    Map<String, Object> getMetadata();

    PlatformStream getStream(String id);

    @JsonIgnore
    default PlatformStream getRecommendedStream() {
        final Collection<PlatformStream> streams = getStreams();
        if (streams.isEmpty()) {
            throw new RuntimeException("Platform " + getPlatformKey() + " does not include any stream");
        }
        return streams.iterator().next();
    }

    default Mutable mutable() {
        return new PlatformImpl.Builder(this);
    }

    interface Mutable extends Platform, JsonBuilder<Platform> {
        Mutable setMetadata(Map<String, Object> metadata);

        Mutable setPlatformKey(String platformKey);

        Mutable setName(String name);

        Mutable setStreams(Collection<PlatformStream> streams);

        Mutable addStream(PlatformStream stream);

        Platform build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new PlatformImpl.Builder();
    }
}
