package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ParamConverterBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final int priority;
    private final boolean registerAsBean;

    public ParamConverterBuildItem(String className, int priority, boolean registerAsBean) {
        this.className = className;
        this.priority = priority;
        this.registerAsBean = registerAsBean;
    }

    public String getClassName() {
        return className;
    }

    public Integer getPriority() {
        return priority;
    }

    @Override
    public boolean isRegisterAsBean() {
        return registerAsBean;
    }
}
