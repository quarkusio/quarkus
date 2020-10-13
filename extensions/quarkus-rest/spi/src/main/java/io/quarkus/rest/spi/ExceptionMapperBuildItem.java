package io.quarkus.rest.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ExceptionMapperBuildItem extends MultiBuildItem {

    private final String className;
    private final Integer priority;
    private final String handledExceptionName;

    public ExceptionMapperBuildItem(String className, String handledExceptionName) {
        this(className, handledExceptionName, null);
    }

    public ExceptionMapperBuildItem(String className, String handledExceptionName, Integer priority) {
        this.className = className;
        this.priority = priority;
        this.handledExceptionName = handledExceptionName;
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
}
