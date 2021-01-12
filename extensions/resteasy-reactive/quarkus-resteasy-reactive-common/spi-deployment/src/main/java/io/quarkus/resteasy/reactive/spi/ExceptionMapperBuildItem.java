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

    private ExceptionMapperBuildItem(Builder builder) {
        this.className = builder.className;
        this.handledExceptionName = builder.handledExceptionName;
        this.priority = builder.priority;
        this.registerAsBean = builder.registerAsBean;
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

    public static class Builder {
        private final String className;
        private final String handledExceptionName;

        private Integer priority;
        private boolean registerAsBean = true;

        public Builder(String className, String handledExceptionName) {
            this.className = className;
            this.handledExceptionName = handledExceptionName;
        }

        public Builder setPriority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public Builder setRegisterAsBean(boolean registerAsBean) {
            this.registerAsBean = registerAsBean;
            return this;
        }

        public ExceptionMapperBuildItem build() {
            return new ExceptionMapperBuildItem(this);
        }
    }
}
