package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ExceptionMapperBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final Integer priority;
    private final String handledExceptionName;

    private final boolean registerAsBean;

    public ExceptionMapperBuildItem(String className, String handledExceptionName, Integer priority, boolean registerAsBean) {
        this.className = className;
        this.priority = priority;
        this.handledExceptionName = handledExceptionName;
        this.registerAsBean = registerAsBean;
    }

    public String getClassName() {
        return className;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getHandledExceptionName() {
        return handledExceptionName;
    }

    @Override
    public boolean isRegisterAsBean() {
        return registerAsBean;
    }
}
