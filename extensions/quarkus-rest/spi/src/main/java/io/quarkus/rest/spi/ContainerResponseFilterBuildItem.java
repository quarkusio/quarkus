package io.quarkus.rest.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ContainerResponseFilterBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final Integer priority;

    private final boolean registerAsBean;

    public ContainerResponseFilterBuildItem(String className, Integer priority, boolean registerAsBean) {
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
