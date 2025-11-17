package io.quarkus.test.spring;

import java.lang.annotation.Annotation;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Auto-registered JUnit 5 extension that detects when a test is annotated with
 * Spring Boot's {@code @org.springframework.boot.test.context.SpringBootTest}
 * and disables the test with a clear migration hint pointing to {@link SpringBootTest}.
 * <p>
 * Implements {@link ExecutionCondition} rather than {@link org.junit.jupiter.api.extension.BeforeAllCallback}
 * so it fires before any other extension's {@code beforeAll} (including {@code SpringExtension}),
 * preventing {@code NoClassDefFoundError} chains from the fake Spring stubs.
 */
public class SpringBootTestAutoExtension implements ExecutionCondition {

    private static final Logger log = Logger.getLogger(SpringBootTestAutoExtension.class);

    private static final String SPRING_BOOT_TEST_FQCN = "org.springframework.boot.test.context.SpringBootTest";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Class<?> testClass = context.getTestClass().orElse(null);
        if (testClass == null) {
            return ConditionEvaluationResult.enabled("No test class");
        }
        if (hasAnnotation(testClass, SPRING_BOOT_TEST_FQCN)
                && !testClass.isAnnotationPresent(SpringBootTest.class)) {
            String message = String.format(
                    "Test class '%s' uses '@%s' from Spring Boot, which is not supported in Quarkus. "
                            + "To run this test with Quarkus, replace the import:%n"
                            + "  - Remove:  import org.springframework.boot.test.context.SpringBootTest;%n"
                            + "  - Add:     import io.quarkus.test.spring.SpringBootTest;",
                    testClass.getSimpleName(),
                    "SpringBootTest");
            log.warn(message);
            return ConditionEvaluationResult.disabled(message);
        }
        return ConditionEvaluationResult.enabled("Using Quarkus specific @SpringBootTest");
    }

    private boolean hasAnnotation(Class<?> testClass, String annotationClassName) {
        try {
            Class<? extends Annotation> annotationClass = Class.forName(annotationClassName)
                    .asSubclass(Annotation.class);
            return testClass.isAnnotationPresent(annotationClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
