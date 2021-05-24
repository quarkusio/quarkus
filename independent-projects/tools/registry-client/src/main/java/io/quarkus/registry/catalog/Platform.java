package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;

public interface Platform {

    String getPlatformKey();

    String getName();

    List<PlatformStream> getStreams();

    Map<String, Object> getMetadata();

    @JsonIgnore
    default PlatformStream getRecommendedStream() {
        final List<PlatformStream> streams = getStreams();
        if (streams.isEmpty()) {
            throw new RuntimeException("Platform " + getPlatformKey() + " does not include any stream");
        }
        return streams.get(0);
    }
}
