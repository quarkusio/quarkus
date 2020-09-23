package io.quarkus.rest.runtime.injection;

public interface QuarkusRestInjectionContext {
    public String getHeader(String name);

    public String getQueryParameter(String name);

    public String getPathParameter(String name);

    public String getMatrixParameter(String name);

    public String getCookieParameter(String name);

    public String getFormParameter(String name);
}
