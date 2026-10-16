package io.quarkus.test.junit.condition;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Base class for {@link ExecutionCondition} implementations that support {@linkplain Repeatable repeatable} annotations.
 *
 * @param <A> the type of repeatable annotation supported by this {@code ExecutionCondition}
 */
public abstract class RepeatableAnnotationCondition<A extends Annotation> implements ExecutionCondition {
    private final Class<A> annotationType;

    public RepeatableAnnotationCondition(Class<A> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public final ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> optionalElement = context.getElement();

        if (optionalElement.isPresent()) {
            AnnotatedElement annotatedElement = optionalElement.get();
            List<A> repeatableAnnotations = AnnotationSupport.findRepeatableAnnotations(annotatedElement, this.annotationType);

            for (A annotation : repeatableAnnotations) {
                ConditionEvaluationResult result = evaluate(annotation);

                if (result.isDisabled()) {
                    return result;
                }
            }
        }

        return getNoDisabledConditionsEncounteredResult();
    }

    protected abstract ConditionEvaluationResult evaluate(A annotation);

    protected abstract ConditionEvaluationResult getNoDisabledConditionsEncounteredResult();
}
