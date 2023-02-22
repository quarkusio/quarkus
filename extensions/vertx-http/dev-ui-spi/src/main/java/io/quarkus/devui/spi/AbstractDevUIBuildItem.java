package io.quarkus.devui.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * For All DEV UI Build Item, we need to distinguish between the extensions, and the internal usage of Dev UI
 */
public abstract class AbstractDevUIBuildItem extends MultiBuildItem {
    private static final String SPACE = " ";
    private static final String DASH = "-";

    protected final String extensionName;

    public AbstractDevUIBuildItem(String extensionName) {
        this.extensionName = extensionName;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public String getExtensionPathName() {
        return extensionName.toLowerCase().replaceAll(SPACE, DASH);
    }

    public boolean isInternal() {
        return this.extensionName == DEV_UI;
    }

    public static final String DEV_UI = "DevUI";
}
