package io.quarkus.devui.spi.page;

import java.util.Arrays;
import java.util.List;

/**
 * Add a menu (or section) to the Dev UI.
 */
public final class MenuPageBuildItem extends AbstractPageBuildItem {

    private final List<PageBuilder> pageBuilders;

    public MenuPageBuildItem(String extensionName, PageBuilder... pageBuilder) {
        super(extensionName);
        this.pageBuilders = Arrays.asList(pageBuilder);
    }

    public List<PageBuilder> getPages() {
        return this.pageBuilders;
    }

}
