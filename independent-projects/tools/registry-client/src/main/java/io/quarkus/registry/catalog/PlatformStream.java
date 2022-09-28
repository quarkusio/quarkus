package io.quarkus.registry.catalog;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.registry.json.JsonBuilder;

public interface PlatformStream {

    String getId();

    String getName();

    Collection<PlatformRelease> getReleases();

    Map<String, Object> getMetadata();

    PlatformRelease getRelease(PlatformReleaseVersion version);

    @JsonIgnore
    default PlatformRelease getRecommendedRelease() {
        final Collection<PlatformRelease> releases = getReleases();
        if (releases.isEmpty()) {
            throw new RuntimeException("Stream " + getId() + " does not include any release");
        }
        return releases.iterator().next();
    }

    default Mutable mutable() {
        return new PlatformStreamImpl.Builder(this);
    }

    interface Mutable extends PlatformStream, JsonBuilder<PlatformStream> {
        Mutable setId(String id);

        Mutable setName(String name);

        Mutable setReleases(Collection<PlatformRelease> releases);

        Mutable addRelease(PlatformRelease platformRelease);

        Mutable setMetadata(Map<String, Object> metadata);

        Mutable setMetadata(String key, Object value);

        Mutable removeMetadata(String key);

        PlatformStream build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new PlatformStreamImpl.Builder();
    }
}
