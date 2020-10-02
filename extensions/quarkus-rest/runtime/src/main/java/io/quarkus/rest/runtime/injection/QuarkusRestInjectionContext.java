package io.quarkus.rest.runtime.injection;

public interface QuarkusRestInjectionContext {
    public Object getHeader(String name, boolean single);

    public Object getQueryParameter(String name, boolean single, boolean encoded);

    public String getPathParameter(String name, boolean encoded);

    public Object getMatrixParameter(String name, boolean single, boolean encoded);

    public String getCookieParameter(String name);

    public Object getFormParameter(String name, boolean single);
}
