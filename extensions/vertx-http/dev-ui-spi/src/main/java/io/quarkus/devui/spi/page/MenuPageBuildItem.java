package io.quarkus.devui.spi.page;

/**
 * Add a menu (or section) to the Dev UI.
 */
public final class MenuPageBuildItem extends AbstractPageBuildItem {

    public MenuPageBuildItem() {
        super();
    }

    public MenuPageBuildItem(PageBuilder... pageBuilder) {
        super(pageBuilder);
    }

    public MenuPageBuildItem(String customIdentifier, PageBuilder... pageBuilder) {
        super(customIdentifier, pageBuilder);
    }
}
