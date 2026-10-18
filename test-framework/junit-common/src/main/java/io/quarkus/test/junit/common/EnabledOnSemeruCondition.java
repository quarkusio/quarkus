package io.quarkus.test.junit.common;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EnabledOnSemeruCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<EnabledOnSemeru> optional = findAnnotation(element, EnabledOnSemeru.class);
        if (optional.isEmpty()) {
            return ConditionEvaluationResult.enabled("@EnabledOnSemeru was not found");
        }
        if (JdkUtil.isSemeru()) {
            return ConditionEvaluationResult.enabled("JVM is identified as Semeru");
        }
        return ConditionEvaluationResult.disabled("JVM is not identified as Semeru");
    }
}
