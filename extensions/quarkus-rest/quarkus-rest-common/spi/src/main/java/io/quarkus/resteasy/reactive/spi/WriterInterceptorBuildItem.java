package io.quarkus.resteasy.reactive.spi;

public final class WriterInterceptorBuildItem extends AbstractInterceptorBuildItem {

    protected WriterInterceptorBuildItem(AbstractInterceptorBuildItem.Builder<?, ?> builder) {
        super(builder);
    }

    public static final class Builder extends AbstractInterceptorBuildItem.Builder<WriterInterceptorBuildItem, Builder> {

        public Builder(String className) {
            super(className);
        }

        public WriterInterceptorBuildItem build() {
            return new WriterInterceptorBuildItem(this);
        }
    }
}
