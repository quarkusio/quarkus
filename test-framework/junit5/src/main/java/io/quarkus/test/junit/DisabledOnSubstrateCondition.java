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

public class DisabledOnSubstrateCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult
            .enabled("@DisabledOnSubstrate is not present");

    /**
     * Containers/tests are disabled if {@code @DisabledOnSubstrate} is present on the test
     * class or method and we're running on Substrate.
     */
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<DisabledOnSubstrate> disabled = findAnnotation(element, DisabledOnSubstrate.class);
        if (disabled.isPresent()) {
            Store store = context.getStore(Namespace.GLOBAL);
            ExtensionState state = (ExtensionState) store.get(ExtensionState.class.getName());
            if (state != null && state.isSubstrate()) {
                String reason = disabled.map(DisabledOnSubstrate::value)
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> element.get() + " is @DisabledOnSubstrate");
                return ConditionEvaluationResult.disabled(reason);
            }
        }

        return ENABLED;
    }

}
