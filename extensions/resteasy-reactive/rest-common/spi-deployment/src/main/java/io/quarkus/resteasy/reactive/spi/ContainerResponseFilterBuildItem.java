package io.quarkus.resteasy.reactive.spi;

import org.jboss.jandex.MethodInfo;

public final class ContainerResponseFilterBuildItem extends AbstractInterceptorBuildItem {

    private final MethodInfo filterSourceMethod;
    private final boolean cancellable;

    protected ContainerResponseFilterBuildItem(ContainerResponseFilterBuildItem.Builder builder) {
        super(builder);
        this.filterSourceMethod = builder.filterSourceMethod;
        this.cancellable = builder.cancellable;
    }

    public MethodInfo getFilterSourceMethod() {
        return filterSourceMethod;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<ContainerResponseFilterBuildItem, Builder> {

        private MethodInfo filterSourceMethod = null;
        private boolean cancellable = true;

        public Builder(String className) {
            super(className);
        }

        public Builder setFilterSourceMethod(MethodInfo filterSourceMethod) {
            this.filterSourceMethod = filterSourceMethod;
            return this;
        }

        public Builder setCancellable(boolean cancellable) {
            this.cancellable = cancellable;
            return this;
        }

        public ContainerResponseFilterBuildItem build() {
            return new ContainerResponseFilterBuildItem(this);
        }
    }
}
