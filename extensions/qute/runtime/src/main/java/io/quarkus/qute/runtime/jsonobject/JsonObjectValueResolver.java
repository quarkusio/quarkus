package io.quarkus.qute.runtime.jsonobject;

import java.util.concurrent.CompletionStage;

import io.quarkus.qute.CompletedStage;
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Results;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.ValueResolvers;
import io.vertx.core.json.JsonObject;

/**
 * A value resolver for {@link JsonObject}.
 */
@EngineConfiguration
public class JsonObjectValueResolver implements ValueResolver {

    @Override
    public boolean appliesTo(EvalContext context) {
        return ValueResolvers.matchClass(context, JsonObject.class);
    }

    @Override
    public CompletionStage<Object> resolve(EvalContext context) {

        JsonObject jsonObject = (JsonObject) context.getBase();
        switch (context.getName()) {
            case "fieldNames":
            case "fields":
                return CompletedStage.of(jsonObject.fieldNames());
            case "size":
                return CompletedStage.of(jsonObject.size());
            case "empty":
            case "isEmpty":
                return CompletedStage.of(jsonObject.isEmpty());
            case "get":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        if (k == null || Results.isNotFound(k)) {
                            return Results.notFound(context);
                        }
                        return CompletedStage.of(jsonObject.getValue(k.toString()));
                    });
                }
            case "containsKey":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        if (k == null || Results.isNotFound(k)) {
                            return Results.notFound(context);
                        }
                        return CompletedStage.of(jsonObject.containsKey(k.toString()));
                    });
                }
            default:
                return jsonObject.containsKey(context.getName())
                        ? CompletedStage.of(jsonObject.getValue(context.getName()))
                        : Results.notFound(context);
        }
    }
}
