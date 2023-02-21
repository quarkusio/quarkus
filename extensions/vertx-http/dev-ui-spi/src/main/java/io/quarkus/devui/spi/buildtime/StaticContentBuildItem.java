package io.quarkus.devui.spi.buildtime;

import java.util.List;

import io.quarkus.devui.deployment.spi.DevUIContent;
import io.quarkus.devui.spi.AbstractDevUIBuildItem;

/**
 * Static Content generated at build time
 *
 * This is used to generate components that will be available in Dev UI, but generated during build.
 * This contains the final content (no more generation) and will be served as is
 */
public final class StaticContentBuildItem extends AbstractDevUIBuildItem {

    private final List<DevUIContent> content;

    public StaticContentBuildItem(String extensionName, List<DevUIContent> content) {
        super(extensionName);
        this.content = content;
    }

    public List<DevUIContent> getContent() {
        return content;
    }

}