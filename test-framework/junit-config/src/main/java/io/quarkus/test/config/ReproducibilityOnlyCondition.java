package io.quarkus.test.config;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ReproducibilityOnlyCondition implements ExecutionCondition {

    private static final String REPRODUCIBILITY_CHECK_PROPERTY_NAME = "quarkus-internal.test.reproducibility-check";
    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
            .enabled("Reproducibility check is not active");
    private static final ConditionEvaluationResult ENABLED_REPRODUCIBILITY_TEST = ConditionEvaluationResult
            .enabled("Reproducibility check is active and the test uses a supported Quarkus test extension");
    private static final ConditionEvaluationResult DISABLED = ConditionEvaluationResult
            .disabled("Reproducibility check is active and the test does not use a supported Quarkus test extension");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (System.getProperty(REPRODUCIBILITY_CHECK_PROPERTY_NAME) == null) {
            return ENABLED;
        }

        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isEmpty()) {
            return ENABLED_REPRODUCIBILITY_TEST;
        }

        return usesSupportedExtension(testClass.get()) ? ENABLED_REPRODUCIBILITY_TEST : DISABLED;
    }

    private static boolean usesSupportedExtension(Class<?> testClass) {
        Class<?> current = testClass;
        while (current != null) {
            if (hasSupportedExtensionField(current)) {
                return true;
            }
            current = current.getSuperclass();
        }
        Class<?> enclosingClass = testClass.getEnclosingClass();
        return enclosingClass != null && usesSupportedExtension(enclosingClass);
    }

    private static boolean hasSupportedExtensionField(Class<?> testClass) {
        for (Field field : testClass.getDeclaredFields()) {
            if (ReproducibilityCapableTestExtension.class.isAssignableFrom(field.getType())) {
                return true;
            }
        }
        return false;
    }
}
