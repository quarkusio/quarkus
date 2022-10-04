package io.quarkus.registry.catalog;

import java.util.Collection;
import java.util.Map;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Platform release
 */
public interface PlatformRelease {

    PlatformReleaseVersion getVersion();

    Collection<ArtifactCoords> getMemberBoms();

    String getQuarkusCoreVersion();

    String getUpstreamQuarkusCoreVersion();

    Map<String, Object> getMetadata();

    default Mutable mutable() {
        return new PlatformReleaseImpl.Builder(this);
    }

    interface Mutable extends PlatformRelease, JsonBuilder<PlatformRelease> {

        Mutable setVersion(PlatformReleaseVersion version);

        Mutable setMemberBoms(Collection<ArtifactCoords> memberBoms);

        Mutable setQuarkusCoreVersion(String quarkusCoreVersion);

        Mutable setUpstreamQuarkusCoreVersion(String upstreamQuarkusCoreVersion);

        Mutable setMetadata(Map<String, Object> metadata);

        Mutable setMetadata(String key, Object value);

        Mutable removeMetadata(String key);

        PlatformRelease build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new PlatformReleaseImpl.Builder();
    }
}
