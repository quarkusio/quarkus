package io.quarkus.qute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Futures {

    private Futures() {
    }

    static <T> Supplier<T> toSupplier(CompletableFuture<T> fu) {
        return new Supplier<T>() {
            @Override
            public T get() {
                try {
                    return fu.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new TemplateException(e);
                }
            }
        };
    }

    static CompletionStage<Map<String, Object>> evaluateParams(Map<String, Expression> parameters,
            ResolutionContext resolutionContext) {
        if (parameters.size() == 1) {
            // single param
            Entry<String, Expression> entry = parameters.entrySet().iterator().next();
            if (entry.getValue().isLiteral()) {
                // literal - no async computation needed
                return CompletedStage.of(singleValueMap(entry.getKey(), entry.getValue().getLiteral()));
            } else {
                // single non-literal param - just avoid CompletableFuture.allOf()
                return resolutionContext.evaluate(entry.getValue()).thenApply(v -> singleValueMap(entry.getKey(), v));
            }
        } else {
            // multiple params
            List<Entry<String, CompletableFuture<Object>>> asyncResults = new ArrayList<>(parameters.size());
            for (Entry<String, Expression> e : parameters.entrySet()) {
                if (!e.getValue().isLiteral()) {
                    asyncResults.add(Map.entry(e.getKey(), resolutionContext.evaluate(e.getValue()).toCompletableFuture()));
                }
            }
            if (asyncResults.isEmpty()) {
                // only literals
                return CompletedStage.of(parameters.entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getLiteral())));
            } else if (asyncResults.size() == 1) {
                // single non-literal param - avoid CompletableFuture.allOf() and add remaining literals
                return asyncResults.get(0).getValue().thenApply(v -> {
                    try {
                        return singleValueMap(parameters, asyncResults.get(0));
                    } catch (InterruptedException | ExecutionException e1) {
                        throw new CompletionException(e1);
                    }
                });
            } else {
                // multiple non-literal params
                CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
                CompletableFuture.allOf(asyncResults.stream().map(Entry::getValue).toArray(CompletableFuture[]::new))
                        .whenComplete((v, t1) -> {
                            if (t1 != null) {
                                result.completeExceptionally(t1);
                            } else {
                                // IMPL NOTE: Keep the map mutable - it can be modified in UserTagSectionHelper
                                Map<String, Object> values = new HashMap<>();
                                int j = 0;
                                try {
                                    for (Entry<String, Expression> entry : parameters.entrySet()) {
                                        if (entry.getValue().isLiteral()) {
                                            values.put(entry.getKey(), entry.getValue().getLiteral());
                                        } else {
                                            values.put(entry.getKey(), asyncResults.get(j++).getValue().get());
                                        }
                                    }
                                    result.complete(values);
                                } catch (Throwable e) {
                                    result.completeExceptionally(e);
                                }

                            }
                        });
                return result;
            }
        }
    }

    private static Map<String, Object> singleValueMap(String key, Object value) {
        // IMPL NOTE: Keep the map mutable - it can be modified in UserTagSectionHelper
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static Map<String, Object> singleValueMap(Map<String, Expression> parameters,
            Entry<String, CompletableFuture<Object>> value) throws InterruptedException, ExecutionException {
        // IMPL NOTE: Keep the map mutable - it can be modified in UserTagSectionHelper
        Map<String, Object> map = new HashMap<>();
        for (Entry<String, Expression> param : parameters.entrySet()) {
            if (param.getValue().isLiteral()) {
                map.put(param.getKey(), param.getValue().getLiteral());
            } else {
                map.put(value.getKey(), value.getValue().get());
            }
        }
        return map;
    }

}
