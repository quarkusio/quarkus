package io.quarkus.test.junit;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.common.JdkUtil;

public class DisabledOnSemeruCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<DisabledOnSemeru> optional = findAnnotation(element, DisabledOnSemeru.class);
        if (optional.isEmpty()) {
            return ConditionEvaluationResult.enabled("@DisabledOnSemeru was not found");
        }
        if (!JdkUtil.isSemeru()) {
            return ConditionEvaluationResult.enabled("JVM is not identified as Semeru");
        }

        DisabledOnSemeru disabledOnSemeru = optional.get();
        if (disabledOnSemeru.versionGreaterThanOrEqualTo() > 0 || disabledOnSemeru.versionLessThanOrEqualTo() > 0) {
            if (disabledOnSemeru.versionGreaterThanOrEqualTo() > 0) {
                if (Runtime.version().feature() < disabledOnSemeru.versionGreaterThanOrEqualTo()) {
                    return ConditionEvaluationResult.disabled(
                            "JVM identified as Semeru and JVM version < " + disabledOnSemeru.versionGreaterThanOrEqualTo());
                }
            }
            if (disabledOnSemeru.versionLessThanOrEqualTo() > 0) {
                if (Runtime.version().feature() > disabledOnSemeru.versionLessThanOrEqualTo()) {
                    return ConditionEvaluationResult.disabled(
                            "JVM identified as Semeru and JVM version > " + disabledOnSemeru.versionLessThanOrEqualTo());
                }
            }
            return ConditionEvaluationResult.enabled("JVM is identified as Semeru but version matches");
        }

        return ConditionEvaluationResult.disabled("JVM is identified as Semeru");
    }
}
