package io.quarkus.registry.catalog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see Extension#builder() creates a builder
 * @see Extension#mutable() creates a builder from an existing Extension
 * @see Extension#mutableFromFile(Path) creates a builder from the contents of a file
 * @see Extension#fromFile(Path) creates (and builds) a builder from the contents of a file
 * @see Extension#persist(Path) to write an Extension to a file
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "name", "description", "metadata", "artifact", "origins" })
public class ExtensionImpl implements Extension {
    private final String name;
    private final String description;
    private final Map<String, Object> metadata; // Not a JsonAnyGetter
    private final ArtifactCoords artifact;
    private final List<ExtensionOrigin> origins;

    private ExtensionImpl(Builder builder, List<ExtensionOrigin> origins) {
        this.name = builder.getName();
        this.description = builder.getDescription();
        this.artifact = builder.getArtifact();

        this.origins = origins;
        this.metadata = JsonBuilder.toUnmodifiableMap(builder.metadata);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    /**
     * Serialize as ExtensionOriginImpl, which defines JsonIdentityInfo
     */
    @Override
    @JsonIdentityReference(alwaysAsId = true)
    @JsonSerialize(contentAs = ExtensionOriginImpl.class)
    public List<ExtensionOrigin> getOrigins() {
        return origins;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        return extensionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArtifact());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "artifact=" + getArtifact() +
                '}';
    }

    /**
     * Builder.
     */
    public static class Builder implements Extension.Mutable {
        private String name;
        private String description;
        private Map<String, Object> metadata;
        private ArtifactCoords artifact;
        private List<ExtensionOrigin> origins;

        // to support legacy format
        private String groupId;
        private String artifactId;
        private String version;

        Builder() {
        }

        @JsonIgnore
        Builder(Extension e) {
            this.artifact = e.getArtifact();
            this.name = e.getName();
            this.description = e.getDescription();
            this.origins = JsonBuilder.modifiableListOrNull(e.getOrigins());
            setMetadata(e.getMetadata());
        }

        public Builder setGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder setArtifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public String getDescription() {
            return description;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ArtifactCoords getArtifact() {
            if (artifact == null && artifactId != null) {
                artifact = ArtifactCoords.jar(groupId, artifactId, version);
            }
            return artifact;
        }

        public Builder setArtifact(ArtifactCoords coords) {
            this.artifact = coords;
            return this;
        }

        @Override
        @JsonIdentityReference(alwaysAsId = true)
        public List<ExtensionOrigin> getOrigins() {
            return origins == null ? origins = new ArrayList<>() : origins;
        }

        /**
         * Deserialize using ExtensionOriginImpl.Builder, which defines JsonIdentityInfo
         */
        @JsonDeserialize(contentAs = ExtensionOriginImpl.Builder.class)
        @JsonIdentityReference(alwaysAsId = true)
        public Builder setOrigins(List<ExtensionOrigin> origins) {
            this.origins = JsonBuilder.modifiableListOrNull(origins);
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
        public void persist(Path p) throws IOException {
            // the immutable version is properly bound
            build().persist(p);
        }

        @Override
        public ExtensionImpl build() {
            List<ExtensionOrigin> built = origins == null || origins.isEmpty()
                    ? Collections.emptyList()
                    : origins.stream()
                            .map(x -> buildOrigin(x))
                            .collect(Collectors.toUnmodifiableList());
            return new ExtensionImpl(this, built);
        }

        public ExtensionOrigin buildOrigin(ExtensionOrigin x) {
            if (x instanceof ExtensionCatalogImpl.Builder) {
                return ExtensionOrigin.builder()
                        .setBom(x.getBom())
                        .setId(x.getId())
                        .setPlatform(x.isPlatform())
                        .setMetadata(x.getMetadata())
                        .build();
            }
            return JsonBuilder.buildIfBuilder(x);
        }

        @Override
        public boolean equals(Object o) {
            return extensionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getArtifact());
        }
    }

    static boolean extensionEquals(Extension o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (!(o2 instanceof Extension)) {
            return false;
        }
        Extension extension = (Extension) o2;
        return Objects.equals(o1.getArtifact(), extension.getArtifact());
    }

}
