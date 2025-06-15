package io.quarkus.devui.spi.buildtime;

import java.util.List;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;
import io.quarkus.devui.spi.DevUIContent;

/**
 * Static Content generated at build time This is used to generate components that will be available in Dev UI, but
 * generated during build. This contains the final content (no more generation) and will be served as is
 */
public final class StaticContentBuildItem extends AbstractDevUIBuildItem {

    private final List<DevUIContent> content;

    public StaticContentBuildItem(List<DevUIContent> content) {
        super();
        this.content = content;
    }

    public StaticContentBuildItem(String customIdentifier, List<DevUIContent> content) {
        super(customIdentifier);
        this.content = content;
    }

    public List<DevUIContent> getContent() {
        return content;
    }

}
