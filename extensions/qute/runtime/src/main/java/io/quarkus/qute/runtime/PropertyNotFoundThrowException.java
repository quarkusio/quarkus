package io.quarkus.qute.runtime;

import io.quarkus.qute.Expression;
import io.quarkus.qute.ResultMapper;
import io.quarkus.qute.Results;
import io.quarkus.qute.Results.NotFound;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateNode.Origin;

class PropertyNotFoundThrowException implements ResultMapper {

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean appliesTo(Origin origin, Object result) {
        return Results.isNotFound(result);
    }

    @Override
    public String map(Object result, Expression expression) {
        String propertyMessage;
        if (result instanceof NotFound) {
            propertyMessage = ((NotFound) result).asMessage();
        } else {
            propertyMessage = "Property not found";
        }
        throw new TemplateException(expression.getOrigin(),
                String.format("%s in expression {%s} in template %s on line %s", propertyMessage, expression.toOriginalString(),
                        expression.getOrigin().getTemplateId(), expression.getOrigin().getLine()));
    }

}
