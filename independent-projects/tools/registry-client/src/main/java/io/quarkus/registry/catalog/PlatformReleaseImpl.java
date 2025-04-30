package io.quarkus.registry.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;
import io.quarkus.registry.json.JsonEntityWithAnySupport;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see PlatformRelease#mutable() creates a builder from an existing Category
 * @see PlatformRelease#builder() creates a builder
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "version", "memberBoms", "quarkusCoreVersion", "upstreamQuarkusCoreVersion", "metadata" })
public class PlatformReleaseImpl extends JsonEntityWithAnySupport implements PlatformRelease {
    private final PlatformReleaseVersion version;
    private final Collection<ArtifactCoords> memberBoms;
    private final String quarkusCoreVersion;
    private final String upstreamQuarkusCoreVersion;

    private PlatformReleaseImpl(Builder builder) {
        super(builder);
        this.version = builder.version;
        this.quarkusCoreVersion = builder.quarkusCoreVersion;
        this.upstreamQuarkusCoreVersion = builder.upstreamQuarkusCoreVersion;

        this.memberBoms = JsonBuilder.toUnmodifiableList(builder.memberBoms);
    }

    @Override
    @JsonSerialize(as = PlatformReleaseVersion.class)
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

    @Override
    public boolean equals(Object o) {
        return platformReleaseEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return platformReleaseToString(this);
    }

    /**
     * Builder.
     */
    public static class Builder extends JsonEntityWithAnySupport.Builder implements PlatformRelease.Mutable {
        private PlatformReleaseVersion version;
        private List<ArtifactCoords> memberBoms;
        private String quarkusCoreVersion;
        private String upstreamQuarkusCoreVersion;

        public Builder() {
        }

        Builder(PlatformRelease source) {
            this.version = source.getVersion();
            this.memberBoms = JsonBuilder.modifiableListOrNull(source.getMemberBoms());
            this.quarkusCoreVersion = source.getQuarkusCoreVersion();
            this.upstreamQuarkusCoreVersion = source.getUpstreamQuarkusCoreVersion();
            setMetadata(source.getMetadata());
        }

        @Override
        public PlatformReleaseVersion getVersion() {
            return version;
        }

        public Builder setVersion(PlatformReleaseVersion version) {
            this.version = version;
            return this;
        }

        @Override
        public Collection<ArtifactCoords> getMemberBoms() {
            return memberBoms == null
                    ? memberBoms = new ArrayList<>()
                    : memberBoms;
        }

        public Builder setMemberBoms(Collection<ArtifactCoords> memberBoms) {
            this.memberBoms = JsonBuilder.modifiableListOrNull(memberBoms);
            return this;
        }

        @Override
        public String getQuarkusCoreVersion() {
            return quarkusCoreVersion;
        }

        public Builder setQuarkusCoreVersion(String quarkusCoreVersion) {
            this.quarkusCoreVersion = quarkusCoreVersion;
            return this;
        }

        @Override
        public String getUpstreamQuarkusCoreVersion() {
            return upstreamQuarkusCoreVersion;
        }

        public Builder setUpstreamQuarkusCoreVersion(String upstreamQuarkusCoreVersion) {
            this.upstreamQuarkusCoreVersion = upstreamQuarkusCoreVersion;
            return this;
        }

        @Override
        public Builder setMetadata(Map<String, Object> metadata) {
            super.setMetadata(metadata);
            return this;
        }

        @Override
        public Builder setMetadata(String key, Object value) {
            super.setMetadata(key, value);
            return this;
        }

        @Override
        public Builder removeMetadata(String key) {
            super.removeMetadata(key);
            return this;
        }

        @Override
        public PlatformReleaseImpl build() {
            return new PlatformReleaseImpl(this);
        }

        @Override
        public boolean equals(Object o) {
            return platformReleaseEquals(this, o);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version);
        }

        @Override
        public String toString() {
            return platformReleaseToString(this);
        }
    }

    static final boolean platformReleaseEquals(PlatformRelease p, Object o) {
        if (p == o) {
            return true;
        }
        if (!(o instanceof PlatformRelease)) {
            return false;
        }
        PlatformRelease that = (PlatformRelease) o;
        return Objects.equals(p.getVersion(), that.getVersion());
    }

    static final String platformReleaseToString(PlatformRelease p) {
        return p.getClass().getSimpleName() +
                "{version=" + p.getVersion() +
                ", quarkusCoreVersion='" + p.getQuarkusCoreVersion() + '\'' +
                ", upstreamQuarkusCoreVersion='" + p.getUpstreamQuarkusCoreVersion() + '\'' +
                ", memberBoms=" + p.getMemberBoms() +
                '}';
    }
}
