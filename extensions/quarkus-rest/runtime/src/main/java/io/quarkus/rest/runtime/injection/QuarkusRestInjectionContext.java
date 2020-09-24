package io.quarkus.rest.runtime.injection;

public interface QuarkusRestInjectionContext {
    public Object getHeader(String name, boolean single);

    public Object getQueryParameter(String name, boolean single);

    public String getPathParameter(String name);

    public Object getMatrixParameter(String name, boolean single);

    public String getCookieParameter(String name);

    public Object getFormParameter(String name, boolean single);
}
