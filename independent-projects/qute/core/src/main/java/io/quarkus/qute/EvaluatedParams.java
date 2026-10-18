package io.quarkus.qute;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public final class EvaluatedParams {

    static final EvaluatedParams EMPTY = new EvaluatedParams(CompletedStage.ofVoid(), new Supplier<?>[0]);

    /**
     *
     * @param context
     * @return the evaluated params
     */
    public static EvaluatedParams evaluate(EvalContext context) {
        List<Expression> params = context.getParams();
        if (params.isEmpty()) {
            return EMPTY;
        } else if (params.size() == 1) {
            return new EvaluatedParams(context.evaluate(params.get(0)));
        }
        Supplier<?>[] allResults = new Supplier[params.size()];
        List<CompletableFuture<?>> asyncResults = null;
        int i = 0;
        CompletedStage<?> failure = null;
        Iterator<Expression> it = params.iterator();
        while (it.hasNext()) {
            Expression expression = it.next();
            CompletionStage<?> result = context.evaluate(expression);
            if (result instanceof CompletedStage) {
                CompletedStage<?> completed = (CompletedStage<?>) result;
                allResults[i++] = completed;
                if (completed.isFailure()) {
                    failure = completed;
                }
                // No async computation needed
                continue;
            } else {
                CompletableFuture<?> fu = result.toCompletableFuture();
                if (asyncResults == null) {
                    asyncResults = new ArrayList<>();
                }
                asyncResults.add(fu);
                allResults[i++] = Futures.toSupplier(fu);
            }
        }
        CompletionStage<?> cs;
        if (asyncResults == null) {
            cs = failure != null ? failure : CompletedStage.ofVoid();
        } else if (asyncResults.size() == 1) {
            cs = asyncResults.get(0);
        } else {
            cs = CompletableFuture.allOf(asyncResults.toArray(new CompletableFuture[0]));
        }
        return new EvaluatedParams(cs, allResults);
    }

    public static EvaluatedParams evaluateMessageKey(EvalContext context) {
        List<Expression> params = context.getParams();
        if (params.isEmpty()) {
            throw new IllegalArgumentException("No params to evaluate");
        }
        return new EvaluatedParams(context.evaluate(params.get(0)));
    }

    public static EvaluatedParams evaluateMessageParams(EvalContext context) {
        List<Expression> params = context.getParams();
        if (params.size() < 2) {
            return EMPTY;
        } else if (params.size() == 2) {
            return new EvaluatedParams(context.evaluate(params.get(1)));
        }
        Supplier<?>[] allResults = new Supplier[params.size()];
        List<CompletableFuture<?>> asyncResults = null;

        int i = 0;
        CompletedStage<?> failure = null;
        Iterator<Expression> it = params.subList(1, params.size()).iterator();
        while (it.hasNext()) {
            CompletionStage<?> result = context.evaluate(it.next());
            if (result instanceof CompletedStage) {
                CompletedStage<?> completed = (CompletedStage<?>) result;
                allResults[i++] = completed;
                if (completed.isFailure()) {
                    failure = completed;
                }
                // No async computation needed
                continue;
            } else {
                CompletableFuture<?> fu = result.toCompletableFuture();
                if (asyncResults == null) {
                    asyncResults = new ArrayList<>();
                }
                asyncResults.add(fu);
                allResults[i++] = Futures.toSupplier(fu);
            }
        }
        CompletionStage<?> cs;
        if (asyncResults == null) {
            cs = failure != null ? failure : CompletedStage.ofVoid();
        } else if (asyncResults.size() == 1) {
            cs = asyncResults.get(0);
        } else {
            cs = CompletableFuture.allOf(asyncResults.toArray(new CompletableFuture[0]));
        }
        return new EvaluatedParams(cs, allResults);
    }

    public final CompletionStage<?> stage;
    private final Supplier<?>[] results;

    EvaluatedParams(CompletionStage<?> stage) {
        this.stage = stage;
        if (stage instanceof CompletedStage) {
            this.results = new Supplier[] { (CompletedStage<?>) stage };
        } else {
            this.results = new Supplier[] { Futures.toSupplier(stage.toCompletableFuture()) };
        }
    }

    EvaluatedParams(CompletionStage<?> stage, Supplier<?>[] results) {
        this.stage = stage;
        this.results = results;
    }

    public Object getResult(int index) throws InterruptedException, ExecutionException {
        return results[index].get();
    }

    /**
     *
     * @param varargs
     * @param types
     * @return {@code true} if the parameter types match the type of the evaluated params
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public boolean parameterTypesMatch(boolean varargs, Class<?>[] types) throws InterruptedException, ExecutionException {
        // Check the number of parameters and replace the last param type with component type if needed
        Class<?> componentType = null;
        if (types.length == results.length) {
            if (varargs) {
                componentType = boxType(types[types.length - 1].getComponentType());
            }
        } else {
            if (varargs) {
                int diff = types.length - results.length;
                if (diff > 1) {
                    return false;
                } else if (diff < 1) {
                    componentType = boxType(types[types.length - 1].getComponentType());
                }
                // if diff == 1 then vargs may be empty and we need to compare the result types
            } else {
                return false;
            }
        }
        int i = 0;
        Class<?> paramType = boxType(types[i]);
        while (i < results.length) {
            Object result = getResult(i);
            if (result != null) {
                Class<?> resultClass = boxType(result.getClass());
                if (!paramType.isAssignableFrom(resultClass)
                        // For varargs we also try to match the component type
                        && (componentType == null
                                || i < (types.length - 1)
                                || !componentType.isAssignableFrom(resultClass))) {
                    return false;
                }
            }
            if (types.length > ++i) {
                paramType = boxType(types[i]);
            }
        }
        return true;
    }

    public Object getVarargsResults(int numberOfParameters, Class<?> componentType)
            throws InterruptedException, ExecutionException {
        // For varargs we want to skip all previous args
        int skip = numberOfParameters - 1;
        if (skip < 0 || skip >= results.length) {
            return Array.newInstance(componentType, 0);
        }
        Object result = null;
        int capacity = results.length - skip;
        if (numberOfParameters == results.length) {
            // If there is exactly one non-skipped argument
            // test if it's not a matching array
            result = getResult(skip);
            Class<?> resultClass = result.getClass();
            if (resultClass.isArray() && resultClass.getComponentType().equals(componentType)) {
                return result;
            }
            skip++;
        }
        Object array = Array.newInstance(componentType, capacity);
        int idx = 0;
        if (result != null) {
            Array.set(array, idx++, result);
        }
        for (int i = skip; i < results.length; i++) {
            result = getResult(i);
            Array.set(array, idx++, result);
        }
        return array;
    }

    private static Class<?> boxType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        } else if (type.equals(Boolean.TYPE)) {
            return Boolean.class;
        } else if (type.equals(Character.TYPE)) {
            return Character.class;
        } else if (type.equals(Byte.TYPE)) {
            return Byte.class;
        } else if (type.equals(Short.TYPE)) {
            return Short.class;
        } else if (type.equals(Integer.TYPE)) {
            return Integer.class;
        } else if (type.equals(Long.TYPE)) {
            return Long.class;
        } else if (type.equals(Float.TYPE)) {
            return Float.class;
        } else if (type.equals(Double.TYPE)) {
            return Double.class;
        } else if (type.equals(Void.TYPE)) {
            return Void.class;
        } else {
            throw new IllegalArgumentException();
        }
    }

}