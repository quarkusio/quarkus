package io.quarkus.devui.spi.page;

import java.util.Arrays;
import java.util.List;

/**
 * Add a menu (or section) to the Dev UI.
 */
public final class MenuPageBuildItem extends AbstractPageBuildItem {

    private final List<PageBuilder> pageBuilders;

    public MenuPageBuildItem(PageBuilder... pageBuilder) {
        super();
        this.pageBuilders = Arrays.asList(pageBuilder);
    }

    public MenuPageBuildItem(String customIdentifier, PageBuilder... pageBuilder) {
        super(customIdentifier);
        this.pageBuilders = Arrays.asList(pageBuilder);
    }

    public List<PageBuilder> getPages() {
        return this.pageBuilders;
    }

}
