package io.quarkus.resteasy.reactive.server.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * When an {@link Exception} of this type is thrown and no {@code jakarta.ws.rs.ext.ExceptionMapper} exists,
 * then RESTEasy Reactive will attempt to locate an {@code ExceptionMapper} for the cause of the Exception.
 * <p>
 * When {@code always} is {@code true}, unwrapping occurs even if an {@code ExceptionMapper} exists for one
 * of the class super classes, but not if the exception is directly mapped.
 */
public final class UnwrappedExceptionBuildItem extends MultiBuildItem {

    private final String throwableClassName;
    private final boolean always;

    public UnwrappedExceptionBuildItem(String throwableClassName) {
        this(throwableClassName, false);
    }

    public UnwrappedExceptionBuildItem(String throwableClassName, boolean always) {
        this.throwableClassName = throwableClassName;
        this.always = always;
    }

    public UnwrappedExceptionBuildItem(Class<? extends Throwable> throwableClassName) {
        this(throwableClassName.getName(), false);
    }

    public UnwrappedExceptionBuildItem(Class<? extends Throwable> throwableClassName, boolean always) {
        this(throwableClassName.getName(), always);
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

    public boolean isAlways() {
        return always;
    }
}
