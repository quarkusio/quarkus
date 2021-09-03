package io.quarkus.test.junit;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.StringUtils;

import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;

public class DisabledOnIntegrationTestCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
            .enabled("@DisabledOnIntegrationTest is not present");

    /**
     * Containers/tests are disabled if {@code @DisabledOnIntegrationTest} is present on the test
     * class or method and we're running on a native image.
     */
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        ConditionEvaluationResult disabledOnIntegrationTestReason = check(context, element, DisabledOnIntegrationTest.class,
                DisabledOnIntegrationTest::value);
        if (disabledOnIntegrationTestReason != null) {
            return disabledOnIntegrationTestReason;
        }
        // support DisabledOnNativeImage for backward compatibility
        ConditionEvaluationResult disabledOnNativeImageReason = check(context, element, DisabledOnNativeImage.class,
                DisabledOnNativeImage::value);
        if (disabledOnNativeImageReason != null) {
            return disabledOnNativeImageReason;
        }
        return ENABLED;
    }

    private <T extends Annotation> ConditionEvaluationResult check(ExtensionContext context, Optional<AnnotatedElement> element,
            Class<T> annotationClass, Function<T, String> valueExtractor) {
        Optional<T> disabled = findAnnotation(element, annotationClass);
        if (disabled.isPresent()) {
            // Cannot use ExtensionState here because this condition needs to be evaluated before QuarkusTestExtension
            boolean it = findAnnotation(context.getTestClass(), QuarkusIntegrationTest.class).isPresent()
                    || findAnnotation(context.getTestClass(), QuarkusMainIntegrationTest.class).isPresent();
            if (it) {
                String reason = disabled.map(valueExtractor)
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> element.get() + " is @DisabledOnIntegrationTest");
                return ConditionEvaluationResult.disabled(reason);
            }
        }
        return null;
    }

}
