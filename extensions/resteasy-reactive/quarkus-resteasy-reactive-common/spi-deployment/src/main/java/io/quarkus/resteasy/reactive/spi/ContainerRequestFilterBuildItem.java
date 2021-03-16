package io.quarkus.resteasy.reactive.spi;

public final class ContainerRequestFilterBuildItem extends AbstractInterceptorBuildItem {

    private final boolean preMatching;

    protected ContainerRequestFilterBuildItem(Builder builder) {
        super(builder);
        this.preMatching = builder.preMatching;
    }

    public ContainerRequestFilterBuildItem(String className) {
        super(className);
        this.preMatching = false;
    }

    public boolean isPreMatching() {
        return preMatching;
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<ContainerRequestFilterBuildItem, Builder> {
        boolean preMatching = false;

        public Builder(String className) {
            super(className);
        }

        public Builder setPreMatching(Boolean preMatching) {
            this.preMatching = preMatching;
            return this;
        }

        public ContainerRequestFilterBuildItem build() {
            return new ContainerRequestFilterBuildItem(this);
        }
    }
}
