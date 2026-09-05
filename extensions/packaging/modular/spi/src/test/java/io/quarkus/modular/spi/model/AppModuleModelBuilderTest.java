package io.quarkus.modular.spi.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.modules.desc.Dependency;
import io.smallrye.modules.desc.ModuleDescriptor;
import io.smallrye.modules.desc.PackageAccess;
import io.smallrye.modules.desc.PackageInfo;

/**
 * Tests for {@link AppModuleModel.Builder}.
 */
class AppModuleModelBuilderTest {

    private static ModuleInfo moduleInfo(String groupId, String artifactId, String name) {
        ResolvedDependency rd = TestResolvedDependency.create(groupId, artifactId, "1.0");
        return new ModuleInfo(
                name, "1.0", ModuleDescriptor.Modifier.Set.of(), rd, null,
                Map.of(), List.of(), List.of(), Set.of(), Map.of(), List.of());
    }

    private static ModuleInfo moduleInfo(String groupId, String artifactId, String name,
            Map<String, PackageInfo> packages, List<DependencyInfo> dependencies) {
        ResolvedDependency rd = TestResolvedDependency.create(groupId, artifactId, "1.0");
        return new ModuleInfo(
                name, "1.0", ModuleDescriptor.Modifier.Set.of(), rd, null,
                packages, dependencies, List.of(), Set.of(), Map.of(), List.of());
    }

    @Test
    void appModuleInfoIsIndexedInMaps() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .build();

        assertThat(model.appModuleInfo()).isSameAs(appModule);
        assertThat(model.modulesByName()).containsKey("app.module");
        assertThat(model.modulesByKey()).containsKey(ArtifactKey.ga("org.app", "app"));
    }

    @Test
    void moduleInfoAddsToMaps() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        ModuleInfo libModule = moduleInfo("org.lib", "lib", "lib.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .moduleInfo(libModule)
                .build();

        assertThat(model.modulesByName()).containsKeys("app.module", "lib.module");
        assertThat(model.modulesByKey()).containsKeys(
                ArtifactKey.ga("org.app", "app"),
                ArtifactKey.ga("org.lib", "lib"));
    }

    @Test
    void laterModuleInfoOverwritesEarlierForSameName() {
        ModuleInfo first = moduleInfo("org.v1", "lib", "lib.module");
        ModuleInfo second = moduleInfo("org.v2", "lib2", "lib.module");
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .moduleInfo(first)
                .moduleInfo(second)
                .build();

        assertThat(model.modulesByName().get("lib.module").key())
                .isEqualTo(ArtifactKey.ga("org.v2", "lib2"));
    }

    // -- JDK module filtering and sorting --

    @Test
    void jdkModuleUsedAcceptsJavaPrefix() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .jdkModuleUsed("java.base")
                .build();
        assertThat(model.jdkModulesUsed()).containsExactly("java.base");
    }

    @Test
    void jdkModuleUsedAcceptsJdkPrefix() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .jdkModuleUsed("jdk.management")
                .build();
        assertThat(model.jdkModulesUsed()).containsExactly("jdk.management");
    }

    @Test
    void jdkModuleUsedAcceptsIbmPrefix() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .jdkModuleUsed("ibm.jsse2")
                .build();
        assertThat(model.jdkModulesUsed()).containsExactly("ibm.jsse2");
    }

    @Test
    void jdkModuleUsedIgnoresNonJdkModules() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .jdkModuleUsed("org.foo")
                .build();
        assertThat(model.jdkModulesUsed()).isEmpty();
    }

    @Test
    void jdkModuleUsedMaintainsSortedOrder() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .jdkModuleUsed("java.sql")
                .jdkModuleUsed("java.base")
                .jdkModuleUsed("java.net.http")
                .build();
        assertThat(model.jdkModulesUsed()).containsExactly("java.base", "java.net.http", "java.sql");
    }

    @Test
    void jdkModuleUsedIgnoresDuplicates() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .jdkModuleUsed("java.base")
                .jdkModuleUsed("java.base")
                .build();
        assertThat(model.jdkModulesUsed()).containsExactly("java.base");
    }

    // -- boot modules --

    @Test
    void bootModuleAddsSingleModule() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .bootModule("boot.a")
                .build();
        assertThat(model.bootModules()).containsExactly("boot.a");
    }

    @Test
    void bootModulesAddsMultipleModules() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .bootModules(Set.of("boot.a", "boot.b"))
                .build();
        assertThat(model.bootModules()).containsExactlyInAnyOrder("boot.a", "boot.b");
    }

    @Test
    void bootModulesAccumulatesAcrossCalls() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .bootModule("boot.a")
                .bootModules(Set.of("boot.b", "boot.c"))
                .bootModule("boot.d")
                .build();
        assertThat(model.bootModules()).containsExactlyInAnyOrder("boot.a", "boot.b", "boot.c", "boot.d");
    }

    // -- stale package access cleanup --
    //
    // Package access entries (add-exports/add-opens) can reference packages that don't
    // exist in the target module. This happens when:
    //  - a hardcoded workaround in ModularitySteps references a package that was removed
    //    or renamed in a newer version of the library
    //  - tree-shaking removes all classes from a package, eliminating it from the
    //    target module's package map
    // Stale entries cause warnings from the module runtime at startup. The builder
    // strips them so the written descriptors stay consistent with actual module content.

    @Test
    void stalePackageAccessRemovedOnBuild() {
        // lib.module declares package "com.lib.api" but NOT "com.lib.internal"
        ModuleInfo libModule = moduleInfo("org.lib", "lib", "lib.module",
                Map.of("com.lib.api", PackageInfo.EXPORTED), List.of());

        // app.module depends on lib.module and requests access to both packages
        DependencyInfo depToLib = new DependencyInfo("lib.module",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED),
                Map.of("com.lib.api", PackageAccess.EXPORTED,
                        "com.lib.internal", PackageAccess.EXPORTED));
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module",
                Map.of(), List.of(depToLib));

        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .moduleInfo(libModule)
                .build();

        ModuleInfo cleanedApp = model.modulesByName().get("app.module");
        DependencyInfo cleanedDep = cleanedApp.dependencies().stream()
                .filter(d -> d.moduleName().equals("lib.module"))
                .findFirst().orElseThrow();
        assertThat(cleanedDep.packageAccesses()).containsKey("com.lib.api");
        assertThat(cleanedDep.packageAccesses()).doesNotContainKey("com.lib.internal");
    }

    @Test
    void validPackageAccessesPreservedOnBuild() {
        ModuleInfo libModule = moduleInfo("org.lib", "lib", "lib.module",
                Map.of("com.lib.api", PackageInfo.EXPORTED,
                        "com.lib.spi", PackageInfo.EXPORTED),
                List.of());

        DependencyInfo depToLib = new DependencyInfo("lib.module",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED),
                Map.of("com.lib.api", PackageAccess.EXPORTED,
                        "com.lib.spi", PackageAccess.OPEN));
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module",
                Map.of(), List.of(depToLib));

        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .moduleInfo(libModule)
                .build();

        ModuleInfo cleanedApp = model.modulesByName().get("app.module");
        DependencyInfo cleanedDep = cleanedApp.dependencies().stream()
                .filter(d -> d.moduleName().equals("lib.module"))
                .findFirst().orElseThrow();
        assertThat(cleanedDep.packageAccesses()).containsOnlyKeys("com.lib.api", "com.lib.spi");
    }

    @Test
    void packageAccessToExternalModuleNotCleaned() {
        // dependency to a module not in the model (e.g. JDK) should be left alone
        DependencyInfo depToJdk = new DependencyInfo("java.base",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED),
                Map.of("java.lang", PackageAccess.EXPORTED));
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module",
                Map.of(), List.of(depToJdk));

        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .build();

        ModuleInfo cleanedApp = model.modulesByName().get("app.module");
        DependencyInfo cleanedDep = cleanedApp.dependencies().get(0);
        assertThat(cleanedDep.packageAccesses()).containsKey("java.lang");
    }

    // -- immutability --

    @Test
    void builtModelHasImmutableCollections() {
        ModuleInfo appModule = moduleInfo("org.app", "app", "app.module");
        AppModuleModel model = AppModuleModel.builder()
                .appModuleInfo(appModule)
                .jdkModuleUsed("java.base")
                .bootModule("boot.a")
                .build();

        assertThatThrownBy(() -> model.modulesByName().put("foo", appModule))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> model.modulesByKey().put(ArtifactKey.ga("a", "b"), appModule))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> model.jdkModulesUsed().add("java.sql"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> model.bootModules().add("boot.b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
