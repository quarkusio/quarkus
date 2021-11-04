package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesConfigWithSecretsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-config-with-secrets")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-config-with-secrets.properties")
            .setForcedDependencies(Collections.singletonList(
                    new AppArtifact("io.quarkus", "quarkus-kubernetes-config", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(h -> "Role".equals(h.getKind())).hasSize(1);

        assertThat(kubernetesList).anySatisfy(res -> {
            assertThat(res).isInstanceOfSatisfying(Role.class, role -> {
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
                            assertThat(m.getName()).isEqualTo("kubernetes-config-with-secrets-view-secrets");
                        });

                        assertThat(roleBinding.getRoleRef().getKind()).isEqualTo("Role");
                        assertThat(roleBinding.getRoleRef().getName()).isEqualTo("view-secrets");

                        assertThat(roleBinding.getSubjects()).singleElement().satisfies(subject -> {
                            assertThat(subject.getKind()).isEqualTo("ServiceAccount");
                            assertThat(subject.getName()).isEqualTo("kubernetes-config-with-secrets");
                        });
                    });
                })
                .anySatisfy(res -> {
                    assertThat(res).isInstanceOfSatisfying(RoleBinding.class, roleBinding -> {
                        assertThat(roleBinding.getMetadata()).satisfies(m -> {
                            assertThat(m.getName()).isEqualTo("kubernetes-config-with-secrets-view");
                        });

                        assertThat(roleBinding.getRoleRef().getKind()).isEqualTo("ClusterRole");
                        assertThat(roleBinding.getRoleRef().getName()).isEqualTo("view");

                        assertThat(roleBinding.getSubjects()).singleElement().satisfies(subject -> {
                            assertThat(subject.getKind()).isEqualTo("ServiceAccount");
                            assertThat(subject.getName()).isEqualTo("kubernetes-config-with-secrets");
                        });
                    });
                });
    }

}
