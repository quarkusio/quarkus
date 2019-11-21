package io.quarkus.qute.deployment;

import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class TemplateVariantsBuildItem extends SimpleBuildItem {

    private final Map<String, List<String>> variants;

    public TemplateVariantsBuildItem(Map<String, List<String>> variants) {
        this.variants = variants;
    }

    public Map<String, List<String>> getVariants() {
        return variants;
    }

}
