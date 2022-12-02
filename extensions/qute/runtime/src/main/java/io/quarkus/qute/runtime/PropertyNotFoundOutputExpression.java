package io.quarkus.qute.runtime;

import io.quarkus.qute.Expression;
import io.quarkus.qute.ResultMapper;
import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateNode.Origin;

class PropertyNotFoundOutputOriginal implements ResultMapper {

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
        return "{" + expression.toOriginalString() + "}";
    }

}
