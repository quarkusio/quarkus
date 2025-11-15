package io.quarkus.devui.spi.page;

/**
 * Add a unlisted page to the Dev UI. Page that is not linked from anywhere in the Dashboard, but reachable if you know the url
 */
public final class UnlistedPageBuildItem extends AbstractPageBuildItem {

    public UnlistedPageBuildItem() {
        super();
    }

    public UnlistedPageBuildItem(PageBuilder... pageBuilder) {
        super(pageBuilder);
    }

    public UnlistedPageBuildItem(String customIdentifier, PageBuilder... pageBuilder) {
        super(customIdentifier, pageBuilder);
    }
}
