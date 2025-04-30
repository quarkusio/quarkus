package io.quarkus.resteasy.reactive.spi;

import org.jboss.jandex.MethodInfo;

public final class ContainerResponseFilterBuildItem extends AbstractInterceptorBuildItem {

    private final MethodInfo filterSourceMethod;

    protected ContainerResponseFilterBuildItem(ContainerResponseFilterBuildItem.Builder builder) {
        super(builder);
        this.filterSourceMethod = builder.filterSourceMethod;
    }

    public MethodInfo getFilterSourceMethod() {
        return filterSourceMethod;
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<ContainerResponseFilterBuildItem, Builder> {

        private MethodInfo filterSourceMethod = null;

        public Builder(String className) {
            super(className);
        }

        public Builder setFilterSourceMethod(MethodInfo filterSourceMethod) {
            this.filterSourceMethod = filterSourceMethod;
            return this;
        }

        public ContainerResponseFilterBuildItem build() {
            return new ContainerResponseFilterBuildItem(this);
        }
    }
}
