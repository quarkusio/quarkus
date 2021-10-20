package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonPlatformRelease extends JsonEntityWithAnySupport implements PlatformRelease.Mutable {

    private PlatformReleaseVersion version;
    private Collection<ArtifactCoords> memberBoms;
    private String quarkusCoreVersion;
    private String upstreamQuarkusCoreVersion;

    @Override
    @JsonDeserialize(as = PlatformReleaseVersion.class)
    @JsonSerialize(as = PlatformReleaseVersion.class)
    public PlatformReleaseVersion getVersion() {
        return version;
    }

    public JsonPlatformRelease setVersion(PlatformReleaseVersion version) {
        this.version = version;
        return this;
    }

    @Override
    public Collection<ArtifactCoords> getMemberBoms() {
        return memberBoms == null ? Collections.emptyList() : memberBoms;
    }

    public JsonPlatformRelease setMemberBoms(Collection<ArtifactCoords> memberBoms) {
        this.memberBoms = memberBoms;
        return this;
    }

    @Override
    public String getQuarkusCoreVersion() {
        return quarkusCoreVersion;
    }

    public JsonPlatformRelease setQuarkusCoreVersion(String quarkusCoreVersion) {
        this.quarkusCoreVersion = quarkusCoreVersion;
        return this;
    }

    @Override
    public String getUpstreamQuarkusCoreVersion() {
        return upstreamQuarkusCoreVersion;
    }

    public JsonPlatformRelease setUpstreamQuarkusCoreVersion(String quarkusCoreVersion) {
        this.upstreamQuarkusCoreVersion = quarkusCoreVersion;
        return this;
    }

    @Override
    public JsonPlatformRelease setMetadata(Map<String, Object> metadata) {
        super.setMetadata(metadata);
        return this;
    }

    @Override
    public JsonPlatformRelease setMetadata(String name, Object value) {
        super.setMetadata(name, value);
        return this;
    }

    @Override
    public JsonPlatformRelease removeMetadata(String key) {
        super.removeMetadata(key);
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
        JsonPlatformRelease that = (JsonPlatformRelease) o;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return version.toString() + memberBoms;
    }

    @Override
    public JsonPlatformRelease mutable() {
        return this;
    }

    @Override
    public JsonPlatformRelease build() {
        return this;
    }
}
