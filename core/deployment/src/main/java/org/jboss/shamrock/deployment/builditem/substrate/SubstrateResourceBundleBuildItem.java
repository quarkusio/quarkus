package org.jboss.shamrock.deployment.builditem.substrate;

import org.jboss.builder.item.MultiBuildItem;

public final class SubstrateResourceBundleBuildItem extends MultiBuildItem {

    private final String bundleName;

    public SubstrateResourceBundleBuildItem(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleName() {
        return bundleName;
    }
}
