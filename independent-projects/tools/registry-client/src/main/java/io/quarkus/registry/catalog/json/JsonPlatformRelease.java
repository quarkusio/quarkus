package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import java.util.Collection;
import java.util.Collections;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JsonPlatformRelease extends JsonEntityWithAnySupport implements PlatformRelease {

    private PlatformReleaseVersion version;
    private Collection<ArtifactCoords> memberBoms;
    private String quarkusCoreVersion;
    private String upstreamQuarkusCoreVersion;

    @Override
    @JsonDeserialize(as = JsonPlatformReleaseVersion.class)
    @JsonSerialize(as = JsonPlatformReleaseVersion.class)
    public PlatformReleaseVersion getVersion() {
        return version;
    }

    public void setVersion(PlatformReleaseVersion version) {
        this.version = version;
    }

    @Override
    public Collection<ArtifactCoords> getMemberBoms() {
        return memberBoms == null ? Collections.emptyList() : memberBoms;
    }

    public void setMemberBoms(Collection<ArtifactCoords> memberBoms) {
        this.memberBoms = memberBoms;
    }

    @Override
    public String getQuarkusCoreVersion() {
        return quarkusCoreVersion;
    }

    public void setQuarkusCoreVersion(String quarkusCoreVersion) {
        this.quarkusCoreVersion = quarkusCoreVersion;
    }

    @Override
    public String getUpstreamQuarkusCoreVersion() {
        return upstreamQuarkusCoreVersion;
    }

    public void setUpstreamQuarkusCoreVersion(String quarkusCoreVersion) {
        this.upstreamQuarkusCoreVersion = quarkusCoreVersion;
    }

    @Override
    public String toString() {
        return version.toString() + memberBoms;
    }
}
