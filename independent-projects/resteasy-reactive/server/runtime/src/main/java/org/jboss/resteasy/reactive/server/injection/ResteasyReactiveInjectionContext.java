package org.jboss.resteasy.reactive.server.injection;

public interface ResteasyReactiveInjectionContext {
    Object getHeader(String name, boolean single);

    Object getQueryParameter(String name, boolean single, boolean encoded, String separator);

    String getPathParameter(String name, boolean encoded);

    Object getMatrixParameter(String name, boolean single, boolean encoded);

    String getCookieParameter(String name);

    Object getFormParameter(String name, boolean single, boolean encoded);

    <T> T unwrap(Class<T> theType);

    /**
     * Gets the context parameter instance by type for predefined types, also calls unwrap, and CDI.
     *
     * @throws IllegalStateException if there is no such context object
     */
    <T> T getContextParameter(Class<T> type);

    /**
     * Gets the bean parameter instance by type via CDI, and registers a cleanup for it. This does not call __inject
     * on it, and it does not work for records (which cannot be gotten via CDI).
     *
     * @throws IllegalStateException if there is no such context object
     */
    <T> T getBeanParameter(Class<T> type);
}
