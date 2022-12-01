package io.quarkus.resteasy.reactive.spi;

public final class ReaderInterceptorBuildItem extends AbstractInterceptorBuildItem {

    protected ReaderInterceptorBuildItem(AbstractInterceptorBuildItem.Builder<?, ?> builder) {
        super(builder);
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<ReaderInterceptorBuildItem, Builder> {

        public Builder(String className) {
            super(className);
        }

        public ReaderInterceptorBuildItem build() {
            return new ReaderInterceptorBuildItem(this);
        }
    }
}
