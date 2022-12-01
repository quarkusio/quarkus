package io.quarkus.vertx.http.deployment.devmode.console;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Results;
import io.quarkus.qute.ValueResolver;
import io.vertx.core.MultiMap;

public class MultiMapValueResolver implements ValueResolver {

    public boolean appliesTo(EvalContext context) {
        return ValueResolver.matchClass(context, MultiMap.class);
    }

    @Override
    public CompletionStage<Object> resolve(EvalContext context) {
        MultiMap multiMap = (MultiMap) context.getBase();
        switch (context.getName()) {
            case "names":
                return CompletableFuture.completedFuture(multiMap.names());
            case "size":
                return CompletableFuture.completedFuture(multiMap.size());
            case "empty":
            case "isEmpty":
                return CompletableFuture.completedFuture(multiMap.isEmpty());
            case "get":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletableFuture.completedFuture(multiMap.get((String) k));
                    });
                }
            case "getAll":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletableFuture.completedFuture(multiMap.getAll((String) k));
                    });
                }
            case "contains":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletableFuture.completedFuture(multiMap.contains((String) k));
                    });
                }
            default:
                return multiMap.contains(context.getName())
                        ? CompletableFuture.completedFuture(multiMap.get(context.getName()))
                        : Results.notFound(context);
        }
    }

}
