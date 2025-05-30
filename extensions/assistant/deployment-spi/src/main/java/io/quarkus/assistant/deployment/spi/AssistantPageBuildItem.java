package io.quarkus.assistant.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.devui.spi.page.PageBuilder;

public final class AssistantPageBuildItem extends MultiBuildItem {
    private final PageBuilder pageBuilder;
    private final boolean alwaysVisible;

    public AssistantPageBuildItem(PageBuilder pageBuilder) {
        this(pageBuilder, false);
    }

    public AssistantPageBuildItem(PageBuilder pageBuilder, boolean alwaysVisible) {
        this.pageBuilder = pageBuilder;
        this.alwaysVisible = alwaysVisible;
    }

    public PageBuilder getPageBuilder() {
        return pageBuilder;
    }

    public boolean isAlwaysVisible() {
        return alwaysVisible;
    }
}
