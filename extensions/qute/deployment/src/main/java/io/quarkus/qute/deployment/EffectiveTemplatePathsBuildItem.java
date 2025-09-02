package io.quarkus.qute.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.qute.runtime.QuteConfig;

/**
 * This build item represents all template paths of an application.
 * <p>
 * If {@link QuteConfig.DuplicitTemplatesStrategy#PRIORITIZE} is used then duplicit template paths with lower priority are not
 * included.
 */
public final class EffectiveTemplatePathsBuildItem extends SimpleBuildItem {

    private final List<TemplatePathBuildItem> templatePaths;

    EffectiveTemplatePathsBuildItem(List<TemplatePathBuildItem> templatePaths) {
        this.templatePaths = templatePaths;
    }

    public List<TemplatePathBuildItem> getTemplatePaths() {
        return templatePaths;
    }

}
