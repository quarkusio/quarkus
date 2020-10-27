package io.quarkus.rest.server.runtime.core.parameters;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

public class BodyParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        return context.getRequestEntity();
    }

}
