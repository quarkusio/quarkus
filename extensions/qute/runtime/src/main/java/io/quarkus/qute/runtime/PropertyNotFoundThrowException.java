package io.quarkus.qute.runtime;

import io.quarkus.qute.Expression;
import io.quarkus.qute.ResultMapper;
import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateNode.Origin;

class PropertyNotFoundThrowException implements ResultMapper {

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean appliesTo(Origin origin, Object result) {
        return result.equals(Result.NOT_FOUND);
    }

    @Override
    public String map(Object result, Expression expression) {
        throw new TemplateException(expression.getOrigin(),
                String.format("Property not found in expression {%s} in template %s on line %s", expression.toOriginalString(),
                        expression.getOrigin().getTemplateId(), expression.getOrigin().getLine()));
    }

}
