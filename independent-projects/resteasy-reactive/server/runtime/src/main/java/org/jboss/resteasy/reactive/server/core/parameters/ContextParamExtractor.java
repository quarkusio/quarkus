package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class ContextParamExtractor implements ParameterExtractor {

    private final Class<?> type;

    public ContextParamExtractor(String type) {
        try {
            this.type = Class.forName(type, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ContextParamExtractor(Class<?> type) {
        this.type = type;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getContextParameter(type);
    }

}
