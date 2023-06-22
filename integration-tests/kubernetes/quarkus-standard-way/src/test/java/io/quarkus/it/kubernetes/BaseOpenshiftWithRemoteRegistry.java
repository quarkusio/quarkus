package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;

public class BaseOpenshiftWithRemoteRegistry {

    public void assertGeneratedResources(String name, String tag, Path buildDir) throws IOException {
        Path kubernetesDir = buildDir.resolve("kubernetes");

        assertThat(kubernetesDir).isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "DeploymentConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(name);
            });
            assertThat(h).isInstanceOfSatisfying(DeploymentConfig.class, d -> {
                assertThat(d.getSpec().getTemplate().getSpec().getImagePullSecrets()).singleElement().satisfies(l -> {
                    assertThat(l.getName()).isEqualTo(name + "-push-secret");
                });
            });
        });

        assertThat(openshiftList).filteredOn(h -> "Secret".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(name + "-push-secret");
            });
            assertThat(h).isInstanceOfSatisfying(Secret.class, s -> {
                assertThat(s.getType()).isEqualTo("kubernetes.io/dockerconfigjson");
                assertThat(s.getData()).containsKey(".dockerconfigjson");
            });
        });

        assertThat(openshiftList).filteredOn(h -> "BuildConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(name);
            });
            assertThat(h).isInstanceOfSatisfying(BuildConfig.class, b -> {
                assertThat(b.getSpec().getOutput().getTo().getKind()).isEqualTo("DockerImage");
                assertThat(b.getSpec().getOutput().getTo().getName()).isEqualTo("quay.io/user/" + name + ":" + tag);
            });
        });

        assertThat(openshiftList)
                .filteredOn(h -> "ImageStream".equals(h.getKind()) && h.getMetadata().getName().equals(name))
                .singleElement().satisfies(h -> {
                    assertThat(h).isInstanceOfSatisfying(ImageStream.class, i -> {
                        assertThat(i.getSpec().getDockerImageRepository()).isEqualTo("quay.io/user/" + name);
                    });
                });
    }
}
