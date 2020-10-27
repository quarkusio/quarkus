package io.quarkus.rest.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class ContainerResponseFilterBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final Integer priority;

    private final boolean registerAsBean;

    private ContainerResponseFilterBuildItem(String className, Integer priority, boolean registerAsBean) {
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

    public static final class Builder {
        private final String className;

        private Integer priority;
        private boolean registerAsBean = true;

        public Builder(String className) {
            this.className = className;
        }

        public Builder setPriority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public Builder setRegisterAsBean(boolean registerAsBean) {
            this.registerAsBean = registerAsBean;
            return this;
        }

        public ContainerResponseFilterBuildItem build() {
            return new ContainerResponseFilterBuildItem(className, priority, registerAsBean);
        }
    }
}
