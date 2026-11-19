package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class EnvVarDependencyTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("env-dep-test")
            .setApplicationVersion("1.0.0")
            .setRun(false)
            .overrideConfigKey("quarkus.kubernetes.env.vars.Z_HOST", "localhost")
            .overrideConfigKey("quarkus.kubernetes.env.vars.A_URL", "http://$(Z_HOST):8080")
            .overrideConfigKey("quarkus.kubernetes.env.vars.M-PORT", "9090")
            .overrideConfigKey("quarkus.kubernetes.liveness-probe.enabled", "false")
            .overrideConfigKey("quarkus.kubernetes.readiness-probe.enabled", "false");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testEnvVarOrderPreserved() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Deployment deployment = kubernetesList.stream()
                .filter(r -> r instanceof Deployment)
                .map(r -> (Deployment) r)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Deployment found"));

        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        List<EnvVar> envVars = container.getEnv();

        int zHostIndex = -1;
        int aUrlIndex = -1;
        int mPortIndex = -1;

        for (int i = 0; i < envVars.size(); i++) {
            String name = envVars.get(i).getName();
            if ("Z_HOST".equals(name)) {
                zHostIndex = i;
            } else if ("A_URL".equals(name)) {
                aUrlIndex = i;
            } else if ("M_PORT".equals(name)) {
                mPortIndex = i;
            }
        }

        assertThat(zHostIndex)
                .as("Z_HOST not found in env vars")
                .isGreaterThanOrEqualTo(0);
        assertThat(aUrlIndex)
                .as("A_URL not found in env vars")
                .isGreaterThanOrEqualTo(0);
        assertThat(mPortIndex)
                .as("M_PORT not found in env vars")
                .isGreaterThanOrEqualTo(0);

        assertThat(zHostIndex)
                .as("Z_HOST must precede A_URL for variable expansion")
                .isLessThan(aUrlIndex);

        assertThat(envVars.get(zHostIndex).getValue()).isEqualTo("localhost");
        assertThat(envVars.get(aUrlIndex).getValue()).isEqualTo("http://$(Z_HOST):8080");
        assertThat(envVars.get(mPortIndex).getValue()).isEqualTo("9090");
    }
}
