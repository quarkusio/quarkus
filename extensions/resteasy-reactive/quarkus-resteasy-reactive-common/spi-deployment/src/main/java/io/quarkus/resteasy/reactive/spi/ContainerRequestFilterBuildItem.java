package io.quarkus.resteasy.reactive.spi;

public final class ContainerRequestFilterBuildItem extends AbstractInterceptorBuildItem {

    private final boolean preMatching;
    private final boolean nonBlockingRequired;

    protected ContainerRequestFilterBuildItem(Builder builder) {
        super(builder);
        this.preMatching = builder.preMatching;
        this.nonBlockingRequired = builder.nonBlockingRequired;
    }

    public ContainerRequestFilterBuildItem(String className) {
        super(className);
        this.preMatching = false;
        this.nonBlockingRequired = false;
    }

    public boolean isPreMatching() {
        return preMatching;
    }

    public boolean isNonBlockingRequired() {
        return nonBlockingRequired;
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<ContainerRequestFilterBuildItem, Builder> {
        boolean preMatching = false;
        boolean nonBlockingRequired = false;

        public Builder(String className) {
            super(className);
        }

        public Builder setPreMatching(Boolean preMatching) {
            this.preMatching = preMatching;
            return this;
        }

        public Builder setNonBlockingRequired(Boolean nonBlockingRequired) {
            this.nonBlockingRequired = nonBlockingRequired;
            return this;
        }

        public ContainerRequestFilterBuildItem build() {
            return new ContainerRequestFilterBuildItem(this);
        }
    }
}
