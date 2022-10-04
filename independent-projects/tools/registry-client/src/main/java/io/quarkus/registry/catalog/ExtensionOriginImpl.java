package io.quarkus.registry.catalog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 * <p>
 * Note the scope for IdentityInfo is {@link ExtensionOrigin}, to cover both the builder and the impl.
 * </p>
 *
 * @see ExtensionOrigin#mutable() creates a builder from an existing Category
 * @see ExtensionOrigin#builder() creates a builder
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class, scope = ExtensionOrigin.class)
public class ExtensionOriginImpl implements ExtensionOrigin {
    protected final String id;
    protected final boolean platform;
    protected final ArtifactCoords bom;
    protected final Map<String, Object> metadata;

    protected ExtensionOriginImpl(String id) {
        this.id = id;
        this.platform = false;
        this.bom = null;
        metadata = Collections.emptyMap();
    }

    protected ExtensionOriginImpl(Builder builder) {
        this.id = builder.id;
        this.platform = builder.platform;
        this.bom = builder.bom;
        this.metadata = JsonBuilder.toUnmodifiableMap(builder.metadata);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ArtifactCoords getBom() {
        return bom;
    }

    @Override
    public boolean isPlatform() {
        return platform;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        return originEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return originToString(this);
    }

    /**
     * Builder.
     */
    @JsonIdentityInfo(property = "id", generator = ObjectIdGenerators.PropertyGenerator.class, scope = ExtensionOrigin.class)
    public static class Builder implements ExtensionOrigin.Mutable {
        protected String id;
        protected boolean platform;
        protected ArtifactCoords bom;
        private Map<String, Object> metadata;

        public Builder() {
        }

        Builder(ExtensionOrigin source) {
            this.id = source.getId();
            this.platform = source.isPlatform();
            this.bom = source.getBom();
            setMetadata(source.getMetadata());
        }

        public Builder(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        @Override
        public ArtifactCoords getBom() {
            return bom;
        }

        public Builder setBom(ArtifactCoords bom) {
            this.bom = bom;
            return this;
        }

        @Override
        public boolean isPlatform() {
            return platform;
        }

        public Builder setPlatform(boolean platform) {
            this.platform = platform;
            return this;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata == null ? metadata = new HashMap<>() : metadata;
        }

        public Builder setMetadata(Map<String, Object> newValues) {
            metadata = JsonBuilder.modifiableMapOrNull(newValues, HashMap::new);
            return this;
        }

        @JsonIgnore
        public Builder setMetadata(String key, Object value) {
            getMetadata().put(key, value);
            return this;
        }

        public Builder removeMetadata(String key) {
            getMetadata().remove(key);
            return this;
        }

        @Override
        public ExtensionOriginImpl build() {
            return new ExtensionOriginImpl(this);
        }

        @Override
        public boolean equals(Object o) {
            return originEquals(this, o);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return originToString(this);
        }
    }

    final static boolean originEquals(ExtensionOrigin o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (!(o2 instanceof ExtensionOrigin)) {
            return false;
        }
        ExtensionOrigin origin = (ExtensionOrigin) o2;
        return Objects.equals(o1.getId(), origin.getId());
    }

    final static String originToString(ExtensionOrigin o1) {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if (o1.getId() != null) {
            buf.append("id=").append(o1.getId()).append(", ");
        }
        buf.append("platform=").append(o1.isPlatform());
        buf.append(", boms=").append(o1.getBom());
        return buf.append(']').toString();
    }
}
