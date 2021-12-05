package io.quarkus.it.main;

import static java.lang.String.format;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.junit.QuarkusTestExtension;

@ExtendWith(value = { QuarkusTestExtension.class, EnabledIfTest.EnabledIfCondition.class })
public class EnabledIfTest {

    @Test
    @EnabledIf(R5Enabled.class)
    void testInjectionWorksProperly() {
        // empty test, nothing to test here
    }

    public static final class R5Enabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ConfigProvider.getConfig().getOptionalValue("quarkus.camel.fhir.enable-r5", Boolean.class)
                    .orElse(Boolean.TRUE);
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ExtendWith(EnabledIfCondition.class)
    public @interface EnabledIf {
        Class<? extends BooleanSupplier>[] value() default {};
    }

    static class EnabledIfCondition implements ExecutionCondition {
        private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledIf is not present");

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return findAnnotation(context.getElement(), EnabledIf.class).map(this::map).orElse(ENABLED_BY_DEFAULT);
        }

        private ConditionEvaluationResult map(EnabledIf annotation) {
            for (Class<? extends BooleanSupplier> type : annotation.value()) {
                try {
                    if (!type.getConstructor().newInstance().getAsBoolean()) {
                        return disabled(format("Condition %s is false", type.getName()));
                    }
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                        | InvocationTargetException e) {
                    return disabled(format("Unable to evaluate condition: %s", type.getName()));
                }
            }
            return enabled("All conditions match");
        }
    }

}
