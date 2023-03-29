package io.quarkus.devui.spi.page;

/**
 * Add a footer tab to the Dev UI.
 */
public final class FooterPageBuildItem extends AbstractPageBuildItem {

    public FooterPageBuildItem() {
        super();
    }

    public FooterPageBuildItem(PageBuilder... pageBuilder) {
        super(pageBuilder);
    }

    public FooterPageBuildItem(String customIdentifier, PageBuilder... pageBuilder) {
        super(customIdentifier, pageBuilder);
    }
}
