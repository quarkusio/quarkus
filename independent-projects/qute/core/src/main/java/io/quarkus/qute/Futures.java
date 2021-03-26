package io.quarkus.qute;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class Futures {

    static final CompletableFuture<Void> COMPLETED = CompletableFuture.completedFuture(null);

    private Futures() {
    }

    public static <T> CompletableFuture<T> failure(Throwable t) {
        CompletableFuture<T> failure = new CompletableFuture<>();
        failure.completeExceptionally(t);
        return failure;
    }

    @SuppressWarnings("unchecked")
    static CompletionStage<Map<String, Object>> evaluateParams(Map<String, Expression> parameters,
            ResolutionContext resolutionContext) {
        CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
        CompletableFuture<Object>[] results = new CompletableFuture[parameters.size()];
        int idx = 0;
        for (Entry<String, Expression> entry : parameters.entrySet()) {
            results[idx++] = resolutionContext.evaluate(entry.getValue()).toCompletableFuture();
        }
        CompletableFuture.allOf(results).whenComplete((v, t1) -> {
            if (t1 != null) {
                result.completeExceptionally(t1);
            } else {
                // Build a map from the params
                // IMPL NOTE: Keep the map mutable - it can be modified in UserTagSectionHelper 
                Map<String, Object> paramValues = new HashMap<>();
                int j = 0;
                try {
                    for (Entry<String, Expression> entry : parameters.entrySet()) {
                        paramValues.put(entry.getKey(), results[j++].get());
                    }
                    result.complete(paramValues);
                } catch (Throwable e) {
                    result.completeExceptionally(e);
                }

            }
        });
        return result;
    }
}
