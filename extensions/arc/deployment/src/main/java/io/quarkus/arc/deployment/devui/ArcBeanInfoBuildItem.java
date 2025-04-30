package io.quarkus.arc.deployment.devui;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ArcBeanInfoBuildItem extends SimpleBuildItem {

    private final DevBeanInfos beanInfos;

    public ArcBeanInfoBuildItem(DevBeanInfos beanInfos) {
        this.beanInfos = beanInfos;
    }

    public DevBeanInfos getBeanInfos() {
        return beanInfos;
    }
}
