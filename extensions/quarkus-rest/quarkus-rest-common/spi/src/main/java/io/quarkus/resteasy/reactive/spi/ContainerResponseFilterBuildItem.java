package io.quarkus.resteasy.reactive.spi;

public final class ContainerResponseFilterBuildItem extends AbstractInterceptorBuildItem {

    protected ContainerResponseFilterBuildItem(AbstractInterceptorBuildItem.Builder<?, ?> builder) {
        super(builder);
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<ContainerResponseFilterBuildItem, Builder> {

        public Builder(String className) {
            super(className);
        }

        public ContainerResponseFilterBuildItem build() {
            return new ContainerResponseFilterBuildItem(this);
        }
    }
}
