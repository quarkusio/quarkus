package io.quarkus.qute.deployment.propertynotfound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.Variant;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Reproducer for a bug when calling .raw on a missing bean property in an HTML template, the value resolver converts
 * Results.NotFound to string via toString() during method resolution, outputting
 * "NOT_FOUND" instead of respecting property-not-found-strategy=NOOP.
 *
 * This is different from the simpler case of {item.missingProperty} which correctly
 * outputs empty string with NOOP strategy.
 */
public class RawCallOnMissingPropertyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClass(Guide.class))
            .overrideConfigKey("quarkus.qute.property-not-found-strategy", "NOOP")
            .overrideConfigKey("quarkus.qute.strict-rendering", "false");

    @Inject
    Engine engine;

    @Test
    public void testMethodCallOnMissingProperty() {
        // Simulate the Roq case:
        // 1. {#include} passes guide.status (which is NotFound) as a parameter
        // 2. Inside the included template, status is bound to NotFound
        // 3. {status.escapeHtml.raw} tries to call .escapeHtml on NotFound
        Template template = engine.parse(
                "{@io.quarkus.qute.deployment.propertynotfound.RawCallOnMissingPropertyTest$Guide guide}"
                        + "{guide.status.escapeHtml.raw}",
                new Variant(Locale.ENGLISH, "text/html", "UTF-8"));

        String result = template.data("guide", new Guide()).render();

        // With property-not-found-strategy=NOOP, missing properties output empty string
        assertThat(result).as("Method call on missing property should respect NOOP strategy").isEmpty();
    }

    @Test
    public void testDirectMissingPropertyWorks() {
        // This works correctly - direct access to missing property outputs empty string
        Template template = engine.parse(
                "{@io.quarkus.qute.deployment.propertynotfound.RawCallOnMissingPropertyTest$Guide guide}"
                        + "{guide.status}",
                new Variant(Locale.ENGLISH, "text/html", "UTF-8"));

        String result = template.data("guide", new Guide()).render();

        assertThat(result).isEmpty();
    }

    @Test
    public void testConsistency() {
        // These should both output empty string with NOOP strategy
        Template template = engine.parse(
                "{@io.quarkus.qute.deployment.propertynotfound.RawCallOnMissingPropertyTest$Guide guide}"
                        + "Direct: {guide.status} | With method: {guide.status.raw}",
                new Variant(Locale.ENGLISH, "text/html", "UTF-8"));

        String result = template.data("guide", new Guide()).render();

        // Both should be empty
        assertThat(result).isEqualTo("Direct:  | With method: ");
    }

    public static class Guide {
        // No status, keywords, or origin properties - simulating the YAML data
    }

}
