package io.quarkus.test.junit;

import static io.quarkus.test.junit.ArtifactTypeUtil.isContainer;
import static io.quarkus.test.junit.ArtifactTypeUtil.isJar;
import static io.quarkus.test.junit.ArtifactTypeUtil.isNativeBinary;
import static io.quarkus.test.junit.DisabledOnIntegrationTest.ArtifactType.ALL;
import static io.quarkus.test.junit.DisabledOnIntegrationTest.ArtifactType.CONTAINER;
import static io.quarkus.test.junit.DisabledOnIntegrationTest.ArtifactType.JAR;
import static io.quarkus.test.junit.DisabledOnIntegrationTest.ArtifactType.NATIVE_BINARY;
import static io.quarkus.test.junit.IntegrationTestUtil.getArtifactType;
import static io.quarkus.test.junit.IntegrationTestUtil.readQuarkusArtifactProperties;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
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
     * Containers/tests are disabled if {@code @DisabledOnIntegrationTest} is present on the test class or method and
     * we're running on a native image.
     */
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        ConditionEvaluationResult disabledOnIntegrationTestReason = check(context, element,
                DisabledOnIntegrationTest.class, DisabledOnIntegrationTest::value,
                // check and see if the artifact type of the build has been disabled
                ((ec, an) -> {
                    DisabledOnIntegrationTest.ArtifactType[] disabledArtifactTypes = an.forArtifactTypes();
                    String artifactType = getArtifactType(readQuarkusArtifactProperties(ec));
                    for (DisabledOnIntegrationTest.ArtifactType disabledArtifactType : disabledArtifactTypes) {
                        if (disabledArtifactType == ALL) {
                            return true;
                        }
                        if ((disabledArtifactType == CONTAINER) && isContainer(artifactType)) {
                            return true;
                        }
                        if ((disabledArtifactType == NATIVE_BINARY) && isNativeBinary(artifactType)) {
                            return true;
                        }
                        if ((disabledArtifactType == JAR) && isJar(artifactType)) {
                            return true;
                        }
                    }
                    return false;
                }));
        return Objects.requireNonNullElse(disabledOnIntegrationTestReason, ENABLED);
    }

    private <T extends Annotation> ConditionEvaluationResult check(ExtensionContext context,
            Optional<AnnotatedElement> element, Class<T> annotationClass, Function<T, String> valueExtractor,
            BiFunction<ExtensionContext, T, Boolean> predicate) {
        Optional<T> disabled = findAnnotation(element, annotationClass);
        if (disabled.isPresent()) {
            // Cannot use ExtensionState here because this condition needs to be evaluated before QuarkusTestExtension
            boolean it = findAnnotation(context.getTestClass(), QuarkusIntegrationTest.class).isPresent()
                    || findAnnotation(context.getTestClass(), QuarkusMainIntegrationTest.class).isPresent();
            if (it) {
                if ((predicate == null) || predicate.apply(context, disabled.get())) {
                    String reason = disabled.map(valueExtractor).filter(StringUtils::isNotBlank)
                            .orElseGet(() -> element.get() + " is @DisabledOnIntegrationTest");
                    return ConditionEvaluationResult.disabled(reason);
                }
            }
        }
        return null;
    }

}
