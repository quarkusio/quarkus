package io.quarkus.qrs.runtime.core;

public class BodyParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(RequestContext context) {
        return context.getRequestEntity();
    }

}