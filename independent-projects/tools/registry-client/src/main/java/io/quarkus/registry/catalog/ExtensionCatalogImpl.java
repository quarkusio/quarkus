package io.quarkus.registry.catalog;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see ExtensionCatalog#builder() creates a builder
 * @see ExtensionCatalog#mutable() creates a builder from an existing ExtensionCatalog
 * @see ExtensionCatalog#mutableFromFile(Path) creates a builder from the contents of a file
 * @see ExtensionCatalog#fromFile(Path) creates (and builds) a builder from the contents of a file
 * @see ExtensionCatalog#fromStream(InputStream) creates (and builds) a builder from the contents of an input stream
 * @see ExtensionCatalog#persist(Path) for writing an ExtensionCatalog to a file
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "id", "platform", "bom", "quarkus-core-version", "upstream-quarkus-core-version", "extensions",
        "categories", "metadata" })
public class ExtensionCatalogImpl extends ExtensionOriginImpl implements ExtensionCatalog {

    private final String quarkusCoreVersion;
    private final String upstreamQuarkusCoreVersion;
    private final List<ExtensionOrigin> derivedFrom;
    private final List<Extension> extensions;
    private final List<Category> categories;

    private ExtensionCatalogImpl(Builder builder, List<Extension> extensions) {
        super(builder);
        this.quarkusCoreVersion = builder.quarkusCoreVersion;
        this.upstreamQuarkusCoreVersion = builder.upstreamQuarkusCoreVersion;

        this.derivedFrom = JsonBuilder.buildersToUnmodifiableList(builder.derivedFrom);
        this.extensions = extensions; // avoid recursion as extensions reference origin catalog
        this.categories = JsonBuilder.buildersToUnmodifiableList(builder.categories);
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
    public List<ExtensionOrigin> getDerivedFrom() {
        return derivedFrom;
    }

    @Override
    public List<Extension> getExtensions() {
        return extensions;
    }

    @Override
    public List<Category> getCategories() {
        return categories;
    }

    /**
     * Builder.
     */
    public static class Builder extends ExtensionOriginImpl.Builder implements ExtensionCatalog.Mutable {
        private String quarkusCoreVersion;
        private String upstreamQuarkusCoreVersion;
        private List<ExtensionOrigin> derivedFrom;
        private List<Extension> extensions;
        private List<Category> categories;

        public Builder() {
        }

        Builder(ExtensionCatalog source) {
            super(source);
            this.quarkusCoreVersion = source.getQuarkusCoreVersion();
            this.upstreamQuarkusCoreVersion = source.getUpstreamQuarkusCoreVersion();
            this.derivedFrom = JsonBuilder.modifiableListOrNull(source.getDerivedFrom());
            this.extensions = JsonBuilder.modifiableListOrNull(source.getExtensions());
            this.categories = JsonBuilder.modifiableListOrNull(source.getCategories());
        }

        @Override
        public Builder setId(String id) {
            super.setId(id);
            return this;
        }

        @Override
        public Builder setPlatform(boolean platform) {
            super.setPlatform(platform);
            return this;
        }

        @Override
        public Builder setBom(ArtifactCoords bom) {
            super.setBom(bom);
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

        @Override
        public Builder setUpstreamQuarkusCoreVersion(String upstreamQuarkusCoreVersion) {
            this.upstreamQuarkusCoreVersion = upstreamQuarkusCoreVersion;
            return this;
        }

        @Override
        public List<ExtensionOrigin> getDerivedFrom() {
            return derivedFrom == null ? Collections.emptyList() : derivedFrom;
        }

        @JsonDeserialize(contentAs = ExtensionOriginImpl.Builder.class)
        public Builder setDerivedFrom(List<ExtensionOrigin> origins) {
            this.derivedFrom = JsonBuilder.modifiableListOrNull(origins);
            return this;
        }

        @Override
        public List<Extension> getExtensions() {
            return extensions == null ? Collections.emptyList() : extensions;
        }

        @JsonDeserialize(contentAs = ExtensionImpl.Builder.class)
        public Builder setExtensions(List<Extension> extensions) {
            this.extensions = JsonBuilder.modifiableListOrNull(extensions);
            return this;
        }

        public Builder addExtension(Extension e) {
            if (extensions == null) {
                extensions = new ArrayList<>();
            }
            extensions.add(e);
            return this;
        }

        @Override
        public List<Category> getCategories() {
            return categories == null ? Collections.emptyList() : categories;
        }

        @JsonDeserialize(contentAs = CategoryImpl.Builder.class)
        public Builder setCategories(List<Category> categories) {
            this.categories = JsonBuilder.modifiableListOrNull(categories);
            return this;
        }

        public Builder addCategory(Category c) {
            if (categories == null) {
                categories = new ArrayList<>();
            }
            categories.add(c);
            return this;
        }

        public Builder setMetadata(Map<String, Object> newValues) {
            super.setMetadata(newValues);
            return this;
        }

        @JsonIgnore
        public Builder setMetadata(String key, Object value) {
            super.setMetadata(key, value);
            return this;
        }

        public Builder removeMetadata(String key) {
            super.removeMetadata(key);
            return this;
        }

        @Override
        public ExtensionCatalogImpl build() {
            List<Extension> built = JsonBuilder.buildersToUnmodifiableList(this.extensions);
            return new ExtensionCatalogImpl(this, built);
        }
    }

    // Note: hashcode, equals, and toString from ExtensionOrigin
}
