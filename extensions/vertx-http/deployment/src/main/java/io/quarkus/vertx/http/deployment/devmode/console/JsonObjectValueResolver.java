package io.quarkus.vertx.http.deployment.devmode.console;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Results;
import io.quarkus.qute.ValueResolver;
import io.vertx.core.json.JsonObject;

public class JsonObjectValueResolver implements ValueResolver {

    public boolean appliesTo(EvalContext context) {
        return ValueResolver.matchClass(context, JsonObject.class);
    }

    @Override
    public CompletionStage<Object> resolve(EvalContext context) {
        JsonObject jsonObject = (JsonObject) context.getBase();
        switch (context.getName()) {
            case "fieldNames":
            case "fields":
                return CompletableFuture.completedFuture(jsonObject.fieldNames());
            case "size":
                return CompletableFuture.completedFuture(jsonObject.size());
            case "empty":
            case "isEmpty":
                return CompletableFuture.completedFuture(jsonObject.isEmpty());
            case "get":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletableFuture.completedFuture(jsonObject.getValue((String) k));
                    });
                }
            case "containsKey":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletableFuture.completedFuture(jsonObject.containsKey((String) k));
                    });
                }
            default:
                return jsonObject.containsKey(context.getName())
                        ? CompletableFuture.completedFuture(jsonObject.getValue(context.getName()))
                        : Results.notFound(context);
        }
    }

}
