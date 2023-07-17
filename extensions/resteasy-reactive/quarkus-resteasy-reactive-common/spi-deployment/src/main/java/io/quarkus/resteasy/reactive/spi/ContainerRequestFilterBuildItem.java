package io.quarkus.resteasy.reactive.spi;

import org.jboss.jandex.MethodInfo;

public final class ContainerRequestFilterBuildItem extends AbstractInterceptorBuildItem {

    private final boolean preMatching;
    private final boolean nonBlockingRequired;
    private final boolean withFormRead;

    private final MethodInfo filterSourceMethod;

    protected ContainerRequestFilterBuildItem(Builder builder) {
        super(builder);
        this.preMatching = builder.preMatching;
        this.nonBlockingRequired = builder.nonBlockingRequired;
        this.withFormRead = builder.withFormRead;
        this.filterSourceMethod = builder.filterSourceMethod;
    }

    public ContainerRequestFilterBuildItem(String className) {
        super(className);
        this.preMatching = false;
        this.nonBlockingRequired = false;
        this.withFormRead = false;
        this.filterSourceMethod = null;
    }

    public boolean isPreMatching() {
        return preMatching;
    }

    public boolean isNonBlockingRequired() {
        return nonBlockingRequired;
    }

    public boolean isWithFormRead() {
        return withFormRead;
    }

    public MethodInfo getFilterSourceMethod() {
        return filterSourceMethod;
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<ContainerRequestFilterBuildItem, Builder> {
        boolean preMatching = false;
        boolean nonBlockingRequired = false;
        boolean withFormRead = false;

        MethodInfo filterSourceMethod = null;

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

        public Builder setWithFormRead(boolean withFormRead) {
            this.withFormRead = withFormRead;
            return this;
        }

        public Builder setFilterSourceMethod(MethodInfo filterSourceMethod) {
            this.filterSourceMethod = filterSourceMethod;
            return this;
        }

        public ContainerRequestFilterBuildItem build() {
            return new ContainerRequestFilterBuildItem(this);
        }
    }
}
