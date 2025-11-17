package io.quarkus.test.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;

class SpringBootTestAutoExtensionTest {

    @SpringBootTest
    static class SpringAnnotatedClass {
    }

    @io.quarkus.test.spring.SpringBootTest
    static class QuarkusAnnotatedClass {
    }

    static class UnannotatedClass {
    }

    private final SpringBootTestAutoExtension extension = new SpringBootTestAutoExtension();

    @Test
    void shouldHaveWarningAndMigrationHintWhenSpringAnnotationIsUsed() {
        ConditionEvaluationResult result = extension.evaluateExecutionCondition(contextFor(SpringAnnotatedClass.class));

        assertTrue(result.isDisabled());
        String reason = result.getReason().orElse("");
        assertTrue(reason.contains("import io.quarkus.test.spring.SpringBootTest"),
                "Reason should contain migration hint, but was: " + reason);
    }

    @Test
    void shouldBeEnabledWhenQuarkusSpecificSpringAnnotationIsUsed() {
        ConditionEvaluationResult result = extension.evaluateExecutionCondition(contextFor(QuarkusAnnotatedClass.class));

        assertFalse(result.isDisabled());
    }

    @Test
    void shouldBeEnabledWhenNoAnnotationPresent() {
        ConditionEvaluationResult result = extension.evaluateExecutionCondition(contextFor(UnannotatedClass.class));

        assertFalse(result.isDisabled());
    }

    @Test
    void shouldBeEnabledWhenNullTestClass() {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getTestClass()).thenReturn(Optional.empty());

        ConditionEvaluationResult result = extension.evaluateExecutionCondition(context);

        assertFalse(result.isDisabled());
    }

    private ExtensionContext contextFor(Class<?> testClass) {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getTestClass()).thenReturn(Optional.of(testClass));
        return context;
    }
}