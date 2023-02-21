package io.quarkus.devui.spi.page;

import java.util.Arrays;
import java.util.List;

/**
 * Add a footer tab to the Dev UI.
 */
public final class FooterPageBuildItem extends AbstractPageBuildItem {

    private final List<PageBuilder> pageBuilders;

    public FooterPageBuildItem(String extensionName, PageBuilder... pageBuilder) {
        super(extensionName);
        this.pageBuilders = Arrays.asList(pageBuilder);
    }

    public List<PageBuilder> getPages() {
        return this.pageBuilders;
    }

}
