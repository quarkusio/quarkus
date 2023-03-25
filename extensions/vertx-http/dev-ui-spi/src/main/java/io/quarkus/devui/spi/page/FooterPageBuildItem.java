package io.quarkus.devui.spi.page;

import java.util.Arrays;
import java.util.List;

/**
 * Add a footer tab to the Dev UI.
 */
public final class FooterPageBuildItem extends AbstractPageBuildItem {

    private final List<PageBuilder> pageBuilders;

    public FooterPageBuildItem(PageBuilder... pageBuilder) {
        super();
        this.pageBuilders = Arrays.asList(pageBuilder);
    }

    public FooterPageBuildItem(String customIdentifier, PageBuilder... pageBuilder) {
        super(customIdentifier);
        this.pageBuilders = Arrays.asList(pageBuilder);
    }

    public List<PageBuilder> getPages() {
        return this.pageBuilders;
    }

}
