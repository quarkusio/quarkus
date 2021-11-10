package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = ExtensionCatalogImpl.Builder.class)
public class ExtensionCatalogImpl extends ExtensionOriginImpl implements ExtensionCatalog {
    private final String quarkusCoreVersion;
    private final String upstreamQuarkusCoreVersion;
    private final List<ExtensionOrigin> derivedFrom;
    private final List<Extension> extensions;
    private final List<Category> categories;
    private final Map<String, Object> metadata;

    private ExtensionCatalogImpl(Builder builder) {
        super(builder);
        this.quarkusCoreVersion = builder.quarkusCoreVersion;
        this.upstreamQuarkusCoreVersion = builder.upstreamQuarkusCoreVersion;
        this.derivedFrom = builder.derivedFrom;
        this.extensions = builder.getExtensions();
        this.categories = builder.getCategories();
        this.metadata = Collections.unmodifiableMap(builder.getMetadata());
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

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends ExtensionOriginImpl.Builder implements ExtensionCatalog {
        private String quarkusCoreVersion;
        private String upstreamQuarkusCoreVersion;
        private List<ExtensionOrigin> derivedFrom;
        private List<Extension> extensions;
        private List<Category> categories;
        private Map<String, Object> metadata = new HashMap<>(0);

        public Builder() {
        }

        public Builder withQuarkusCoreVersion(String quarkusCoreVersion) {
            this.quarkusCoreVersion = quarkusCoreVersion;
            return this;
        }

        public Builder withUpstreamQuarkusCoreVersion(String upstreamQuarkusCoreVersion) {
            this.upstreamQuarkusCoreVersion = upstreamQuarkusCoreVersion;
            return this;
        }

        @JsonDeserialize(contentAs = ExtensionOriginImpl.class)
        public Builder withDerivedFrom(List<ExtensionOrigin> derivedFrom) {
            this.derivedFrom = derivedFrom;
            return this;
        }

        @JsonDeserialize(contentAs = ExtensionImpl.class)
        public Builder withExtensions(List<Extension> extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder addExtension(Extension e) {
            if (extensions == null) {
                extensions = new ArrayList<>();
            }
            extensions.add(e);
            return this;
        }

        @JsonDeserialize(contentAs = CategoryImpl.class)
        public Builder withCategories(List<Category> categories) {
            this.categories = categories;
            return this;
        }

        public Builder addCategory(Category c) {
            if (categories == null) {
                categories = new ArrayList<>();
            }
            categories.add(c);
            return this;
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            this.getMetadata().clear();
            if (metadata != null) {
                this.getMetadata().putAll(metadata);
            }
            return this;
        }

        public Builder setMetadata(String name, Object value) {
            metadata.put(name, value);
            return this;
        }

        public Builder removeMetadata(String key) {
            this.getMetadata().remove(key);
            return this;
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
            return extensions == null ? Collections.emptyList() : extensions;
        }

        @Override
        public List<Category> getCategories() {
            return categories == null ? Collections.emptyList() : categories;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata == null ? Collections.emptyMap() : metadata;
        }

        public ExtensionCatalogImpl build() {
            return new ExtensionCatalogImpl(this);
        }
    }
}
