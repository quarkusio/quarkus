package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonExtensionCatalog extends JsonExtensionOrigin implements ExtensionCatalog {

    private String quarkusCore;
    private String upstreamQuarkusCore;
    private List<ExtensionOrigin> derivedFrom;
    private List<Extension> extensions;
    private List<Category> categories;
    private Map<String, Object> metadata;

    @Override
    public String getQuarkusCoreVersion() {
        return quarkusCore;
    }

    public void setQuarkusCoreVersion(String quarkusCore) {
        this.quarkusCore = quarkusCore;
    }

    @Override
    public String getUpstreamQuarkusCoreVersion() {
        return upstreamQuarkusCore;
    }

    public void setUpstreamQuarkusCoreVersion(String upstreamQuarkusCore) {
        this.upstreamQuarkusCore = upstreamQuarkusCore;
    }

    @Override
    @JsonDeserialize(contentAs = JsonExtensionOrigin.class)
    public List<ExtensionOrigin> getDerivedFrom() {
        return derivedFrom == null ? Collections.emptyList() : derivedFrom;
    }

    public void setDerivedFrom(List<ExtensionOrigin> origins) {
        this.derivedFrom = origins;
    }

    @Override
    @JsonDeserialize(contentAs = JsonExtension.class)
    public List<Extension> getExtensions() {
        return extensions == null ? Collections.emptyList() : extensions;
    }

    public void setExtensions(List<Extension> extensions) {
        this.extensions = extensions;
    }

    public void addExtension(Extension e) {
        if (extensions == null) {
            extensions = new ArrayList<>();
        }
        extensions.add(e);
    }

    @Override
    @JsonDeserialize(contentAs = JsonCategory.class)
    public List<Category> getCategories() {
        return categories == null ? Collections.emptyList() : categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public void addCategory(Category c) {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        categories.add(c);
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata == null ? Collections.emptyMap() : metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
