package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ImageStream;

public class BaseOpenshiftWithRemoteRegistry extends BaseWithRemoteRegistry {

    @Override
    public void assertGeneratedResources(String name, String tag, Path buildDir) throws IOException {
        List<HasMetadata> resourceList = getResources("openshift", buildDir);
        assertGeneratedResources(name, tag, resourceList);
    }

    public void assertGeneratedResources(String name, String tag, List<HasMetadata> resourceList) throws IOException {
        super.assertGeneratedResources(name, resourceList);

        assertThat(resourceList)
                .filteredOn(
                        h -> "Secret".equals(h.getKind()) && h.getMetadata().getName().equals(name + "-push-secret"))
                .singleElement().satisfies(h -> {
                    assertThat(h).isInstanceOfSatisfying(Secret.class, s -> {
                        assertThat(s.getType()).isEqualTo("kubernetes.io/dockerconfigjson");
                        assertThat(s.getData()).containsKey(".dockerconfigjson");
                    });
                });

        assertThat(resourceList).filteredOn(h -> "BuildConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(name);
            });
            assertThat(h).isInstanceOfSatisfying(BuildConfig.class, b -> {
                assertThat(b.getSpec().getOutput().getTo().getKind()).isEqualTo("DockerImage");
                assertThat(b.getSpec().getOutput().getTo().getName()).isEqualTo("quay.io/user/" + name + ":" + tag);
            });
        });

        assertThat(resourceList)
                .filteredOn(h -> "ImageStream".equals(h.getKind()) && h.getMetadata().getName().equals(name))
                .singleElement().satisfies(h -> {
                    assertThat(h).isInstanceOfSatisfying(ImageStream.class, i -> {
                        assertThat(i.getSpec().getDockerImageRepository()).isEqualTo("quay.io/user/" + name);
                    });
                });
    }
}
