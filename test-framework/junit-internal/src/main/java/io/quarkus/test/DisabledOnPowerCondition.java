package io.quarkus.test;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisabledOnPowerCondition implements ExecutionCondition {

    private static final String ARCH = System.getProperty("os.arch");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<DisabledOnPower> optional = findAnnotation(element, DisabledOnPower.class);
        if (optional.isEmpty()) {
            return ConditionEvaluationResult.enabled("@DisabledOnPower was not found");
        }
        if (!"ppc64le".equals(ARCH)) {
            return ConditionEvaluationResult.enabled("arch is not ppc64le");
        }

        return ConditionEvaluationResult.disabled("arch is ppc64le");
    }
}
