package io.quarkus.qute;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("rawtypes")
public final class EvaluatedParams {

    /**
     * 
     * @param context
     * @return the evaluated params
     */
    public static EvaluatedParams evaluate(EvalContext context) {
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

    public final CompletionStage stage;
    private final CompletableFuture<?>[] results;

    EvaluatedParams(CompletionStage stage) {
        this.stage = stage;
        this.results = new CompletableFuture<?>[] { stage.toCompletableFuture() };
    }

    EvaluatedParams(CompletionStage stage, CompletableFuture[] results) {
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
        if (types.length != results.length) {
            if (varargs) {
                int diff = types.length - results.length;
                if (diff == 1) {
                    // varargs may be empty
                    return true;
                } else if (diff > 1) {
                    return false;
                }
                // diff < 1
                // Replace the last param type with component type
                Class<?> varargsType = types[types.length - 1];
                types[types.length - 1] = varargsType.getComponentType();
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
                paramType = types[i];
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