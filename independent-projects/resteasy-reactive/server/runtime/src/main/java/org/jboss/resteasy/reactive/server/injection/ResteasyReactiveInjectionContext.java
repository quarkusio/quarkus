package org.jboss.resteasy.reactive.server.injection;

public interface ResteasyReactiveInjectionContext {
    Object getHeader(String name, boolean single);

    Object getQueryParameter(String name, boolean single, boolean encoded, String separator);

    String getPathParameter(String name, boolean encoded);

    Object getMatrixParameter(String name, boolean single, boolean encoded);

    String getCookieParameter(String name);

    Object getFormParameter(String name, boolean single, boolean encoded);

    <T> T unwrap(Class<T> theType);
}
