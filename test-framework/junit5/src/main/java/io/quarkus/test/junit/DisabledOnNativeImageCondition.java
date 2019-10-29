package io.quarkus.test.junit;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.util.StringUtils;

import io.quarkus.test.junit.QuarkusTestExtension.ExtensionState;

public class DisabledOnNativeImageCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
            .enabled("@DisabledOnNativeImage is not present");

    /**
     * Containers/tests are disabled if {@code @DisabledOnNativeImage} is present on the test
     * class or method and we're running on a native image.
     */
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<DisabledOnNativeImage> disabled = findAnnotation(element, DisabledOnNativeImage.class);
        if (disabled.isPresent()) {
            Store store = context.getStore(Namespace.GLOBAL);
            ExtensionState state = (ExtensionState) store.get(ExtensionState.class.getName());
            if (state != null && state.isNativeImage()) {
                String reason = disabled.map(DisabledOnNativeImage::value)
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> element.get() + " is @DisabledOnNativeImage");
                return ConditionEvaluationResult.disabled(reason);
            }
        }

        return ENABLED;
    }

}
