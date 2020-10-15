package io.quarkus.rest.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ContainerRequestFilterBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final Integer priority;
    private final Boolean preMatching;

    private final boolean registerAsBean;

    // TODO: use builder

    public ContainerRequestFilterBuildItem(String className) {
        this(className, null, null, true);
    }

    public ContainerRequestFilterBuildItem(String className, boolean registerAsBean) {
        this(className, null, null, registerAsBean);
    }

    public ContainerRequestFilterBuildItem(String className, Integer priority, Boolean preMatching) {
        this(className, priority, preMatching, true);
    }

    public ContainerRequestFilterBuildItem(String className, Integer priority, Boolean preMatching, boolean registerAsBean) {
        this.className = className;
        this.priority = priority;
        this.preMatching = preMatching;
        this.registerAsBean = registerAsBean;
    }

    public String getClassName() {
        return className;
    }

    public Integer getPriority() {
        return priority;
    }

    public Boolean getPreMatching() {
        return preMatching;
    }

    public boolean isRegisterAsBean() {
        return registerAsBean;
    }
}
