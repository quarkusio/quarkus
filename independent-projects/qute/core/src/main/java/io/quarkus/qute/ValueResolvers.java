package io.quarkus.qute;

import static io.quarkus.qute.Booleans.isFalsy;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
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
     * Returns the default value if the base object is null or {@link Result#NOT_FOUND} and the base object otherwise.
     * 
     * {@code foo.or(bar)}, {@code foo or true}, {@code name ?: 'elvis'}
     */
    public static ValueResolver orResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                if (context.getParams().size() != 1) {
                    return false;
                }
                String name = context.getName();
                return name.equals("?:") || name.equals("or") || name.equals(":");
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                if (context.getBase() == null || Results.isNotFound(context.getBase())) {
                    return context.evaluate(context.getParams().get(0));
                }
                return CompletableFuture.completedFuture(context.getBase());
            }

        };
    }

    /**
     * Return an empty list if the base object is null or {@link Result#NOT_FOUND}.
     */
    public static ValueResolver orEmpty() {
        CompletionStage<Object> empty = CompletableFuture.completedFuture(Collections.emptyList());
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getParams().isEmpty() && context.getName().equals("orEmpty");
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                if (context.getBase() == null || Results.isNotFound(context.getBase())) {
                    return empty;
                }
                return CompletableFuture.completedFuture(context.getBase());
            }
        };
    }

    /**
     * Returns {@link Result#NOT_FOUND} if the base object is falsy and the base object otherwise.
     * <p>
     * Can be used together with {@link #orResolver()} to form a ternary operator.
     * 
     * {@code person.isElvis ? 'elvis' : notElvis}
     */
    public static ValueResolver trueResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                if (context.getParams().size() != 1) {
                    return false;
                }
                String name = context.getName();
                return name.equals("?") || name.equals("ifTruthy");
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                if (isFalsy(context.getBase())) {
                    return Results.notFound(context);
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
                if (context.getBase() instanceof Mapper && context.getParams().isEmpty()) {
                    return ((Mapper) context.getBase()).appliesTo(context.getName());
                }
                return false;
            }

            @Override
            public int getPriority() {
                // mapper is used in loops so we use a higher priority to jump the queue
                return 5;
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                Mapper mapper = (Mapper) context.getBase();
                return mapper.getAsync(context.getName());
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

    public static ValueResolver arrayResolver() {
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getBase() != null && context.getBase().getClass().isArray();
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                String name = context.getName();
                if (name.equals("length")) {
                    return CompletableFuture.completedFuture(Array.getLength(context.getBase()));
                } else if (name.equals("get")) {
                    if (context.getParams().isEmpty()) {
                        throw new IllegalArgumentException("Index parameter is missing");
                    }
                    Expression indexExpr = context.getParams().get(0);
                    if (indexExpr.isLiteral()) {
                        Object literalValue;
                        try {
                            literalValue = indexExpr.getLiteralValue().get();
                            if (literalValue instanceof Integer) {
                                return CompletableFuture.completedFuture(Array.get(context.getBase(), (Integer) literalValue));
                            }
                            return Results.notFound(context);
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return context.evaluate(indexExpr).thenCompose(idx -> {
                            if (idx instanceof Integer) {
                                return CompletableFuture.completedFuture(Array.get(context.getBase(), (Integer) idx));
                            }
                            return Results.notFound(context);
                        });
                    }
                } else {
                    // Try to use the name as an index
                    int index;
                    try {
                        index = Integer.parseInt(name);
                    } catch (NumberFormatException e) {
                        return Results.notFound(context);
                    }
                    return CompletableFuture.completedFuture(Array.get(context.getBase(), index));
                }
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
                return Results.notFound(context);
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
                                        return Results.NotFound.from(context);
                                    }
                                    return list.get(idx);
                                } catch (NumberFormatException e) {
                                    return Results.NotFound.from(context);
                                }
                            });
                }
            default:
                return Results.notFound(context);
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
                return Results.NotFound.from(name);
        }
    }

    @SuppressWarnings("rawtypes")
    private static CompletionStage<Object> mapResolveAsync(EvalContext context) {
        Map map = (Map) context.getBase();
        String name = context.getName();
        switch (name) {
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
                return map.isEmpty() ? Results.TRUE : Results.FALSE;
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
                Object val = map.get(name);
                if (val == null) {
                    return map.containsKey(name) ? Results.NULL : Results.notFound(context);
                }
                return CompletableFuture.completedFuture(val);
        }
    }

}
