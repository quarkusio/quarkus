package io.quarkus.resteasy.reactive.server.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * When an {@link Exception} of this type is thrown and no {@code jakarta.ws.rs.ext.ExceptionMapper} exists,
 * then RESTEasy Reactive will attempt to locate an {@code ExceptionMapper} for the cause of the Exception.
 */
public final class UnwrappedExceptionBuildItem extends MultiBuildItem {

    private final String throwableClassName;

    public UnwrappedExceptionBuildItem(String throwableClassName) {
        this.throwableClassName = throwableClassName;
    }

    public UnwrappedExceptionBuildItem(Class<? extends Throwable> throwableClassName) {
        this.throwableClassName = throwableClassName.getName();
    }

    @Deprecated(forRemoval = true)
    public Class<? extends Throwable> getThrowableClass() {
        try {
            return (Class<? extends Throwable>) Class.forName(throwableClassName, false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getThrowableClassName() {
        return throwableClassName;
    }
}
