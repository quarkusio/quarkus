package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class CookieParamExtractor implements ParameterExtractor {

    private final String name;

    public CookieParamExtractor(String name) {
        this.name = name;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getCookieParameter(name);
    }
}
