package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class BodyParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getRequestEntity();
    }

}
