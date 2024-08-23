package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesConfigWithSecretsAndClusterRoleTest {

    private static final String APP_NAME = "kubernetes-config-with-secrets-and-cluster-role";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes-config", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).anySatisfy(res -> {
            assertThat(res).isInstanceOfSatisfying(ClusterRole.class, role -> {
                assertThat(role.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("view-secrets");
                });

                assertThat(role.getRules()).singleElement().satisfies(r -> {
                    assertThat(r).isInstanceOfSatisfying(PolicyRule.class, rule -> {
                        assertThat(rule.getApiGroups()).containsExactly("");
                        assertThat(rule.getResources()).containsExactly("secrets");
                        assertThat(rule.getVerbs()).containsExactly("get");
                    });
                });
            });
        });

        assertThat(kubernetesList).filteredOn(h -> "RoleBinding".equals(h.getKind())).hasSize(2)
                .anySatisfy(res -> {
                    assertThat(res).isInstanceOfSatisfying(RoleBinding.class, roleBinding -> {
                        assertThat(roleBinding.getMetadata()).satisfies(m -> {
                            assertThat(m.getName()).isEqualTo(APP_NAME + "-view-secrets");
                        });

                        assertThat(roleBinding.getRoleRef().getKind()).isEqualTo("ClusterRole");
                        assertThat(roleBinding.getRoleRef().getName()).isEqualTo("view-secrets");

                        assertThat(roleBinding.getSubjects()).singleElement().satisfies(subject -> {
                            assertThat(subject.getKind()).isEqualTo("ServiceAccount");
                            assertThat(subject.getName()).isEqualTo(APP_NAME);
                        });
                    });
                });
    }

}
