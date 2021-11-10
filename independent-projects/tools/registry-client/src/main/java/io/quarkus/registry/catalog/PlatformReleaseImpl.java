package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.json.JsonPlatformReleaseVersion;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = PlatformReleaseImpl.Builder.class)
public class PlatformReleaseImpl extends CatalogMetadata implements PlatformRelease {
    private final PlatformReleaseVersion version;
    private final Collection<ArtifactCoords> memberBoms;
    private final String quarkusCoreVersion;
    private final String upstreamQuarkusCoreVersion;

    private PlatformReleaseImpl(Builder builder) {
        super(builder);
        this.version = builder.version;
        this.memberBoms = Collections.unmodifiableCollection(builder.getMemberBoms());
        this.quarkusCoreVersion = builder.quarkusCoreVersion;
        this.upstreamQuarkusCoreVersion = builder.upstreamQuarkusCoreVersion;
    }

    @Override
    @JsonDeserialize(as = PlatformReleaseVersionImpl.class)
    @JsonSerialize(as = PlatformReleaseVersionImpl.class)
    public PlatformReleaseVersion getVersion() {
        return version;
    }

    @Override
    public Collection<ArtifactCoords> getMemberBoms() {
        return memberBoms;
    }

    @Override
    public String getQuarkusCoreVersion() {
        return quarkusCoreVersion;
    }

    @Override
    public String getUpstreamQuarkusCoreVersion() {
        return upstreamQuarkusCoreVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends CatalogMetadata.Builder implements PlatformRelease {
        private PlatformReleaseVersion version;
        private Collection<ArtifactCoords> memberBoms;
        private String quarkusCoreVersion;
        private String upstreamQuarkusCoreVersion;

        public Builder() {
        }

        public Builder withVersion(PlatformReleaseVersion version) {
            this.version = version;
            return this;
        }

        public Builder withMemberBoms(Collection<ArtifactCoords> memberBoms) {
            this.memberBoms = memberBoms;
            return this;
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            super.withMetadata(metadata);
            return this;
        }

        public Builder withQuarkusCoreVersion(String quarkusCoreVersion) {
            this.quarkusCoreVersion = quarkusCoreVersion;
            return this;
        }

        public Builder withUpstreamQuarkusCoreVersion(String upstreamQuarkusCoreVersion) {
            this.upstreamQuarkusCoreVersion = upstreamQuarkusCoreVersion;
            return this;
        }

        @Override
        @JsonDeserialize(as = JsonPlatformReleaseVersion.class)
        @JsonSerialize(as = JsonPlatformReleaseVersion.class)
        public PlatformReleaseVersion getVersion() {
            return version;
        }

        @Override
        public Collection<ArtifactCoords> getMemberBoms() {
            return memberBoms == null ? Collections.emptyList() : memberBoms;
        }

        @Override
        public String getQuarkusCoreVersion() {
            return quarkusCoreVersion;
        }

        @Override
        public String getUpstreamQuarkusCoreVersion() {
            return upstreamQuarkusCoreVersion;
        }

        public PlatformReleaseImpl build() {
            return new PlatformReleaseImpl(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof PlatformRelease)) {
            return false;
        }
        PlatformRelease that = (PlatformRelease) o;
        return Objects.equals(version, that.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return version.toString() + memberBoms;
    }
}
