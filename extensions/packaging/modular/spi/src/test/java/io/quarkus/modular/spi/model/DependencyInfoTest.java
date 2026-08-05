package io.quarkus.modular.spi.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.modules.desc.Dependency;
import io.smallrye.modules.desc.PackageAccess;

/**
 * Tests for {@link DependencyInfo}, focusing on the {@code merge()} logic.
 */
class DependencyInfoTest {

    @Test
    void constructorMakesDefensiveCopyOfPackageAccesses() {
        HashMap<String, PackageAccess> original = new HashMap<>();
        original.put("com.foo", PackageAccess.EXPORTED);
        DependencyInfo info = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(), original);
        original.put("com.bar", PackageAccess.OPEN);
        assertThat(info.packageAccesses()).doesNotContainKey("com.bar");
    }

    @Test
    void mergeWithMatchingNamesPreservesModuleName() {
        DependencyInfo a = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo b = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(Dependency.Modifier.READ), Map.of());
        DependencyInfo merged = DependencyInfo.merge(a, b);
        assertThat(merged.moduleName()).isEqualTo("mod.a");
    }

    @Test
    void mergeThrowsOnDifferentModuleNames() {
        DependencyInfo a = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(), Map.of());
        DependencyInfo b = new DependencyInfo("mod.b", Dependency.Modifier.Set.of(), Map.of());
        assertThatThrownBy(() -> DependencyInfo.merge(a, b))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mergeNonSyntheticTakesPriorityOverSynthetic() {
        // when a has SYNTHETIC + LINKED and b has LINKED (non-synthetic), result should have LINKED without SYNTHETIC
        DependencyInfo a = new DependencyInfo("mod.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.SYNTHETIC, Dependency.Modifier.LINKED), Map.of());
        DependencyInfo b = new DependencyInfo("mod.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo merged = DependencyInfo.merge(a, b);
        assertThat(merged.modifiers().contains(Dependency.Modifier.LINKED)).isTrue();
        assertThat(merged.modifiers().contains(Dependency.Modifier.SYNTHETIC)).isFalse();
    }

    @Test
    void mergeBothSyntheticRemainsSynthetic() {
        DependencyInfo a = new DependencyInfo("mod.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.SYNTHETIC, Dependency.Modifier.LINKED), Map.of());
        DependencyInfo b = new DependencyInfo("mod.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.SYNTHETIC, Dependency.Modifier.READ), Map.of());
        DependencyInfo merged = DependencyInfo.merge(a, b);
        assertThat(merged.modifiers().contains(Dependency.Modifier.SYNTHETIC)).isTrue();
        assertThat(merged.modifiers().contains(Dependency.Modifier.LINKED)).isTrue();
        assertThat(merged.modifiers().contains(Dependency.Modifier.READ)).isTrue();
    }

    @Test
    void mergeNeitherSyntheticRemainsNonSynthetic() {
        DependencyInfo a = new DependencyInfo("mod.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo b = new DependencyInfo("mod.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.READ), Map.of());
        DependencyInfo merged = DependencyInfo.merge(a, b);
        assertThat(merged.modifiers().contains(Dependency.Modifier.SYNTHETIC)).isFalse();
        assertThat(merged.modifiers().contains(Dependency.Modifier.LINKED)).isTrue();
        assertThat(merged.modifiers().contains(Dependency.Modifier.READ)).isTrue();
    }

    @Test
    void mergePackageAccessesTakesMax() {
        DependencyInfo a = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(),
                Map.of("com.foo", PackageAccess.PRIVATE));
        DependencyInfo b = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(),
                Map.of("com.foo", PackageAccess.EXPORTED));
        DependencyInfo merged = DependencyInfo.merge(a, b);
        assertThat(merged.packageAccesses().get("com.foo")).isEqualTo(PackageAccess.EXPORTED);
    }

    @Test
    void mergePackageAccessesUnionsDisjointKeys() {
        DependencyInfo a = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(),
                Map.of("com.foo", PackageAccess.EXPORTED));
        DependencyInfo b = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(),
                Map.of("com.bar", PackageAccess.OPEN));
        DependencyInfo merged = DependencyInfo.merge(a, b);
        assertThat(merged.packageAccesses()).containsEntry("com.foo", PackageAccess.EXPORTED);
        assertThat(merged.packageAccesses()).containsEntry("com.bar", PackageAccess.OPEN);
    }

    @Test
    void mergeEmptyPackageAccesses() {
        DependencyInfo a = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(), Map.of());
        DependencyInfo b = new DependencyInfo("mod.a", Dependency.Modifier.Set.of(), Map.of());
        DependencyInfo merged = DependencyInfo.merge(a, b);
        assertThat(merged.packageAccesses()).isEmpty();
    }
}
