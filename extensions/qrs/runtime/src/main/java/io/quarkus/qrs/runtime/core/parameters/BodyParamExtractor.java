package io.quarkus.qrs.runtime.core.parameters;

import io.quarkus.qrs.runtime.core.QrsRequestContext;

public class BodyParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(QrsRequestContext context) {
        return context.getRequestEntity();
    }

}
