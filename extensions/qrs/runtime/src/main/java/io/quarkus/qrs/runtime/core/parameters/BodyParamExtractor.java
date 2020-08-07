package io.quarkus.qrs.runtime.core.parameters;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.parameters.ParameterExtractor;

public class BodyParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(QrsRequestContext context) {
        return context.getRequestEntity();
    }

}