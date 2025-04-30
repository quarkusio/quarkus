package io.quarkus.openshift.deployment.config;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;
import io.smallrye.config.source.yaml.YamlConfigSource;

public class OpenShiftConfigFallbackTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .setApplicationName("config")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.openshift.version", "999-SNAPSHOT")
            .overrideConfigKey("quarkus.openshift.labels.app", "openshift")
            .overrideConfigKey("quarkus.openshift.route.expose", "true")
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .setRun(true);

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    void configFallback() throws Exception {
        List<LogRecord> logRecords = prodModeTestResults.getRetainedBuildLogRecords();
        Set<Object> unrecognized = logRecords.stream()
                .filter(logRecord -> logRecord.getMessage().startsWith("Unrecognized configuration key"))
                .map(logRecord -> Optional.ofNullable(logRecord.getParameters())
                        .map(parameters -> parameters[0])
                        .orElse(new Object[0]))
                .collect(toSet());

        assertTrue(unrecognized.isEmpty());

        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        YamlConfigSource kubernetes = new YamlConfigSource(kubernetesDir.resolve("kubernetes.yml").toUri().toURL());
        YamlConfigSource openshift = new YamlConfigSource(kubernetesDir.resolve("openshift.yml").toUri().toURL());

        // In both, each should retain the value
        assertEquals("0.1-SNAPSHOT", kubernetes.getValue("spec.template.metadata.labels.\"app.kubernetes.io/version\""));
        assertEquals("999-SNAPSHOT", openshift.getValue("spec.template.metadata.labels.\"app.kubernetes.io/version\""));

        // Only in Openshift
        assertNull(kubernetes.getValue("spec.template.metadata.labels.app"));
        assertEquals("openshift", openshift.getValue("spec.template.metadata.labels.app"));
    }
}
