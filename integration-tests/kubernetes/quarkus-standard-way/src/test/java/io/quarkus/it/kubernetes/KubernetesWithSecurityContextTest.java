package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithSecurityContextTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-security-context")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-security-context.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Deployment".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {
                assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                    assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(podSpec -> {
                            PodSecurityContext securityContext = podSpec.getSecurityContext();
                            assertThat(securityContext).isNotNull();
                            assertThat(securityContext.getSeLinuxOptions()).isNotNull();
                            assertThat(securityContext.getSeLinuxOptions().getUser()).isEqualTo("user");
                            assertThat(securityContext.getSeLinuxOptions().getRole()).isEqualTo("role");
                            assertThat(securityContext.getSeLinuxOptions().getType()).isEqualTo("type");
                            assertThat(securityContext.getSeLinuxOptions().getLevel()).isEqualTo("level");
                            assertThat(securityContext.getWindowsOptions()).isNotNull();
                            assertThat(securityContext.getWindowsOptions().getGmsaCredentialSpec()).isEqualTo("spec");
                            assertThat(securityContext.getWindowsOptions().getGmsaCredentialSpecName()).isEqualTo("specName");
                            assertThat(securityContext.getWindowsOptions().getHostProcess()).isTrue();
                            assertThat(securityContext.getWindowsOptions().getRunAsUserName()).isEqualTo("user");
                            assertThat(securityContext.getRunAsUser()).isEqualTo(123);
                            assertThat(securityContext.getRunAsGroup()).isEqualTo(124);
                            assertThat(securityContext.getRunAsNonRoot()).isTrue();
                            assertThat(securityContext.getSupplementalGroups()).containsExactly(125l, 126l);
                            assertThat(securityContext.getFsGroup()).isEqualTo(127);
                            assertThat(securityContext.getFsGroupChangePolicy()).isEqualTo("OnRootMismatch");
                        });
                    });
                });
            });
        });
    }

}
