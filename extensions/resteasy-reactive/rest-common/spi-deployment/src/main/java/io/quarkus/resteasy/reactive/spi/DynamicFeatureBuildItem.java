package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

// TODO: we should really provide a better alternative than this...
public final class DynamicFeatureBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final boolean registerAsBean;

    public DynamicFeatureBuildItem(String className) {
        this(className, true);
    }

    public DynamicFeatureBuildItem(String className, boolean registerAsBean) {
        this.className = className;
        this.registerAsBean = registerAsBean;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public boolean isRegisterAsBean() {
        return registerAsBean;
    }
}
