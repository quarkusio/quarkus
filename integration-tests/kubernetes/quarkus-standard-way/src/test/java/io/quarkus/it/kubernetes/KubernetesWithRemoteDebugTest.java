package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithRemoteDebugTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-debug")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-debug.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(
                kubernetesDir.resolve("kubernetes.yml"));

        assertThat(openshiftList).filteredOn(h -> "Deployment".equals(h.getKind())).singleElement().satisfies(h -> {
            Deployment deployment = (Deployment) h;
            assertThat(deployment.getSpec().getTemplate().getSpec().getContainers()).singleElement().satisfies(container -> {
                List<EnvVar> envVars = container.getEnv();
                assertThat(envVars).anySatisfy(envVar -> {
                    assertThat(envVar.getName()).isEqualTo("JAVA_TOOL_OPTIONS");
                    assertThat(envVar.getValue())
                            .isEqualTo("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000");
                });
            });
        });

        assertThat(openshiftList).filteredOn(h -> "Service".equals(h.getKind())).singleElement().satisfies(h -> {
            Service service = (Service) h;
            assertThat(service.getSpec().getPorts()).anySatisfy(p -> {
                assertThat(p.getName()).isEqualTo("http");
                assertThat(p.getPort()).isEqualTo(80);
                assertThat(p.getTargetPort().getIntVal()).isEqualTo(8080);
            });

            assertThat(service.getSpec().getPorts()).anySatisfy(p -> {
                assertThat(p.getName()).isEqualTo("debug");
                assertThat(p.getPort()).isEqualTo(8000);
                assertThat(p.getTargetPort().getIntVal()).isEqualTo(8000);
            });
        });
    }
}
