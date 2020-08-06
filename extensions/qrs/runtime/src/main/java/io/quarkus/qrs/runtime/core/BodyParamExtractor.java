package io.quarkus.qrs.runtime.core;

public class BodyParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(QrsRequestContext context) {
        return context.getRequestEntity();
    }

}