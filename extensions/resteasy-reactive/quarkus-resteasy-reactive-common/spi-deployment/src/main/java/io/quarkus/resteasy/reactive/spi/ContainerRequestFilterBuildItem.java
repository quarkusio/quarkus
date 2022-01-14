package io.quarkus.resteasy.reactive.spi;

public final class ContainerRequestFilterBuildItem extends AbstractInterceptorBuildItem {

    private final boolean preMatching;
    private final boolean nonBlockingRequired;
    private final boolean readBody;

    protected ContainerRequestFilterBuildItem(Builder builder) {
        super(builder);
        this.preMatching = builder.preMatching;
        this.nonBlockingRequired = builder.nonBlockingRequired;
        this.readBody = builder.readBody;
    }

    public ContainerRequestFilterBuildItem(String className) {
        super(className);
        this.preMatching = false;
        this.nonBlockingRequired = false;
        this.readBody = false;
    }

    public boolean isPreMatching() {
        return preMatching;
    }

    public boolean isNonBlockingRequired() {
        return nonBlockingRequired;
    }

    public boolean isReadBody() {
        return readBody;
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<ContainerRequestFilterBuildItem, Builder> {
        boolean preMatching = false;
        boolean nonBlockingRequired = false;
        boolean readBody = false;

        public Builder(String className) {
            super(className);
        }

        public Builder setPreMatching(boolean preMatching) {
            this.preMatching = preMatching;
            return this;
        }

        public Builder setNonBlockingRequired(boolean nonBlockingRequired) {
            this.nonBlockingRequired = nonBlockingRequired;
            return this;
        }

        public Builder setReadBody(boolean readBody) {
            this.readBody = readBody;
            return this;
        }

        public ContainerRequestFilterBuildItem build() {
            return new ContainerRequestFilterBuildItem(this);
        }
    }
}
