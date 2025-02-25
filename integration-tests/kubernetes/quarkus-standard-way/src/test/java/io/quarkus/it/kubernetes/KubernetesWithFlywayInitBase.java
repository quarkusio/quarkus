package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;

public class KubernetesWithFlywayInitBase {

    public void assertGeneratedResources(Path kubernetesDir, String name, String taskName, String imagePullSecret,
            String serviceAccount)
            throws IOException {
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        String jobName = name + "-" + taskName + "-init";
        Optional<Deployment> deployment = kubernetesList.stream()
                .filter(d -> "Deployment".equals(d.getKind())
                        && name.equals(d.getMetadata().getName()))
                .map(d -> (Deployment) d).findAny();

        assertThat(deployment).isPresent().get().satisfies(d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(name);
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getImagePullSecrets()).singleElement()
                                .satisfies(s -> assertThat(s.getName()).isEqualTo(imagePullSecret));
                        assertThat(podSpec.getServiceAccountName()).isEqualTo(serviceAccount);
                        assertThat(podSpec.getInitContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getName()).isEqualTo("wait-for-" + taskName);
                            assertThat(container.getImage()).isEqualTo("groundnuty/k8s-wait-for:no-root-v1.7");
                        });

                    });
                });
            });
        });

        Optional<Job> job = kubernetesList.stream()
                .filter(j -> "Job".equals(j.getKind()) && jobName.equals(j.getMetadata().getName()))
                .map(j -> (Job) j)
                .findAny();
        assertThat(job).isPresent().get().satisfies(j -> {
            assertThat(j.getSpec()).satisfies(jobSpec -> {
                assertThat(jobSpec.getCompletionMode()).isEqualTo("NonIndexed");
                assertThat(jobSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getImagePullSecrets()).singleElement()
                                .satisfies(s -> assertThat(s.getName()).isEqualTo(imagePullSecret));
                        assertThat(podSpec.getServiceAccountName()).isEqualTo(serviceAccount);
                        assertThat(podSpec.getRestartPolicy()).isEqualTo("OnFailure");
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getName()).isEqualTo(jobName);
                            assertThat(container.getEnv()).filteredOn(env -> "QUARKUS_FLYWAY_ACTIVE".equals(env.getName()))
                                    .singleElement().satisfies(env -> {
                                        assertThat(env.getValue()).isEqualTo("true");
                                    });
                            assertThat(container.getEnv())
                                    .filteredOn(env -> "QUARKUS_INIT_AND_EXIT".equals(env.getName())).singleElement()
                                    .satisfies(env -> {
                                        assertThat(env.getValue()).isEqualTo("true");
                                    });
                        });
                    });
                });
            });
        });

        Optional<RoleBinding> roleBinding = kubernetesList.stream().filter(
                r -> r instanceof RoleBinding && (name + "-view-jobs").equals(r.getMetadata().getName()))
                .map(r -> (RoleBinding) r).findFirst();
        assertTrue(roleBinding.isPresent());
    }
}
