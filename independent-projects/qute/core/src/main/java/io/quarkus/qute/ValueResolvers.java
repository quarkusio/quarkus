package io.quarkus.qute;

import static io.quarkus.qute.Booleans.isFalsy;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
                return CompletedStage.of(new RawString(context.getBase().toString()));
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
                return CompletedStage.of(context.getBase());
            }
        };
    }

    /**
     * Returns the default value if the base object is null or not found and the base object otherwise.
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
                return CompletedStage.of(context.getBase());
            }

        };
    }

    /**
     * Return an empty list if the base object is null or not found.
     */
    public static ValueResolver orEmpty() {
        CompletionStage<Object> empty = CompletedStage.of(Collections.emptyList());
        return new ValueResolver() {

            public boolean appliesTo(EvalContext context) {
                return context.getParams().isEmpty() && context.getName().equals("orEmpty");
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                if (context.getBase() == null || Results.isNotFound(context.getBase())) {
                    return empty;
                }
                return CompletedStage.of(context.getBase());
            }
        };
    }

    /**
     * Returns {@link Results#NotFound} if the base object is falsy and the base object otherwise.
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
                return CompletedStage.of(entryResolve(entry, context.getName()));
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
                return baseIsFalsy ? CompletedStage.of(false)
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
                return !baseIsFalsy ? CompletedStage.of(true)
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
                if (name.equals("length") || name.equals("size")) {
                    return CompletedStage.of(Array.getLength(context.getBase()));
                } else if (name.equals("take")) {
                    if (context.getParams().isEmpty()) {
                        throw new IllegalArgumentException("n-th parameter is missing");
                    }
                    Expression indexExpr = context.getParams().get(0);
                    if (indexExpr.isLiteral()) {
                        Object literalValue = indexExpr.getLiteral();
                        if (literalValue instanceof Integer) {
                            return CompletedStage.of(takeArray((Integer) literalValue, context.getBase()));
                        }
                        return Results.notFound(context);
                    } else {
                        return context.evaluate(indexExpr).thenCompose(n -> {
                            if (n instanceof Integer) {
                                return CompletedStage.of(takeArray((Integer) n, context.getBase()));
                            }
                            return Results.notFound(context);
                        });
                    }
                } else if (name.equals("takeLast")) {
                    if (context.getParams().isEmpty()) {
                        throw new IllegalArgumentException("n-th parameter is missing");
                    }
                    Expression indexExpr = context.getParams().get(0);
                    if (indexExpr.isLiteral()) {
                        Object literalValue = indexExpr.getLiteral();
                        if (literalValue instanceof Integer) {
                            return CompletedStage.of(takeLastArray((Integer) literalValue, context.getBase()));
                        }
                        return Results.notFound(context);
                    } else {
                        return context.evaluate(indexExpr).thenCompose(n -> {
                            if (n instanceof Integer) {
                                return CompletedStage.of(takeLastArray((Integer) n, context.getBase()));
                            }
                            return Results.notFound(context);
                        });
                    }
                } else if (name.equals("get")) {
                    if (context.getParams().isEmpty()) {
                        throw new IllegalArgumentException("Index parameter is missing");
                    }
                    Expression indexExpr = context.getParams().get(0);
                    if (indexExpr.isLiteral()) {
                        Object literalValue = indexExpr.getLiteral();
                        if (literalValue instanceof Integer) {
                            return CompletedStage.of(Array.get(context.getBase(), (Integer) literalValue));
                        }
                        return Results.notFound(context);
                    } else {
                        return context.evaluate(indexExpr).thenCompose(idx -> {
                            if (idx instanceof Integer) {
                                return CompletedStage.of(Array.get(context.getBase(), (Integer) idx));
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
                    return CompletedStage.of(Array.get(context.getBase(), index));
                }
            }
        };
    }

    private static Object takeArray(int n, Object sourceArray) {
        int size = Array.getLength(sourceArray);
        if (n < 1 || n > size) {
            throw new IndexOutOfBoundsException(n);
        }
        Object targetArray = Array.newInstance(sourceArray.getClass().getComponentType(), n);
        System.arraycopy(sourceArray, 0, targetArray, 0, n);
        return targetArray;
    }

    private static Object takeLastArray(int n, Object sourceArray) {
        int size = Array.getLength(sourceArray);
        if (n < 1 || n > size) {
            throw new IndexOutOfBoundsException(n);
        }
        Object targetArray = Array.newInstance(sourceArray.getClass().getComponentType(), n);
        System.arraycopy(sourceArray, size - n, targetArray, 0, n);
        return targetArray;
    }

    // helper methods

    private static CompletionStage<Object> collectionResolveAsync(EvalContext context) {
        Collection<?> collection = (Collection<?>) context.getBase();
        switch (context.getName()) {
            case "size":
                return CompletedStage.of(collection.size());
            case "isEmpty":
            case "empty":
                return CompletedStage.of(collection.isEmpty());
            case "contains":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(e -> {
                        return CompletedStage.of(collection.contains(e));
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
            case "take":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0))
                            .thenApply(r -> {
                                try {
                                    int n = r instanceof Integer ? (Integer) r : Integer.valueOf(r.toString());
                                    if (n < 1 || n > list.size()) {
                                        throw new IndexOutOfBoundsException(n);
                                    }
                                    return list.subList(0, n);
                                } catch (NumberFormatException e) {
                                    return Results.NotFound.from(context);
                                }
                            });
                }
            case "takeLast":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0))
                            .thenApply(r -> {
                                try {
                                    int n = r instanceof Integer ? (Integer) r : Integer.valueOf(r.toString());
                                    if (n < 1 || n > list.size()) {
                                        throw new IndexOutOfBoundsException(n);
                                    }
                                    return list.subList(list.size() - n, list.size());
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
                return CompletedStage.of(map.keySet());
            case "values":
                return CompletedStage.of(map.values());
            case "entrySet":
                return CompletedStage.of(map.entrySet());
            case "size":
                return CompletedStage.of(map.size());
            case "empty":
            case "isEmpty":
                return map.isEmpty() ? Results.TRUE : Results.FALSE;
            case "get":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletedStage.of(map.get(k));
                    });
                }
            case "containsKey":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0)).thenCompose(k -> {
                        return CompletedStage.of(map.containsKey(k));
                    });
                }
            default:
                Object val = map.get(name);
                if (val == null) {
                    return map.containsKey(name) ? Results.NULL : Results.notFound(context);
                }
                return CompletedStage.of(val);
        }
    }

}
