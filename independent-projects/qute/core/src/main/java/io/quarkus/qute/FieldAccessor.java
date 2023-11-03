package io.quarkus.qute;

import java.lang.reflect.Field;
import java.util.concurrent.CompletionStage;

/**
 *
 * @see ReflectionValueResolver
 */
class FieldAccessor implements ValueAccessor, AccessorCandidate {

    private final Field field;

    FieldAccessor(Field field) {
        this.field = field;
    }

    @Override
    public CompletionStage<Object> getValue(Object instance) {
        try {
            return CompletionStageSupport.toCompletionStage(field.get(instance));
        } catch (Exception e) {
            throw new IllegalStateException("Reflection invocation error", e);
        }
    }

    @Override
    public ValueAccessor getAccessor(EvalContext context) {
        return this;
    }

}
