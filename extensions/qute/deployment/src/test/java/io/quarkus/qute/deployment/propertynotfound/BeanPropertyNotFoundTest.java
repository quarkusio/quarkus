package io.quarkus.qute.deployment.propertynotfound;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests for GitHub issue #55438: property-not-found-strategy should apply to bean properties.
 *
 * Uses dynamic template parsing to avoid build-time validation issues.
 */
public class BeanPropertyNotFoundTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.qute.property-not-found-strategy", "noop")
            .overrideConfigKey("quarkus.qute.strict-rendering", "false");

    @Inject
    Engine engine;

    @Test
    public void testBeanPropertyNoop() {
        Template template = engine.parse("{@java.lang.String item}{item.missingProperty}");
        String result = template.data("item", "test").render();
        assertThat(result).as("Missing bean property with NOOP strategy").isEmpty();
    }

    @Test
    public void testTemplateVariableNoop() {
        Template template = engine.parse("{missingVar}");
        String result = template.render();
        assertThat(result).as("Missing template variable with NOOP strategy").isEmpty();
    }

    @Test
    public void testConsistency() {
        Template template = engine.parse("{@java.lang.String item}{missingVar}:{item.missingProperty}");
        String result = template.data("item", "test").render();
        assertThat(result).as("Both missing var and bean property should be empty").isEqualTo(":");
    }
}
