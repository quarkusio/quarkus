package io.quarkus.qute;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public final class Futures {

    private Futures() {
    }

    public static <T> CompletableFuture<T> failure(Throwable t) {
        CompletableFuture<T> failure = new CompletableFuture<>();
        failure.completeExceptionally(t);
        return failure;
    }

    public static EvaluatedParams evaluateParams(EvalContext context) {
        List<Expression> params = context.getParams();
        if (params.size() == 1) {
            return new EvaluatedParams(context.evaluate(params.get(0)));
        }
        CompletableFuture<?>[] results = new CompletableFuture<?>[params.size()];
        int i = 0;
        Iterator<Expression> it = params.iterator();
        while (it.hasNext()) {
            results[i++] = context.evaluate(it.next()).toCompletableFuture();
        }
        return new EvaluatedParams(CompletableFuture.allOf(results), results);
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
                Map<String, Object> paramValues = new HashMap<>();
                int j = 0;
                try {
                    for (Entry<String, Expression> entry : parameters.entrySet()) {
                        paramValues.put(entry.getKey(), results[j++].get());
                    }
                    result.complete(paramValues);
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }

            }
        });
        return result;
    }

    @SuppressWarnings("rawtypes")
    public static final class EvaluatedParams {

        public final CompletionStage stage;
        private final CompletableFuture<?>[] results;

        EvaluatedParams(CompletionStage stage) {
            this(stage, null);
        }

        EvaluatedParams(CompletionStage stage, CompletableFuture[] results) {
            this.stage = stage;
            this.results = results;
        }

        public Object getResult(int index) throws InterruptedException, ExecutionException {
            return results[index].get();
        }

    }
}
