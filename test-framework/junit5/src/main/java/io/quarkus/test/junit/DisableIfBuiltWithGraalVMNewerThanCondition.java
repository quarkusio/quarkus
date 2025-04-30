package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.readQuarkusArtifactProperties;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.deployment.pkg.steps.GraalVM;

public class DisableIfBuiltWithGraalVMNewerThanCondition implements ExecutionCondition {

    private static final String QUARKUS_INTEGRATION_TEST_NAME = QuarkusIntegrationTest.class.getName();
    private static final Set<String> SUPPORTED_INTEGRATION_TESTS = Set.of(QUARKUS_INTEGRATION_TEST_NAME);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<DisableIfBuiltWithGraalVMNewerThan> optional = findAnnotation(element,
                DisableIfBuiltWithGraalVMNewerThan.class);
        if (!optional.isPresent()) {
            return ConditionEvaluationResult.enabled("@DisableIfBuiltWithGraalVMNewerThan was not found");
        }
        if (!isIntegrationTest(context.getRequiredTestClass())) {
            return ConditionEvaluationResult.enabled("@DisableIfBuiltWithGraalVMNewerThan was added to an unsupported test");
        }

        GraalVMVersion annotationValue = optional.get().value();
        Properties quarkusArtifactProperties = readQuarkusArtifactProperties(context);
        try {
            GraalVM.Version version = GraalVM.Version
                    .of(quarkusArtifactProperties.getProperty("metadata.graalvm.version.full").lines());
            int comparison = annotationValue.getVersion().compareTo(version);
            if (comparison < 0) {
                return ConditionEvaluationResult.disabled("Native binary was built with GraalVM{version=" + version.toString()
                        + "} but the test is disabled for GraalVM versions newer than " + annotationValue);
            }
            return ConditionEvaluationResult
                    .enabled("Native binary was built with a GraalVM version compatible with the required version by the test");
        } catch (NumberFormatException e) {
            return ConditionEvaluationResult
                    .disabled("Unable to determine the GraalVM version with which the native binary was built");
        }
    }

    private boolean isIntegrationTest(Class<?> testClass) {
        do {
            Annotation[] annotations = testClass.getAnnotations();
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                String annotationTypeName = annotationType.getName();
                if (SUPPORTED_INTEGRATION_TESTS.contains(annotationTypeName)) {
                    return true;
                }
            }
            testClass = testClass.getSuperclass();
        } while (testClass != Object.class);
        return false;
    }
}
