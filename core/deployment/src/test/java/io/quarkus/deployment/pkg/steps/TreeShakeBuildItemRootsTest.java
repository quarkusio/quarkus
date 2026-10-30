package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

class TreeShakeBuildItemRootsTest {

    @Test
    void allModesRoots() {
        Set<String> roots = new HashSet<>();
        TreeShakeBuildItemRoots.allModes(
                List.of(ReflectiveClassBuildItem.builder("a.Strong").methods().build(),
                        ReflectiveClassBuildItem.builder("a.Weak").methods().weak().build()),
                List.of(new ReflectiveFieldBuildItem("test", "a.FieldOwner", "f")),
                List.of(new ReflectiveMethodBuildItem("test", "a.MethodOwner", "m")),
                List.of(new JniRuntimeAccessBuildItem(true, true, true, "a.Jni")),
                List.of(new JniRuntimeAccessFieldBuildItem("a.JniField", "f")),
                List.of(new JniRuntimeAccessMethodBuildItem("a.JniMethod", "m")),
                List.of(new ServiceProviderBuildItem("a.Service", "a.Provider1", "a.Provider2")),
                roots::add);

        assertThat(roots).containsExactlyInAnyOrder(
                "a.Strong", "a.FieldOwner", "a.MethodOwner", "a.Jni", "a.JniField", "a.JniMethod",
                "a.Provider1", "a.Provider2");
    }

    @Test
    void nativeOnlyRoots() {
        Set<String> roots = new HashSet<>();
        TreeShakeBuildItemRoots.nativeOnly(
                List.of(new RuntimeInitializedClassBuildItem("n.Run")),
                List.of(new RuntimeReinitializedClassBuildItem("n.Reinit")),
                List.of(new NativeImageFeatureBuildItem("n.Feature")),
                List.of(NativeImageConfigBuildItem.builder().addRuntimeInitializedClass("n.ConfigRun").build()),
                roots::add);

        assertThat(roots).containsExactlyInAnyOrder("n.Run", "n.Reinit", "n.Feature", "n.ConfigRun");
    }

    @Test
    void emptyInputsGiveNoRoots() {
        Set<String> roots = new HashSet<>();
        TreeShakeBuildItemRoots.allModes(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                roots::add);
        TreeShakeBuildItemRoots.nativeOnly(List.of(), List.of(), List.of(), List.of(), roots::add);
        assertThat(roots).isEmpty();
    }
}
