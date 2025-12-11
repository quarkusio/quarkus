package io.quarkus.registry.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see RegistryPlatformsConfig#builder() creates a builder
 * @see RegistryPlatformsConfig#mutable() creates a builder from an existing RegistriesConfig
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryPlatformsConfigImpl extends RegistryArtifactConfigImpl implements RegistryPlatformsConfig {

    protected final Boolean extensionCatalogsIncluded;
    protected final RegistryMavenConfig mavenConfig;

    private RegistryPlatformsConfigImpl(Builder builder) {
        super(builder.disabled, builder.artifact);
        this.extensionCatalogsIncluded = builder.extensionCatalogsIncluded;
        this.mavenConfig = builder.mavenConfig;
    }

    @Override
    public Boolean getExtensionCatalogsIncluded() {
        return extensionCatalogsIncluded;
    }

    @Override
    public RegistryMavenConfig getMaven() {
        return mavenConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        RegistryPlatformsConfigImpl that = (RegistryPlatformsConfigImpl) o;
        return Objects.equals(extensionCatalogsIncluded, that.extensionCatalogsIncluded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), extensionCatalogsIncluded);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() +
                "{disabled=" + disabled +
                ", artifact=" + artifact +
                ", extensionCatalogsIncluded=" + extensionCatalogsIncluded +
                '}';
    }

    public RegistryPlatformsConfig.Mutable mutable() {
        return new Builder(this);
    }

    /**
     * Builder.
     */
    public static class Builder extends RegistryArtifactConfigImpl.Builder implements RegistryPlatformsConfig.Mutable {
        protected Boolean extensionCatalogsIncluded;
        protected RegistryMavenConfig mavenConfig;

        public Builder() {
        }

        @JsonIgnore
        Builder(RegistryPlatformsConfig config) {
            super(config);
            this.extensionCatalogsIncluded = config.getExtensionCatalogsIncluded();
        }

        @Override
        public RegistryPlatformsConfig.Mutable setDisabled(boolean disabled) {
            super.setDisabled(disabled);
            return this;
        }

        @Override
        public RegistryPlatformsConfig.Mutable setArtifact(ArtifactCoords artifact) {
            super.setArtifact(artifact);
            return this;
        }

        @Override
        public Boolean getExtensionCatalogsIncluded() {
            return extensionCatalogsIncluded;
        }

        @Override
        public RegistryPlatformsConfig.Mutable setExtensionCatalogsIncluded(Boolean extensionCatalogsIncluded) {
            this.extensionCatalogsIncluded = extensionCatalogsIncluded;
            return this;
        }

        @Override
        @JsonDeserialize(as = RegistryMavenConfigImpl.Builder.class)
        public RegistryMavenConfig getMaven() {
            return mavenConfig;
        }

        @Override
        public RegistryPlatformsConfig.Mutable setMaven(RegistryMavenConfig mavenConfig) {
            this.mavenConfig = mavenConfig;
            return this;
        }

        @Override
        public RegistryPlatformsConfigImpl build() {
            return new RegistryPlatformsConfigImpl(this);
        }
    }
}
