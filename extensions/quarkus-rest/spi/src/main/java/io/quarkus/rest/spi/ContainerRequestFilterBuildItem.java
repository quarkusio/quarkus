package io.quarkus.rest.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ContainerRequestFilterBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final Integer priority;
    private final Boolean preMatching;

    private final boolean registerAsBean;

    public ContainerRequestFilterBuildItem(String className) {
        this(className, null, null, true);
    }

    private ContainerRequestFilterBuildItem(String className, Integer priority, Boolean preMatching, boolean registerAsBean) {
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

    public static final class Builder {
        private final String className;

        private Integer priority;
        private Boolean preMatching;
        private boolean registerAsBean = true;

        public Builder(String className) {
            this.className = className;
        }

        public Builder setPriority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public Builder setPreMatching(Boolean preMatching) {
            this.preMatching = preMatching;
            return this;
        }

        public Builder setRegisterAsBean(boolean registerAsBean) {
            this.registerAsBean = registerAsBean;
            return this;
        }

        public ContainerRequestFilterBuildItem build() {
            return new ContainerRequestFilterBuildItem(className, priority, preMatching, registerAsBean);
        }
    }
}
