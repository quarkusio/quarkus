package io.quarkus.resteasy.reactive.server.spi;

import org.jboss.resteasy.reactive.server.ExceptionUnwrapStrategy;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * When an {@link Exception} of this type is thrown and no {@code jakarta.ws.rs.ext.ExceptionMapper} exists,
 * then RESTEasy Reactive will attempt to locate an {@code ExceptionMapper} for the cause of the Exception.
 * <p>
 * The unwrapping behavior is controlled by the {@link ExceptionUnwrapStrategy}.
 */
public final class UnwrappedExceptionBuildItem extends MultiBuildItem {

    private final String throwableClassName;
    private final ExceptionUnwrapStrategy strategy;

    public UnwrappedExceptionBuildItem(String throwableClassName) {
        this(throwableClassName, ExceptionUnwrapStrategy.UNWRAP_IF_NO_MATCH);
    }

    public UnwrappedExceptionBuildItem(String throwableClassName, ExceptionUnwrapStrategy strategy) {
        this.throwableClassName = throwableClassName;
        this.strategy = strategy;
    }

    public UnwrappedExceptionBuildItem(Class<? extends Throwable> throwableClassName) {
        this(throwableClassName.getName(), ExceptionUnwrapStrategy.UNWRAP_IF_NO_MATCH);
    }

    public UnwrappedExceptionBuildItem(Class<? extends Throwable> throwableClassName, ExceptionUnwrapStrategy strategy) {
        this(throwableClassName.getName(), strategy);
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

    public ExceptionUnwrapStrategy getStrategy() {
        return strategy;
    }
}
