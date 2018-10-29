package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

public final class ResourceBundleBuildItem extends MultiBuildItem {

    private final String bundleName;

    public ResourceBundleBuildItem(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleName() {
        return bundleName;
    }
}
