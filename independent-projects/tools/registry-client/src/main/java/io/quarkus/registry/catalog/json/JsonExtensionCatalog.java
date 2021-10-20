package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonExtensionCatalog extends JsonExtensionOrigin implements ExtensionCatalog.Mutable {

    private String quarkusCore;
    private String upstreamQuarkusCore;
    private List<ExtensionOrigin> derivedFrom;
    private List<Extension> extensions;
    private List<Category> categories;
    private Map<String, Object> metadata;

    @Override
    public ExtensionCatalog.Mutable setId(String id) {
        super.setId(id);
        return this;
    }

    @Override
    public ExtensionCatalog.Mutable setPlatform(boolean platform) {
        super.setPlatform(platform);
        return this;
    }

    @Override
    public ExtensionCatalog.Mutable setBom(ArtifactCoords bom) {
        super.setBom(bom);
        return this;
    }

    @Override
    public String getQuarkusCoreVersion() {
        return quarkusCore;
    }

    public ExtensionCatalog.Mutable setQuarkusCoreVersion(String quarkusCore) {
        this.quarkusCore = quarkusCore;
        return this;
    }

    @Override
    public String getUpstreamQuarkusCoreVersion() {
        return upstreamQuarkusCore;
    }

    public ExtensionCatalog.Mutable setUpstreamQuarkusCoreVersion(String upstreamQuarkusCore) {
        this.upstreamQuarkusCore = upstreamQuarkusCore;
        return this;
    }

    @Override
    @JsonDeserialize(contentAs = JsonExtensionOrigin.class)
    public List<ExtensionOrigin> getDerivedFrom() {
        return derivedFrom == null ? Collections.emptyList() : derivedFrom;
    }

    public ExtensionCatalog.Mutable setDerivedFrom(List<ExtensionOrigin> origins) {
        this.derivedFrom = origins;
        return this;
    }

    @Override
    @JsonDeserialize(contentAs = JsonExtension.class)
    public List<Extension> getExtensions() {
        return extensions == null ? Collections.emptyList() : extensions;
    }

    public ExtensionCatalog.Mutable setExtensions(List<Extension> extensions) {
        this.extensions = extensions;
        return this;
    }

    public ExtensionCatalog.Mutable addExtension(Extension e) {
        if (extensions == null) {
            extensions = new ArrayList<>();
        }
        extensions.add(e);
        return this;
    }

    @Override
    @JsonDeserialize(contentAs = JsonCategory.class)
    public List<Category> getCategories() {
        return categories == null ? Collections.emptyList() : categories;
    }

    public ExtensionCatalog.Mutable setCategories(List<Category> categories) {
        this.categories = categories;
        return this;
    }

    public ExtensionCatalog.Mutable addCategory(Category c) {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        categories.add(c);
        return this;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata == null ? Collections.emptyMap() : metadata;
    }

    public ExtensionCatalog.Mutable setMetadata(Map<String, Object> newValues) {
        if (newValues != Collections.EMPTY_MAP) { // don't keep the empty map
            metadata = newValues;
        }
        return this;
    }

    public ExtensionCatalog.Mutable setMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }

    public ExtensionCatalog.Mutable removeMetadata(String key) {
        if (metadata != null) {
            metadata.remove(key);
        }
        return this;
    }

    @Override
    public ExtensionCatalog build() {
        return this;
    }

    @Override
    public ExtensionCatalog.Mutable mutable() {
        return this;
    }
}
