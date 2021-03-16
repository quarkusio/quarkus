package io.quarkus.vertx.http.deployment.devmode.console;

import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds all dev template variants found.
 */
public final class DevTemplateVariantsBuildItem extends SimpleBuildItem {

    private final Map<String, List<String>> variants;

    public DevTemplateVariantsBuildItem(Map<String, List<String>> variants) {
        this.variants = variants;
    }

    public Map<String, List<String>> getVariants() {
        return variants;
    }

}
