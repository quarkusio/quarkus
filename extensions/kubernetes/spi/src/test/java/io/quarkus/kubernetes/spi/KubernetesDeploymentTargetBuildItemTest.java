package io.quarkus.kubernetes.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class KubernetesDeploymentTargetBuildItemTest {

    @Test
    void testMergeList() {
        List<KubernetesDeploymentTargetBuildItem> input = Arrays.asList(
                new KubernetesDeploymentTargetBuildItem("n1", "k1", 0, false),
                new KubernetesDeploymentTargetBuildItem("n2", "k2", 10, false),
                new KubernetesDeploymentTargetBuildItem("n1", "k1", -10, false),
                new KubernetesDeploymentTargetBuildItem("n3", "k3", Integer.MIN_VALUE, false),
                new KubernetesDeploymentTargetBuildItem("n4", "k4", Integer.MIN_VALUE, true),
                new KubernetesDeploymentTargetBuildItem("n2", "k2", -10, true),
                new KubernetesDeploymentTargetBuildItem("n4", "k4", Integer.MAX_VALUE, true),
                new KubernetesDeploymentTargetBuildItem("n1", "k1", 100, false));

        List<KubernetesDeploymentTargetBuildItem> result = KubernetesDeploymentTargetBuildItem.mergeList(input);

        assertThat(result).containsOnly(
                new KubernetesDeploymentTargetBuildItem("n1", "k1", 100, false),
                new KubernetesDeploymentTargetBuildItem("n2", "k2", 10, true),
                new KubernetesDeploymentTargetBuildItem("n3", "k3", Integer.MIN_VALUE, false),
                new KubernetesDeploymentTargetBuildItem("n4", "k4", Integer.MAX_VALUE, true));
    }
}
