package io.quarkus.openshift.deployment.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;
import io.smallrye.config.source.yaml.YamlConfigSource;

public class OpenshiftConfigFallbackTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .setApplicationName("config")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.replicas", "10")
            .overrideConfigKey("quarkus.openshift.version", "999-SNAPSHOT")
            .overrideConfigKey("quarkus.openshift.labels.app", "openshift")
            .setRun(true);

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    void configFallback() throws Exception {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        YamlConfigSource kubernetes = new YamlConfigSource(kubernetesDir.resolve("kubernetes.yml").toUri().toURL());
        YamlConfigSource openshift = new YamlConfigSource(kubernetesDir.resolve("openshift.yml").toUri().toURL());

        // Only in Kubernetes, must fallback to Openshift
        assertEquals("10", kubernetes.getValue("spec.replicas"));
        assertEquals("10", openshift.getValue("spec.replicas"));

        // In both, each should retain the value
        assertEquals("0.1-SNAPSHOT", kubernetes.getValue("spec.template.metadata.labels.\"app.kubernetes.io/version\""));
        assertEquals("999-SNAPSHOT", openshift.getValue("spec.template.metadata.labels.\"app.kubernetes.io/version\""));

        // Only in Openshift
        assertNull(kubernetes.getValue("spec.template.metadata.labels.app"));
        assertEquals("openshift", openshift.getValue("spec.template.metadata.labels.app"));
    }
}
