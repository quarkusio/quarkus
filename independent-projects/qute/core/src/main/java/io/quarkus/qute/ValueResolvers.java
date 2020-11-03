package io.quarkus.qute;

import static io.quarkus.qute.Booleans.isFalsy;

import io.quarkus.qute.Results.Result;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

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

    public static ValueResolver listResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return ValueResolver.matchClass(context, List.class);
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                return listResolveAsync(context);
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
                if (context.getParams().size() != 1) {
                    return false;
                }
                switch (context.getName()) {
                    case "?:":
                    case "or":
                    case ":":
                        return true;
                    default:
                        return false;
                }
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
                if (isFalsy(context.getBase())) {
                    return Results.NOT_FOUND;
                }
                return context.evaluate(context.getParams().get(0));
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

    /**
     * Performs conditional AND on the base object and the first parameter.
     * It's a short-circuiting operation - the parameter is only evaluated if needed.
     * 
     * @see Booleans#isFalsy(Object)
     */
    public static ValueResolver logicalAndResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getBase() != null && context.getParams().size() == 1
                        && ("&&".equals(context.getName()));
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                boolean baseIsFalsy = Booleans.isFalsy(context.getBase());
                return baseIsFalsy ? CompletableFuture.completedFuture(false)
                        : context.evaluate(context.getParams().get(0)).thenApply(new Function<Object, Object>() {
                            @Override
                            public Object apply(Object booleanParam) {
                                return !Booleans.isFalsy(booleanParam);
                            }
                        });
            }

        };
    }

    /**
     * Performs conditional OR on the base object and the first parameter.
     * It's a short-circuiting operation - the parameter is only evaluated if needed.
     * 
     * @see Booleans#isFalsy(Object)
     */
    public static ValueResolver logicalOrResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getBase() != null && context.getParams().size() == 1
                        && ("||".equals(context.getName()));
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                boolean baseIsFalsy = Booleans.isFalsy(context.getBase());
                return !baseIsFalsy ? CompletableFuture.completedFuture(true)
                        : context.evaluate(context.getParams().get(0)).thenApply(new Function<Object, Object>() {
                            @Override
                            public Object apply(Object booleanParam) {
                                return !Booleans.isFalsy(booleanParam);
                            }
                        });
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

    private static CompletionStage<Object> listResolveAsync(EvalContext context) {
        List<?> list = (List<?>) context.getBase();
        switch (context.getName()) {
            case "get":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0))
                            .thenApply(r -> {
                                try {
                                    int idx = r instanceof Integer ? (Integer) r : Integer.valueOf(r.toString());
                                    if (idx >= list.size()) {
                                        // Be consistent with property resolvers
                                        return Result.NOT_FOUND;
                                    }
                                    return list.get(idx);
                                } catch (NumberFormatException e) {
                                    return Result.NOT_FOUND;
                                }
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
            case "entrySet":
                return CompletableFuture.completedFuture(map.entrySet());
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
