package io.quarkus.qute;

import io.quarkus.qute.Results.Result;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Common value resolvers.
 */
public final class ValueResolvers {

    static final String THIS = "this";

    public static ValueResolver rawResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getBase() != null
                        && (context.getName().equals("raw") || context.getName().equals("safe"));
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                return CompletableFuture.completedFuture(new RawString(context.getBase().toString()));
            }
        };
    }

    public static ValueResolver collectionResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return ValueResolver.matchClass(context, Collection.class);
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                return collectionResolveAsync(context);
            }
        };
    }

    public static ValueResolver thisResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getBase() != null && THIS.equals(context.getName());
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                return CompletableFuture.completedFuture(context.getBase());
            }
        };
    }

    /**
     * Returns the default value if the base object is null or {@link Result#NOT_FOUND}.
     * 
     * {@code foo.or(bar)}, {@code foo or true}, {@code name ?: 'elvis'}
     */
    public static ValueResolver orResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getParams().size() == 1
                        && ("?:".equals(context.getName()) || "or".equals(context.getName()) || ":".equals(context.getName()));
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                if (context.getBase() == null || Results.Result.NOT_FOUND.equals(context.getBase())) {
                    return context.evaluate(context.getParams().get(0));
                }
                return CompletableFuture.completedFuture(context.getBase());
            }

        };
    }

    /**
     * Can be used together with {@link #orResolver()} to form a ternary operator.
     * 
     * {@code person.isElvis ? 'elvis' : notElvis}
     */
    public static ValueResolver trueResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getParams().size() == 1
                        && ("?".equals(context.getName()));
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                if (Boolean.TRUE.equals(context.getBase())) {
                    return context.evaluate(context.getParams().get(0));
                }
                return Results.NOT_FOUND;
            }

        };
    }

    public static ValueResolver mapEntryResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return ValueResolver.matchClass(context, Entry.class);
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                Entry<?, ?> entry = (Entry<?, ?>) context.getBase();
                return CompletableFuture.completedFuture(entryResolve(entry, context.getName()));
            }
        };
    }

    public static ValueResolver mapResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return ValueResolver.matchClass(context, Map.class);
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                return mapResolveAsync(context);
            }
        };
    }

    public static ValueResolver mapperResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getBase() instanceof Mapper;
            }

            @Override
            public int getPriority() {
                // mapper is used in loops so we use a higher priority to jump the queue
                return 5;
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                Mapper mapper = (Mapper) context.getBase();
                return CompletableFuture.completedFuture(mapper.get(context.getName()));
            }

        };
    }

    // helper methods

    private static CompletionStage<Object> collectionResolveAsync(EvalContext context) {
        Collection<?> collection = (Collection<?>) context.getBase();
        switch (context.getName()) {
            case "size":
                return CompletableFuture.completedFuture(collection.size());
            case "isEmpty":
            case "empty":
                return CompletableFuture.completedFuture(collection.isEmpty());
            case "contains":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(e -> {
                        return CompletableFuture.completedFuture(collection.contains(e));
                    });
                }
            default:
                return Results.NOT_FOUND;
        }
    }

    private static Object entryResolve(Entry<?, ?> entry, String name) {
        switch (name) {
            case "key":
            case "getKey":
                return entry.getKey();
            case "value":
            case "getValue":
                return entry.getValue();
            default:
                return Result.NOT_FOUND;
        }
    }

    @SuppressWarnings("rawtypes")
    private static CompletionStage<Object> mapResolveAsync(EvalContext context) {
        Map map = (Map) context.getBase();
        switch (context.getName()) {
            case "keys":
            case "keySet":
                return CompletableFuture.completedFuture(map.keySet());
            case "values":
                return CompletableFuture.completedFuture(map.values());
            case "size":
                return CompletableFuture.completedFuture(map.size());
            case "empty":
            case "isEmpty":
                return CompletableFuture.completedFuture(map.isEmpty());
            case "get":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletableFuture.completedFuture(map.get(k));
                    });
                }
            case "containsKey":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletableFuture.completedFuture(map.containsKey(k));
                    });
                }
            default:
                return map.containsKey(context.getName()) ? CompletableFuture.completedFuture(map.get(context.getName()))
                        : Results.NOT_FOUND;
        }
    }

}
