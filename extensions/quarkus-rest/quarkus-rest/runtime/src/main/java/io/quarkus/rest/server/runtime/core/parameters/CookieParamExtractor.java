package io.quarkus.rest.server.runtime.core.parameters;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

public class CookieParamExtractor implements ParameterExtractor {

    private final String name;

    public CookieParamExtractor(String name) {
        this.name = name;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        return context.getCookieParameter(name);
    }
}
