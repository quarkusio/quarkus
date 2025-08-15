package io.quarkus.devui.spi.page;

/**
 * Add a setting page to the Dev UI. (Tab in the setting screen)
 */
public final class SettingPageBuildItem extends AbstractPageBuildItem {

    public SettingPageBuildItem() {
        super();
    }

    public SettingPageBuildItem(PageBuilder... pageBuilder) {
        super(pageBuilder);
    }

    public SettingPageBuildItem(String customIdentifier, PageBuilder... pageBuilder) {
        super(customIdentifier, pageBuilder);
    }
}
