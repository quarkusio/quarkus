package io.quarkus.modular.spi.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.resource.MemoryResource;
import io.smallrye.common.resource.Resource;
import io.smallrye.modules.desc.Dependency;
import io.smallrye.modules.desc.ModuleDescriptor;
import io.smallrye.modules.desc.PackageAccess;
import io.smallrye.modules.desc.PackageInfo;

/**
 * Tests for {@link ModuleInfo}, focusing on copy-on-write transformation methods.
 */
class ModuleInfoTest {

    private static final ResolvedDependency ARTIFACT = TestResolvedDependency.create("org.test", "test-lib", "1.0");

    private ModuleInfo base() {
        return new ModuleInfo(
                "test.module",
                "1.0",
                ModuleDescriptor.Modifier.Set.of(),
                ARTIFACT,
                null,
                Map.of(),
                List.of(),
                List.of(),
                Set.of(),
                Map.of(),
                List.of());
    }

    @Test
    void keyReturnsCorrectArtifactKey() {
        ModuleInfo info = base();
        ArtifactKey key = info.key();
        assertThat(key.getGroupId()).isEqualTo("org.test");
        assertThat(key.getArtifactId()).isEqualTo("test-lib");
    }

    @Test
    void constructorMakesDefensiveCopies() {
        HashMap<String, PackageInfo> packages = new HashMap<>();
        packages.put("com.foo", new PackageInfo(PackageAccess.EXPORTED, Set.of(), Set.of()));
        ArrayList<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("dep.a", Dependency.Modifier.Set.of(), Map.of()));
        ArrayList<Resource> resources = new ArrayList<>();
        resources.add(new MemoryResource("test.txt", new byte[0]));

        ModuleInfo info = new ModuleInfo(
                "test.module", "1.0", ModuleDescriptor.Modifier.Set.of(), ARTIFACT, null,
                packages, deps, List.of(), Set.of(), Map.of(), resources);

        // mutating originals should not affect the record
        packages.put("com.bar", new PackageInfo(PackageAccess.PRIVATE, Set.of(), Set.of()));
        deps.add(new DependencyInfo("dep.b", Dependency.Modifier.Set.of(), Map.of()));
        resources.add(new MemoryResource("test2.txt", new byte[0]));

        assertThat(info.packages()).doesNotContainKey("com.bar");
        assertThat(info.dependencies()).hasSize(1);
        assertThat(info.generated()).hasSize(1);
    }

    // -- withModifier --

    @Test
    void withModifierAddsNewModifier() {
        ModuleInfo info = base();
        ModuleInfo result = info.withModifier(ModuleDescriptor.Modifier.OPEN);
        assertThat(result).isNotSameAs(info);
        assertThat(result.modifiers().contains(ModuleDescriptor.Modifier.OPEN)).isTrue();
    }

    @Test
    void withModifierReturnsSameInstanceIfAlreadyPresent() {
        ModuleInfo info = new ModuleInfo(
                "test.module", "1.0", ModuleDescriptor.Modifier.Set.of(ModuleDescriptor.Modifier.OPEN),
                ARTIFACT, null, Map.of(), List.of(), List.of(), Set.of(), Map.of(), List.of());
        ModuleInfo result = info.withModifier(ModuleDescriptor.Modifier.OPEN);
        assertThat(result).isSameAs(info);
    }

    // -- withMainClass --

    @Test
    void withMainClassChangesMainClass() {
        ModuleInfo info = base();
        ModuleInfo result = info.withMainClass("com.example.Main");
        assertThat(result).isNotSameAs(info);
        assertThat(result.mainClassName()).isEqualTo("com.example.Main");
    }

    @Test
    void withMainClassReturnsSameInstanceIfUnchanged() {
        ModuleInfo info = new ModuleInfo(
                "test.module", "1.0", ModuleDescriptor.Modifier.Set.of(), ARTIFACT, "com.example.Main",
                Map.of(), List.of(), List.of(), Set.of(), Map.of(), List.of());
        ModuleInfo result = info.withMainClass("com.example.Main");
        assertThat(result).isSameAs(info);
    }

    @Test
    void withMainClassHandlesNullToNonNull() {
        ModuleInfo info = base();
        assertThat(info.mainClassName()).isNull();
        ModuleInfo result = info.withMainClass("com.example.Main");
        assertThat(result).isNotSameAs(info);
        assertThat(result.mainClassName()).isEqualTo("com.example.Main");
    }

    // -- withMorePackages --

    @Test
    void withMorePackagesAddsNewPackages() {
        ModuleInfo info = base();
        PackageInfo pi = new PackageInfo(PackageAccess.EXPORTED, Set.of(), Set.of());
        ModuleInfo result = info.withMorePackages(Map.of("com.foo", pi));
        assertThat(result).isNotSameAs(info);
        assertThat(result.packages()).containsKey("com.foo");
        assertThat(result.packages().get("com.foo")).isEqualTo(pi);
    }

    @Test
    void withMorePackagesReturnsSameInstanceIfEmpty() {
        ModuleInfo info = base();
        ModuleInfo result = info.withMorePackages(Map.of());
        assertThat(result).isSameAs(info);
    }

    @Test
    void withMorePackagesMergesOverlappingKeys() {
        PackageInfo existing = new PackageInfo(PackageAccess.PRIVATE, Set.of(), Set.of());
        ModuleInfo info = new ModuleInfo(
                "test.module", "1.0", ModuleDescriptor.Modifier.Set.of(), ARTIFACT, null,
                Map.of("com.foo", existing), List.of(), List.of(), Set.of(), Map.of(), List.of());
        PackageInfo incoming = new PackageInfo(PackageAccess.EXPORTED, Set.of(), Set.of());
        ModuleInfo result = info.withMorePackages(Map.of("com.foo", incoming));
        // mergedWith should have been called; the result depends on PackageInfo.mergedWith() semantics
        assertThat(result.packages()).containsKey("com.foo");
        assertThat(result).isNotSameAs(info);
    }

    // -- withMoreServices --

    @Test
    void withMoreServicesAddsNewProviders() {
        ModuleInfo info = base();
        ModuleInfo result = info.withMoreServices(Map.of("com.svc.Svc", List.of("com.svc.Impl")));
        assertThat(result).isNotSameAs(info);
        assertThat(result.provides()).containsKey("com.svc.Svc");
        assertThat(result.provides().get("com.svc.Svc")).containsExactly("com.svc.Impl");
    }

    @Test
    void withMoreServicesReturnsSameInstanceIfEmpty() {
        ModuleInfo info = base();
        ModuleInfo result = info.withMoreServices(Map.of());
        assertThat(result).isSameAs(info);
    }

    @Test
    void withMoreServicesMergesOverlappingKeys() {
        ModuleInfo info = new ModuleInfo(
                "test.module", "1.0", ModuleDescriptor.Modifier.Set.of(), ARTIFACT, null,
                Map.of(), List.of(), List.of(), Set.of(),
                Map.of("com.svc.Svc", List.of("com.svc.Impl1")), List.of());
        ModuleInfo result = info.withMoreServices(Map.of("com.svc.Svc", List.of("com.svc.Impl2")));
        assertThat(result.provides().get("com.svc.Svc")).containsExactly("com.svc.Impl1", "com.svc.Impl2");
    }

    // -- withMoreDependencies --

    @Test
    void withMoreDependenciesAddsNewDependencies() {
        ModuleInfo info = base();
        DependencyInfo dep = new DependencyInfo("dep.a", Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        ModuleInfo result = info.withMoreDependencies(List.of(dep));
        assertThat(result).isNotSameAs(info);
        assertThat(result.dependencies()).hasSize(1);
        assertThat(result.dependencies().get(0).moduleName()).isEqualTo("dep.a");
    }

    @Test
    void withMoreDependenciesReturnsSameInstanceIfEmpty() {
        ModuleInfo info = base();
        ModuleInfo result = info.withMoreDependencies(List.of());
        assertThat(result).isSameAs(info);
    }

    @Test
    void withMoreDependenciesMergesExistingByModuleName() {
        DependencyInfo existing = new DependencyInfo("dep.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        ModuleInfo info = new ModuleInfo(
                "test.module", "1.0", ModuleDescriptor.Modifier.Set.of(), ARTIFACT, null,
                Map.of(), List.of(existing), List.of(), Set.of(), Map.of(), List.of());
        DependencyInfo incoming = new DependencyInfo("dep.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.READ), Map.of());
        ModuleInfo result = info.withMoreDependencies(List.of(incoming));
        // should be merged, not duplicated
        assertThat(result.dependencies()).hasSize(1);
        DependencyInfo merged = result.dependencies().get(0);
        assertThat(merged.modifiers().contains(Dependency.Modifier.LINKED)).isTrue();
        assertThat(merged.modifiers().contains(Dependency.Modifier.READ)).isTrue();
    }

    @Test
    void withMoreDependenciesPreservesOrder() {
        DependencyInfo dep1 = new DependencyInfo("dep.a", Dependency.Modifier.Set.of(), Map.of());
        DependencyInfo dep2 = new DependencyInfo("dep.b", Dependency.Modifier.Set.of(), Map.of());
        ModuleInfo info = new ModuleInfo(
                "test.module", "1.0", ModuleDescriptor.Modifier.Set.of(), ARTIFACT, null,
                Map.of(), List.of(dep1), List.of(), Set.of(), Map.of(), List.of());
        ModuleInfo result = info.withMoreDependencies(List.of(dep2));
        assertThat(result.dependencies()).hasSize(2);
        assertThat(result.dependencies().get(0).moduleName()).isEqualTo("dep.a");
        assertThat(result.dependencies().get(1).moduleName()).isEqualTo("dep.b");
    }

    // -- withMoreResources --

    @Test
    void withMoreResourcesAppendsResources() {
        Resource r1 = new MemoryResource("a.txt", new byte[] { 1 });
        ModuleInfo info = new ModuleInfo(
                "test.module", "1.0", ModuleDescriptor.Modifier.Set.of(), ARTIFACT, null,
                Map.of(), List.of(), List.of(), Set.of(), Map.of(), List.of(r1));
        Resource r2 = new MemoryResource("b.txt", new byte[] { 2 });
        ModuleInfo result = info.withMoreResources(List.of(r2));
        assertThat(result.generated()).hasSize(2);
        assertThat(result.generated().get(0).pathName()).isEqualTo("a.txt");
        assertThat(result.generated().get(1).pathName()).isEqualTo("b.txt");
    }

    @Test
    void withMoreResourcesReturnsSameInstanceIfEmpty() {
        ModuleInfo info = base();
        ModuleInfo result = info.withMoreResources(List.of());
        assertThat(result).isSameAs(info);
    }
}
