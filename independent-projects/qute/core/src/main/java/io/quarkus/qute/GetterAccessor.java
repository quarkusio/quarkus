package io.quarkus.qute;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

/**
 *
 * @see ReflectionValueResolver
 */
class GetterAccessor implements ValueAccessor, AccessorCandidate {

    private final Method method;

    GetterAccessor(Method method) {
        this.method = method;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<Object> getValue(Object instance) {
        try {
            Object ret = method.invoke(instance);
            if (ret instanceof CompletionStage) {
                return (CompletionStage<Object>) ret;
            } else {
                return CompletedStage.of(ret);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Reflection invocation error", e);
        }
    }

    @Override
    public ValueAccessor getAccessor(EvalContext context) {
        return this;
    }

}
