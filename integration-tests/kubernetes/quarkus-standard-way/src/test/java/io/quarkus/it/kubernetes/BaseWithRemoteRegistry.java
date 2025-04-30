package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PodSpecFluent;
import io.fabric8.kubernetes.api.model.Secret;

public class BaseWithRemoteRegistry {

    public void assertGeneratedResources(String name, String target, Path buildDir) throws IOException {
        List<HasMetadata> resourceList = getResources(target, buildDir);
        assertGeneratedResources(name, resourceList);
    }

    List<HasMetadata> getResources(String target, Path buildDir) throws IOException {
        Path kubernetesDir = buildDir.resolve("kubernetes");

        assertThat(kubernetesDir).isDirectoryContaining(p -> p.getFileName().endsWith(target + ".json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith(target + ".yml"));
        return DeserializationUtil.deserializeAsList(kubernetesDir.resolve(target + ".yml"));
    }

    public void assertGeneratedResources(String name, List<HasMetadata> resourceList) {
        assertThat(resourceList).satisfies(r -> {
            KubernetesList kubernetesList = new KubernetesListBuilder()
                    .addAllToItems(resourceList)
                    .accept(PodSpecFluent.class, spec -> {
                        assertThat(spec.buildImagePullSecrets()).singleElement().satisfies(e -> {
                            assertThat(e).isInstanceOfSatisfying(LocalObjectReference.class, l -> {
                                assertThat(l.getName()).isEqualTo(name + "-pull-secret");
                            });
                        });

                    }).build();
        });

        assertThat(resourceList)
                .filteredOn(
                        h -> "Secret".equals(h.getKind()) && h.getMetadata().getName().equals(name + "-pull-secret"))
                .singleElement().satisfies(h -> {
                    assertThat(h).isInstanceOfSatisfying(Secret.class, s -> {
                        assertThat(s.getType()).isEqualTo("kubernetes.io/dockerconfigjson");
                        assertThat(s.getData()).containsKey(".dockerconfigjson");
                    });
                });
    }
}
