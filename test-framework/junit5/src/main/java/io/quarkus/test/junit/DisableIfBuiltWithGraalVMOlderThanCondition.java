package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.readQuarkusArtifactProperties;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisableIfBuiltWithGraalVMOlderThanCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<DisableIfBuiltWithGraalVMOlderThan> optional = findAnnotation(element,
                DisableIfBuiltWithGraalVMOlderThan.class);
        if (!optional.isPresent()) {
            return ConditionEvaluationResult.enabled("@DisableIfBuiltWithGraalVMOlderThan was not found");
        }

        DisableIfBuiltWithGraalVMOlderThan.GraalVMVersion annotationValue = optional.get().value();
        Properties quarkusArtifactProperties = readQuarkusArtifactProperties(context);
        try {
            int major = Integer.parseInt(quarkusArtifactProperties.getProperty("metadata.graalvm.version.major"));
            int minor = Integer.parseInt(quarkusArtifactProperties.getProperty("metadata.graalvm.version.minor"));
            int comparison = annotationValue.compareTo(major, minor);
            if (comparison > 0) {
                return ConditionEvaluationResult.disabled("Native binary was built with GraalVM{major=" + major + ", minor= "
                        + minor + "} but the test is disabled for GraalVM versions older than " + annotationValue);
            }
            return ConditionEvaluationResult
                    .enabled("Native binary was built with a GraalVM version compatible with the required version by the test");
        } catch (NumberFormatException e) {
            return ConditionEvaluationResult
                    .disabled("Unable to determine the GraalVM version with which the native binary was built");
        }
    }
}
