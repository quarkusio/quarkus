package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.maven.ArtifactCoords;
import java.util.Objects;

@JsonDeserialize(builder = RegistryPlatformsConfigImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryPlatformsConfigImpl extends RegistryArtifactConfigImpl implements RegistryPlatformsConfig {

    protected final Boolean extensionCatalogsIncluded;

    private RegistryPlatformsConfigImpl(Builder builder) {
        super(builder);
        this.extensionCatalogsIncluded = builder.extensionCatalogsIncluded;
    }

    @Override
    public Boolean getExtensionCatalogsIncluded() {
        return extensionCatalogsIncluded;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends RegistryArtifactConfigImpl.Builder implements RegistryPlatformsConfig {
        protected Boolean extensionCatalogsIncluded;

        public Builder() {
        }

        @JsonIgnore
        public Builder(RegistryPlatformsConfig config) {
            super(config);
            this.extensionCatalogsIncluded = config.getExtensionCatalogsIncluded();
        }

        @Override
        public Builder withDisabled(boolean disabled) {
            super.withDisabled(disabled);
            return this;
        }

        @Override
        public Builder withArtifact(ArtifactCoords artifact) {
            super.withArtifact(artifact);
            return this;
        }

        public Builder withExtensionCatalogsIncluded(boolean extensionCatalogsIncluded) {
            this.extensionCatalogsIncluded = extensionCatalogsIncluded;
            return this;
        }

        @Override
        public Boolean getExtensionCatalogsIncluded() {
            return extensionCatalogsIncluded;
        }

        public RegistryPlatformsConfigImpl build() {
            return new RegistryPlatformsConfigImpl(this);
        }
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
        return "BaseRegistryPlatformsConfig{" +
                "extensionCatalogsIncluded=" + extensionCatalogsIncluded +
                '}';
    }
}
