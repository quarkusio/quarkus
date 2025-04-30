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

    @Override
    public CompletionStage<Object> getValue(Object instance) {
        try {
            return CompletionStageSupport.toCompletionStage(method.invoke(instance));
        } catch (Exception e) {
            throw new IllegalStateException("Reflection invocation error", e);
        }
    }

    @Override
    public ValueAccessor getAccessor(EvalContext context) {
        return this;
    }

}
