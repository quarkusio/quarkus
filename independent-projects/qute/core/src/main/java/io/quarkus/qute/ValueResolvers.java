package io.quarkus.qute;

import static io.quarkus.qute.Booleans.isFalsy;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.qute.EvaluatorImpl.EvalContextImpl;
import io.quarkus.qute.EvaluatorImpl.TerminalEvalContextImpl;

/**
 * Common value resolvers.
 */
public final class ValueResolvers {

    static final String THIS = "this";
    static final String ELVIS = "?:";
    static final String COLON = ":";
    public static final String OR = "or";

    public static ValueResolver rawResolver() {
        return new RawResolver();
    }

    public static final class RawResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            return context.getBase() != null
                    && (context.getName().equals("raw") || context.getName().equals("safe"));
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return CompletedStage.of(new RawString(context.getBase().toString()));
        }

    }

    public static ValueResolver listResolver() {
        return new ListResolver();
    }

    public static final class ListResolver implements ValueResolver {
        @Override
        public int getPriority() {
            // Use this resolver before collectionResolver()
            return WithPriority.DEFAULT_PRIORITY + 1;
        }

        public boolean appliesTo(EvalContext context) {
            return ValueResolvers.matchClass(context, List.class);
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return listResolveAsync(context);
        }
    }

    public static ValueResolver collectionResolver() {
        return new CollectionResolver();
    }

    public static final class CollectionResolver implements ValueResolver {
        public boolean appliesTo(EvalContext context) {
            return ValueResolvers.matchClass(context, Collection.class);
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return collectionResolveAsync(context);
        }
    }

    public static ValueResolver thisResolver() {
        return new ThisResolver();
    }

    public static final class ThisResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            return context.getBase() != null && THIS.equals(context.getName());
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return CompletedStage.of(context.getBase());
        }

    }

    /**
     * Returns the default value if the base object is {@code null}, empty {@link Optional} or not found and the base object
     * otherwise.
     *
     * {@code foo.or(bar)}, {@code foo or true}, {@code name ?: 'elvis'}
     */
    public static ValueResolver orResolver() {
        return new OrResolver();
    }

    public static final class OrResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            if (context.getParams().size() != 1) {
                return false;
            }
            String name = context.getName();
            return name.equals(ELVIS) || name.equals(OR) || name.equals(COLON);
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            Object base = context.getBase();
            if (base == null || Results.isNotFound(base) || (base instanceof Optional && ((Optional<?>) base).isEmpty())) {
                return asLiteralOrEvaluate(context, 0);
            }
            return CompletedStage.of(base);
        }

    }

    /**
     * Return an empty list if the base object is null or not found.
     */
    public static ValueResolver orEmpty() {
        return new OrEmptyResolver();
    }

    public static final class OrEmptyResolver implements ValueResolver {

        private static final CompletionStage<Object> EMPTY = CompletedStage.of(Collections.emptyList());

        public boolean appliesTo(EvalContext context) {
            return context.getParams().isEmpty() && context.getName().equals("orEmpty");
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            if (context.getBase() == null || Results.isNotFound(context.getBase())) {
                return EMPTY;
            }
            return CompletedStage.of(context.getBase());
        }
    }

    /**
     * Returns {@link Results#NotFound} if the base object is falsy and the base object otherwise.
     * <p>
     * Can be used together with {@link #orResolver()} to form a ternary operator.
     *
     * {@code person.isElvis ? 'elvis' : notElvis}
     */
    public static ValueResolver trueResolver() {
        return new TrueResolver();
    }

    public static final class TrueResolver implements ValueResolver {

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
            return asLiteralOrEvaluate(context, 0);
        }
    }

    public static ValueResolver mapEntryResolver() {
        return new MapEntryResolver();
    }

    public static final class MapEntryResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            return ValueResolvers.matchClass(context, Entry.class);
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            Entry<?, ?> entry = (Entry<?, ?>) context.getBase();
            return CompletedStage.of(entryResolve(entry, context.getName()));
        }
    }

    public static ValueResolver mapResolver() {
        return new MapResolver();
    }

    public static final class MapResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            return ValueResolvers.matchClass(context, Map.class);
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return mapResolveAsync(context);
        }
    }

    public static ValueResolver mapperResolver() {
        return new MapperResolver();
    }

    public static final class MapperResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            if (hasNoParams(context)
                    && matchClass(context, Mapper.class)
                    && context.getBase() instanceof Mapper mapper) {
                return mapper.appliesTo(context.getName());
            }
            return false;
        }

        @Override
        public int getPriority() {
            // mapper is used in loops so we use a higher priority to jump the queue
            return 15;
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return ((Mapper) context.getBase()).getAsync(context.getName());
        }
    }

    /**
     * Performs conditional AND on the base object and the first parameter.
     * It's a short-circuiting operation - the parameter is only evaluated if needed.
     *
     * @see Booleans#isFalsy(Object)
     */
    public static ValueResolver logicalAndResolver() {
        return new LogicalAndResolver();
    }

    public static final class LogicalAndResolver implements ValueResolver {

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
    }

    /**
     * Performs conditional OR on the base object and the first parameter.
     * It's a short-circuiting operation - the parameter is only evaluated if needed.
     *
     * @see Booleans#isFalsy(Object)
     */
    public static ValueResolver logicalOrResolver() {
        return new LogicalOrResolver();
    }

    public static final class LogicalOrResolver implements ValueResolver {

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

    }

    public static ValueResolver arrayResolver() {
        return new ArrayResolver();
    }

    public static final class ArrayResolver implements ValueResolver {

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

    }

    public static ValueResolver numberValueResolver() {
        return new NumberValueResolver();
    }

    public static final class NumberValueResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            Object base = context.getBase();
            String name = context.getName();
            return base != null
                    && (base instanceof Number)
                    && context.getParams().isEmpty()
                    && ("intValue".equals(name) || "longValue".equals(name) || "floatValue".equals(name)
                            || "doubleValue".equals(name) || "byteValue".equals(name) || "shortValue".equals(name));
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            switch (context.getName()) {
                case "intValue":
                    return CompletedStage.of(((Number) context.getBase()).intValue());
                case "longValue":
                    return CompletedStage.of(((Number) context.getBase()).longValue());
                case "floatValue":
                    return CompletedStage.of(((Number) context.getBase()).floatValue());
                case "doubleValue":
                    return CompletedStage.of(((Number) context.getBase()).doubleValue());
                case "byteValue":
                    return CompletedStage.of(((Number) context.getBase()).byteValue());
                case "shortValue":
                    return CompletedStage.of(((Number) context.getBase()).shortValue());
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

    public static ValueResolver plusResolver() {
        return new PlusResolver();
    }

    public static final class PlusResolver extends IntArithmeticResolver {

        @Override
        protected boolean appliesToName(String name) {
            return "plus".equals(name) || "+".equals(name);
        }

        @Override
        protected Object compute(Long op1, Long op2) {
            return op1 + op2;
        }

        @Override
        protected Object compute(Integer op1, Long op2) {
            return op1 + op2;
        }

        @Override
        protected Object compute(Long op1, Integer op2) {
            return op1 + op2;
        }

        @Override
        protected Object compute(Integer op1, Integer op2) {
            return op1 + op2;
        }

    }

    public static ValueResolver minusResolver() {
        return new MinusResolver();
    }

    public static final class MinusResolver extends IntArithmeticResolver {

        @Override
        protected boolean appliesToName(String name) {
            return "minus".equals(name) || "-".equals(name);
        }

        @Override
        protected Object compute(Long op1, Long op2) {
            return op1 - op2;
        }

        @Override
        protected Object compute(Integer op1, Long op2) {
            return op1 - op2;
        }

        @Override
        protected Object compute(Long op1, Integer op2) {
            return op1 - op2;
        }

        @Override
        protected Object compute(Integer op1, Integer op2) {
            return op1 - op2;
        }
    }

    public static ValueResolver modResolver() {
        return new ModResolver();
    }

    public static final class ModResolver extends IntArithmeticResolver {

        @Override
        protected boolean appliesToName(String name) {
            return "mod".equals(name);
        }

        @Override
        protected Object compute(Long op1, Long op2) {
            return op1 % op2;
        }

        @Override
        protected Object compute(Integer op1, Long op2) {
            return op1 % op2;
        }

        @Override
        protected Object compute(Long op1, Integer op2) {
            return op1 % op2;
        }

        @Override
        protected Object compute(Integer op1, Integer op2) {
            return op1 % op2;
        }

    }

    static abstract class IntArithmeticResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            Object base = context.getBase();
            return base != null
                    && (base instanceof Integer || base instanceof Long)
                    && context.getParams().size() == 1
                    && appliesToName(context.getName());
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return context.evaluate(context.getParams().get(0)).thenApply(new Function<Object, Object>() {
                @Override
                public Object apply(Object param) {
                    Object base = context.getBase();
                    if (param instanceof Integer) {
                        Integer intParam = (Integer) param;
                        if (base instanceof Integer) {
                            return compute((Integer) base, intParam);
                        } else if (base instanceof Long) {
                            return compute((Long) base, intParam);
                        } else {
                            throw new IllegalStateException("Unsupported base operand: " + base.getClass());
                        }
                    } else if (param instanceof Long) {
                        Long longParam = (Long) param;
                        if (base instanceof Integer) {
                            return compute((Integer) base, longParam);
                        } else if (base instanceof Long) {
                            return compute((Long) base, longParam);
                        } else {
                            throw new IllegalStateException("Unsupported base operand: " + base.getClass());
                        }
                    }
                    return Results.notFound(context);
                }
            });
        }

        protected abstract boolean appliesToName(String name);

        protected abstract Object compute(Integer op1, Integer op2);

        protected abstract Object compute(Long op1, Integer op2);

        protected abstract Object compute(Integer op1, Long op2);

        protected abstract Object compute(Long op1, Long op2);

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
        String name = context.getName();
        switch (name) {
            case "get":
                if (context.getParams().size() == 1) {
                    return context.evaluate(context.getParams().get(0))
                            .thenApply(r -> {
                                try {
                                    int n = r instanceof Integer ? (Integer) r : Integer.parseInt(r.toString());
                                    return list.get(n);
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
            case "first":
                if (list.isEmpty()) {
                    throw new NoSuchElementException();
                }
                return CompletedStage.of(list.get(0));
            case "last":
                if (list.isEmpty()) {
                    throw new NoSuchElementException();
                }
                return CompletedStage.of(list.get(list.size() - 1));
            default:
                // Try to use the name as an index
                int index;
                try {
                    index = Integer.parseInt(name);
                } catch (NumberFormatException e) {
                    return Results.notFound(context);
                }
                return CompletedStage.of(list.get(index));
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

    /**
     *
     * @param ctx
     * @return {@code true} if the context has no parameters; i.e it is not a virtual method call, {@code false} otherwise
     */
    public static boolean hasNoParams(EvalContext ctx) {
        if (ctx instanceof TerminalEvalContextImpl terminal) {
            return terminal.params.isEmpty();
        }
        if (ctx instanceof EvalContextImpl impl) {
            return impl.params.isEmpty();
        }
        return ctx.getParams().isEmpty();
    }

    /**
     *
     * @param ctx
     * @return {@code true} if the context has at least one parameter; i.e it is a virtual method call, {@code false} otherwise
     */
    public static boolean hasParams(EvalContext ctx) {
        return !hasNoParams(ctx);
    }

    /**
     *
     * @param ctx
     * @param clazz
     * @return {@code true} if the base object from the context is assignable from the clazz, {@code false} otherwise
     */
    public static boolean matchClass(EvalContext ctx, Class<?> clazz) {
        Object base;
        if (ctx instanceof TerminalEvalContextImpl terminal) {
            base = terminal.base;
        } else if (ctx instanceof EvalContextImpl impl) {
            base = impl.base;
        } else {
            base = ctx.getBase();
        }
        return base != null && clazz.isAssignableFrom(base.getClass());
    }

    private static CompletionStage<Object> asLiteralOrEvaluate(EvalContext context, int pos) {
        Expression val = context.getParams().get(pos);
        return val.isLiteral() ? val.asLiteral() : context.evaluate(val);
    }

}
