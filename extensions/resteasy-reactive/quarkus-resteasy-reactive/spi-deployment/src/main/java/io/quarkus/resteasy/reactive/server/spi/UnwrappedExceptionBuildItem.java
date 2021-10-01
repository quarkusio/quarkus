package io.quarkus.resteasy.reactive.server.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * When an Exception of this type is thrown and no {@code javax.ws.rs.ext.ExceptionMapper} exists,
 * then RESTEasy Reactive will attempt to locate an {@code ExceptionMapper} for the cause of the Exception.
 */
public final class UnwrappedExceptionBuildItem extends MultiBuildItem {

    private final Class<? extends Throwable> throwableClass;

    public UnwrappedExceptionBuildItem(Class<? extends Throwable> throwableClass) {
        this.throwableClass = throwableClass;
    }

    public Class<? extends Throwable> getThrowableClass() {
        return throwableClass;
    }
}
