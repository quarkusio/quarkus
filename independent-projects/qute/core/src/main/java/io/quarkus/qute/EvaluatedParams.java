package io.quarkus.qute;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("rawtypes")
public final class EvaluatedParams {

    static final EvaluatedParams EMPTY;

    static {
        CompletableFuture<Void> empty = new CompletableFuture<Void>();
        empty.complete(null);
        EMPTY = new EvaluatedParams(empty, new CompletableFuture<?>[0]);
    }

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
        CompletableFuture<?>[] allResults = new CompletableFuture<?>[params.size()];
        List<CompletableFuture<?>> results = null;
        int i = 0;
        Iterator<Expression> it = params.iterator();
        while (it.hasNext()) {
            Expression expression = it.next();
            CompletableFuture<Object> result = context.evaluate(expression).toCompletableFuture();
            allResults[i++] = result;
            if (!expression.isLiteral()) {
                if (results == null) {
                    results = new LinkedList<>();
                }
                results.add(result);
            }
        }
        CompletionStage<?> cs;
        if (results == null) {
            cs = Futures.COMPLETED;
        } else if (results.size() == 1) {
            cs = results.get(0);
        } else {
            cs = CompletableFuture.allOf(results.toArray(new CompletableFuture[0]));
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
        }
        CompletableFuture<?>[] results = new CompletableFuture<?>[params.size() - 1];
        int i = 0;
        Iterator<Expression> it = params.subList(1, params.size()).iterator();
        while (it.hasNext()) {
            results[i++] = context.evaluate(it.next()).toCompletableFuture();
        }
        return new EvaluatedParams(CompletableFuture.allOf(results), results);
    }

    public final CompletionStage<?> stage;
    private final CompletableFuture<?>[] results;

    EvaluatedParams(CompletionStage<?> stage) {
        this.stage = stage;
        this.results = new CompletableFuture<?>[] { stage.toCompletableFuture() };
    }

    EvaluatedParams(CompletionStage<?> stage, CompletableFuture[] results) {
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
        if (types.length == results.length) {
            if (varargs) {
                types[types.length - 1] = types[types.length - 1].getComponentType();
            }
        } else {
            if (varargs) {
                int diff = types.length - results.length;
                if (diff > 1) {
                    return false;
                } else if (diff < 1) {
                    Class<?> varargsType = types[types.length - 1];
                    types[types.length - 1] = varargsType.getComponentType();
                }
                // if diff == 1 then vargs may be empty and we need to compare the result types
            } else {
                return false;
            }
        }
        int i = 0;
        Class<?> paramType = boxType(types[i]);
        while (i < results.length) {
            Class<?> resultClass = boxType(getResult(i).getClass());
            if (!paramType.isAssignableFrom(resultClass)) {
                return false;
            }
            if (types.length > ++i) {
                paramType = boxType(types[i]);
            }
        }
        return true;
    }

    public Object getVarargsResults(int numberOfParameters, Class<?> componentType)
            throws InterruptedException, ExecutionException {
        int skip = numberOfParameters - 1;
        if (skip < 0) {
            return Array.newInstance(componentType, 0);
        }
        int idx = 0;
        Object array = Array.newInstance(componentType, results.length - skip);
        for (int i = skip; i < results.length; i++) {
            Object result = getResult(i);
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