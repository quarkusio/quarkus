package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.maven.ArtifactCoords;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = ExtensionOriginImpl.Builder.class)
public class ExtensionOriginImpl implements ExtensionOrigin {
    protected final String id;
    protected final boolean platform;
    protected final ArtifactCoords bom;

    protected ExtensionOriginImpl(Builder builder) {
        this.id = builder.id;
        this.platform = builder.platform;
        this.bom = builder.bom;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isPlatform() {
        return platform;
    }

    @Override
    public ArtifactCoords getBom() {
        return bom;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder implements ExtensionOrigin {
        protected String id;
        protected boolean platform;
        protected ArtifactCoords bom;

        public Builder() {
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withPlatform(boolean platform) {
            this.platform = platform;
            return this;
        }

        public Builder withBom(ArtifactCoords bom) {
            this.bom = bom;
            return this;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isPlatform() {
            return platform;
        }

        @Override
        public ArtifactCoords getBom() {
            return bom;
        }

        public ExtensionOriginImpl build() {
            return new ExtensionOriginImpl(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !ExtensionOrigin.class.isInstance(o)) {
            return false;
        }
        ExtensionOrigin that = (ExtensionOrigin) o;
        return Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if (id != null) {
            buf.append("id=").append(id).append(' ');
        }
        buf.append("platform=").append(platform);
        buf.append(" boms=").append(bom);
        return buf.append(']').toString();
    }
}
