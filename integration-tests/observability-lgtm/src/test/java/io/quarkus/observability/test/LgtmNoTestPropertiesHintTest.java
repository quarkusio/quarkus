package io.quarkus.observability.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.logging.Level;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the hint stays silent when the property is user-defined or optional.
 */
@Tag("extension")
public class LgtmNoTestPropertiesHintTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(GrafanaClient.class)
                    .addAsResource(new StringAsset("hint-test=present\ngrafana.endpoint=localhost:3000\n"),
                            "application.properties"))
            .overrideConfigKey("quarkus.observability.enabled-in-tests", "false")
            .setLogRecordPredicate(record -> record.getLevel() == Level.WARNING
                    && record.getMessage().contains("quarkus.observability.enabled-in-tests"))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Test
    void applicationStarts() {
    }

    @Unremovable
    @ApplicationScoped
    @IfBuildProperty(name = "hint-test", stringValue = "present")
    public static class GrafanaClient {

        @ConfigProperty(name = "grafana.endpoint")
        String endpoint;

        @ConfigProperty(name = "otel-collector.url")
        Optional<String> otelCollectorUrl;
    }
}
