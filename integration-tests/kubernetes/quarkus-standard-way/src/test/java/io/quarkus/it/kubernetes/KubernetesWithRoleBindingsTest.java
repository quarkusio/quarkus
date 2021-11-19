
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithRoleBindingsTest {

    static final String APP_NAME = "kubernetes-with-role-bindings";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(
                    Collections.singletonList(new AppArtifact("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(i -> i instanceof Role).singleElement().satisfies(role -> {
            assertThat(role).isInstanceOfSatisfying(Role.class, r -> {
                assertThat(r.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME);
                });

                assertThat(r.getRules()).singleElement().satisfies(p -> {
                    assertThat(p.getApiGroups()).containsExactlyInAnyOrder("extensions", "apps");
                    assertThat(p.getResources()).containsExactlyInAnyOrder("deployments", "deployments/status");
                    assertThat(p.getVerbs()).containsExactlyInAnyOrder("get", "watch", "list");
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> i instanceof RoleBinding).singleElement().satisfies(binding -> {
            assertThat(binding).isInstanceOfSatisfying(RoleBinding.class, b -> {
                assertThat(b.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME + "-" + APP_NAME);
                });

                assertThat(b.getRoleRef()).satisfies(rr -> {
                    assertThat(rr.getKind()).isEqualTo(Role.class.getSimpleName());
                    assertThat(rr.getApiGroup()).isEqualTo("rbac.authorization.k8s.io");
                    assertThat(rr.getName()).isEqualTo(APP_NAME);
                });

                assertThat(b.getSubjects()).singleElement().satisfies(s -> {
                    assertThat(s.getKind()).isEqualTo(ServiceAccount.class.getSimpleName());
                    assertThat(s.getName()).isEqualTo(APP_NAME);
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> i instanceof ServiceAccount).singleElement().satisfies(serviceAccount -> {
            assertThat(serviceAccount).isInstanceOfSatisfying(ServiceAccount.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME);
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> i instanceof Deployment).singleElement().satisfies(d -> {
            assertThat(d).isInstanceOfSatisfying(Deployment.class, s -> {
                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(spec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(ts -> {
                            assertThat(ts.getServiceAccountName()).isEqualTo(APP_NAME);
                        });
                    });
                });
            });
        });
    }
}
