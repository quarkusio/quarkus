package io.quarkus.modular.spi.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.modules.desc.Dependency;
import io.smallrye.modules.desc.ModuleDescriptor;
import io.smallrye.modules.desc.PackageInfo;

/**
 * Tests for {@link ModuleTreeShaker}.
 * <p>
 * These tests use the package-private {@link ModuleTreeShaker#shake(Set)} overload
 * to supply a known set of dead modules, bypassing content-tree walking which
 * requires real JAR files on disk.
 */
class ModuleTreeShakerTest {

    private static final ResolvedDependency APP_DEP = TestResolvedDependency.create("org.app", "app", "1.0");
    private static final ResolvedDependency LIB_A_DEP = TestResolvedDependency.create("org.lib", "lib-a", "1.0");
    private static final ResolvedDependency LIB_B_DEP = TestResolvedDependency.create("org.lib", "lib-b", "1.0");
    private static final ResolvedDependency LIB_C_DEP = TestResolvedDependency.create("org.lib", "lib-c", "1.0");

    /**
     * Create a minimal {@link ModuleInfo} for testing.
     */
    private static ModuleInfo module(String name, ResolvedDependency dep) {
        return new ModuleInfo(
                name, "1.0", ModuleDescriptor.Modifier.Set.of(), dep, null,
                Map.of(), List.of(), List.of(), Set.of(), Map.of(), List.of());
    }

    /**
     * Create a {@link ModuleInfo} with explicit packages, dependencies, and provides.
     */
    private static ModuleInfo module(String name, ResolvedDependency dep,
            Map<String, PackageInfo> packages,
            List<DependencyInfo> dependencies,
            List<AutoDependencyGroup> autoDependencies,
            Map<String, List<String>> provides) {
        return new ModuleInfo(
                name, "1.0", ModuleDescriptor.Modifier.Set.of(), dep, null,
                packages, dependencies, autoDependencies, Set.of(), provides, List.of());
    }

    /**
     * Build a model with the given app module, additional modules, JDK modules, and boot modules.
     */
    private static AppModuleModel buildModel(ModuleInfo appModule, List<ModuleInfo> modules,
            List<String> jdkModules, Set<String> bootModules) {
        AppModuleModel.Builder amb = AppModuleModel.builder();
        amb.appModuleInfo(appModule);
        modules.forEach(amb::moduleInfo);
        jdkModules.forEach(amb::jdkModuleUsed);
        amb.bootModules(bootModules);
        return amb.build();
    }

    // -- no-op cases --

    @Test
    void noDeadModulesReturnsOriginalModel() {
        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of());

        assertThat(result).isSameAs(model);
    }

    // -- dead module removal --

    @Test
    void deadModuleIsRemovedFromModel() {
        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP);
        ModuleInfo libB = module("lib.b", LIB_B_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA, libB), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.b"));

        assertThat(result.modulesByName()).containsKey("app.module");
        assertThat(result.modulesByName()).containsKey("lib.a");
        assertThat(result.modulesByName()).doesNotContainKey("lib.b");
        assertThat(result.modulesByKey()).doesNotContainKey(ArtifactKey.ga("org.lib", "lib-b"));
    }

    @Test
    void multipleDeadModulesRemovedSimultaneously() {
        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP);
        ModuleInfo libB = module("lib.b", LIB_B_DEP);
        ModuleInfo libC = module("lib.c", LIB_C_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA, libB, libC), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.a", "lib.c"));

        assertThat(result.modulesByName()).containsOnlyKeys("app.module", "lib.b");
    }

    // -- protected modules --

    @Test
    void appModuleIsNeverRemoved() {
        ModuleInfo appModule = module("app.module", APP_DEP);
        AppModuleModel model = buildModel(appModule, List.of(), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        // even if the app module were listed as dead, it should remain
        AppModuleModel result = shaker.shake(Set.of("app.module"));

        assertThat(result.modulesByName()).containsKey("app.module");
    }

    @Test
    void deadBootModuleIsRemovedFromBootSet() {
        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA), List.of(), Set.of("lib.a"));

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.a"));

        assertThat(result.modulesByName()).doesNotContainKey("lib.a");
        assertThat(result.bootModules()).isEmpty();
    }

    // -- JDK module recomputation --

    @Test
    void jdkModulesRecomputedAfterPruning() {
        DependencyInfo javaSql = new DependencyInfo("java.sql",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo javaBase = new DependencyInfo("java.base",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP,
                Map.of(), List.of(javaBase), List.of(), Map.of());
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(javaSql), List.of(), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA),
                List.of("java.base", "java.sql"), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.a"));

        assertThat(result.jdkModulesUsed()).containsExactly("java.base");
        assertThat(result.jdkModulesUsed()).doesNotContain("java.sql");
    }

    // -- dependency cleanup --

    @Test
    void staleDependenciesRemovedFromSurvivingModule() {
        DependencyInfo depToDeadModule = new DependencyInfo("lib.b",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo depToAliveModule = new DependencyInfo("java.base",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(depToDeadModule, depToAliveModule), List.of(), Map.of());
        ModuleInfo libB = module("lib.b", LIB_B_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA, libB), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.b"));

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        assertThat(cleanedA.dependencies()).hasSize(1);
        assertThat(cleanedA.dependencies().get(0).moduleName()).isEqualTo("java.base");
    }

    // -- auto-dependency cleanup --

    @Test
    void autoDepGroupForDeadHostKeepsAliveEntries() {
        DependencyInfo aliveEntry = new DependencyInfo("some.dep",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        AutoDependencyGroup group = new AutoDependencyGroup("dead.host", List.of(aliveEntry));

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(), List.of(group), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("dead.host"));

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        assertThat(cleanedA.autoDependencies()).hasSize(1);
        assertThat(cleanedA.autoDependencies().get(0).dependencies()).hasSize(1);
        assertThat(cleanedA.autoDependencies().get(0).dependencies().get(0).moduleName()).isEqualTo("some.dep");
    }

    @Test
    void autoDepGroupDroppedWhenHostAndAllEntriesDead() {
        DependencyInfo deadEntry = new DependencyInfo("dead.dep",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        AutoDependencyGroup group = new AutoDependencyGroup("dead.host", List.of(deadEntry));

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(), List.of(group), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("dead.host", "dead.dep"));

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        assertThat(cleanedA.autoDependencies()).isEmpty();
    }

    @Test
    void autoDepEntriesPointingToDeadModulesAreFiltered() {
        DependencyInfo alive = new DependencyInfo("alive.dep",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo dead = new DependencyInfo("dead.dep",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        AutoDependencyGroup group = new AutoDependencyGroup("host.module", List.of(alive, dead));

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(), List.of(group), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("dead.dep"));

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        assertThat(cleanedA.autoDependencies()).hasSize(1);
        assertThat(cleanedA.autoDependencies().get(0).dependencies()).hasSize(1);
        assertThat(cleanedA.autoDependencies().get(0).dependencies().get(0).moduleName()).isEqualTo("alive.dep");
    }

    // -- provides cleanup --

    @Test
    void deadServiceImplsRemovedFromProvides() {
        Map<String, List<String>> provides = Map.of(
                "com.svc.Service", List.of("com.impl.Alive", "com.impl.Dead"));

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(), List.of(), provides);
        ModuleInfo libB = module("lib.b", LIB_B_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA, libB), List.of(), Set.of());

        // only com.impl.Alive is reachable
        Set<String> reachable = Set.of("com.impl.Alive");
        ModuleTreeShaker shaker = new ModuleTreeShaker(model, reachable, Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.b"));

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        assertThat(cleanedA.provides()).containsKey("com.svc.Service");
        assertThat(cleanedA.provides().get("com.svc.Service")).containsExactly("com.impl.Alive");
    }

    @Test
    void provideEntryDroppedWhenAllImplsDead() {
        Map<String, List<String>> provides = Map.of(
                "com.svc.Service", List.of("com.impl.Dead1", "com.impl.Dead2"));

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(), List.of(), provides);
        ModuleInfo libB = module("lib.b", LIB_B_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA, libB), List.of(), Set.of());

        // no impls are reachable
        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.b"));

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        assertThat(cleanedA.provides()).isEmpty();
    }

    // -- provides unchanged when all impls alive --

    @Test
    void providesUnchangedWhenAllImplsAlive() {
        Map<String, List<String>> provides = Map.of(
                "com.svc.Service", List.of("com.impl.A", "com.impl.B"));

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(), List.of(), provides);
        ModuleInfo libB = module("lib.b", LIB_B_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA, libB), List.of(), Set.of());

        Set<String> reachable = Set.of("com.impl.A", "com.impl.B");
        ModuleTreeShaker shaker = new ModuleTreeShaker(model, reachable, Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.b"));

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        assertThat(cleanedA.provides().get("com.svc.Service")).containsExactly("com.impl.A", "com.impl.B");
    }

    // -- combined cleanup --

    @Test
    void allCleanupOperationsAppliedTogether() {
        DependencyInfo depToDead = new DependencyInfo("lib.b",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo depToJdk = new DependencyInfo("java.base",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        Map<String, List<String>> provides = Map.of(
                "com.svc.Svc", List.of("com.alive.Impl"));

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(depToDead, depToJdk), List.of(), provides);
        ModuleInfo libB = module("lib.b", LIB_B_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA, libB),
                List.of("java.base", "java.sql"), Set.of());

        Set<String> reachable = Set.of("com.alive.Impl");
        ModuleTreeShaker shaker = new ModuleTreeShaker(model, reachable, Set.of());
        AppModuleModel result = shaker.shake(Set.of("lib.b"));

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        // dependency to dead module removed
        assertThat(cleanedA.dependencies()).hasSize(1);
        assertThat(cleanedA.dependencies().get(0).moduleName()).isEqualTo("java.base");
        // provides kept because impl is reachable
        assertThat(cleanedA.provides()).containsKey("com.svc.Svc");
        // java.sql no longer used (only lib.b needed it, and lib.b was dead)
        assertThat(result.jdkModulesUsed()).containsExactly("java.base");
    }

    // -- graph reachability --

    @Test
    void graphUnreachableModuleIsPruned() {
        // app.module has no dependency on lib.c; lib.c is only depended on by dead lib.b
        DependencyInfo depToC = new DependencyInfo("lib.c",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo depToA = new DependencyInfo("lib.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP,
                Map.of(), List.of(depToA), List.of(), Map.of());
        ModuleInfo libA = module("lib.a", LIB_A_DEP);
        ModuleInfo libB = module("lib.b", LIB_B_DEP,
                Map.of(), List.of(depToC), List.of(), Map.of());
        ModuleInfo libC = module("lib.c", LIB_C_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libA, libB, libC), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        Set<String> graphDead = shaker.findGraphUnreachableModules(Set.of("lib.b"));

        assertThat(graphDead).containsExactly("lib.c");
    }

    @Test
    void graphReachableThroughAliveModuleSurvives() {
        // lib.c is depended on by dead lib.b AND alive app.module
        DependencyInfo depToC = new DependencyInfo("lib.c",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP,
                Map.of(), List.of(depToC), List.of(), Map.of());
        ModuleInfo libB = module("lib.b", LIB_B_DEP,
                Map.of(), List.of(depToC), List.of(), Map.of());
        ModuleInfo libC = module("lib.c", LIB_C_DEP);
        AppModuleModel model = buildModel(appModule, List.of(libB, libC), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        Set<String> graphDead = shaker.findGraphUnreachableModules(Set.of("lib.b"));

        assertThat(graphDead).isEmpty();
    }

    @Test
    void transitiveChainThroughDeadModulePruned() {
        // app → lib.b (dead) → lib.c (alive) → lib.a (alive)
        // lib.c and lib.a are only reachable through dead lib.b
        DependencyInfo depToB = new DependencyInfo("lib.b",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo depToC = new DependencyInfo("lib.c",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo depToA = new DependencyInfo("lib.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP,
                Map.of(), List.of(depToB), List.of(), Map.of());
        ModuleInfo libA = module("lib.a", LIB_A_DEP);
        ModuleInfo libB = module("lib.b", LIB_B_DEP,
                Map.of(), List.of(depToC), List.of(), Map.of());
        ModuleInfo libC = module("lib.c", LIB_C_DEP,
                Map.of(), List.of(depToA), List.of(), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA, libB, libC), List.of(), Set.of());

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        Set<String> graphDead = shaker.findGraphUnreachableModules(Set.of("lib.b"));

        assertThat(graphDead).containsExactlyInAnyOrder("lib.a", "lib.c");
    }

    @Test
    void bootModuleIsGraphRoot() {
        // lib.a is not reachable from app.module, but is reachable from boot module lib.b
        DependencyInfo depToA = new DependencyInfo("lib.a",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP);
        ModuleInfo libB = module("lib.b", LIB_B_DEP,
                Map.of(), List.of(depToA), List.of(), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA, libB), List.of(), Set.of("lib.b"));

        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        Set<String> graphDead = shaker.findGraphUnreachableModules(Set.of());

        assertThat(graphDead).isEmpty();
    }

    // -- java.se expansion --

    @Test
    void javaseExpandedToSpecificModules() {
        DependencyInfo javaSeDep = new DependencyInfo("java.se",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED, Dependency.Modifier.SYNTHETIC), Map.of());
        DependencyInfo javaBaseDep = new DependencyInfo("java.base",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED, Dependency.Modifier.MANDATED), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP,
                Map.of(), List.of(javaBaseDep), List.of(), Map.of());
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(javaSeDep, javaBaseDep), List.of(), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA),
                List.of("java.base", "java.se"), Set.of());

        Set<String> jdkPackages = Set.of("java.util", "java.sql", "java.io");
        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), jdkPackages);
        AppModuleModel result = shaker.shake(Set.of());

        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        List<String> depNames = cleanedA.dependencies().stream().map(DependencyInfo::moduleName).toList();
        assertThat(depNames).contains("java.base", "java.sql");
        assertThat(depNames).doesNotContain("java.se");
        assertThat(result.jdkModulesUsed()).doesNotContain("java.se");
        assertThat(result.jdkModulesUsed()).contains("java.base", "java.sql");
    }

    @Test
    void javaseExpansionDropsUnusedJdkModulesAfterDeadModuleRemoval() {
        DependencyInfo javaSeDep = new DependencyInfo("java.se",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED, Dependency.Modifier.SYNTHETIC), Map.of());
        DependencyInfo javaSqlDep = new DependencyInfo("java.sql",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED), Map.of());
        DependencyInfo javaBaseDep = new DependencyInfo("java.base",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED, Dependency.Modifier.MANDATED), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP,
                Map.of(), List.of(javaBaseDep), List.of(), Map.of());
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(javaSeDep, javaBaseDep), List.of(), Map.of());
        ModuleInfo libB = module("lib.b", LIB_B_DEP,
                Map.of(), List.of(javaSqlDep), List.of(), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA, libB),
                List.of("java.base", "java.se", "java.sql"), Set.of());

        // only java.util and java.io are referenced — NOT java.sql
        Set<String> jdkPackages = Set.of("java.util", "java.io");
        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), jdkPackages);
        // lib.b is dead — it was the only module requiring java.sql
        AppModuleModel result = shaker.shake(Set.of("lib.b"));

        assertThat(result.jdkModulesUsed()).contains("java.base");
        assertThat(result.jdkModulesUsed()).doesNotContain("java.sql");
        assertThat(result.jdkModulesUsed()).doesNotContain("java.se");
    }

    @Test
    void noExpansionWhenNoJdkPackagesReferenced() {
        DependencyInfo javaSeDep = new DependencyInfo("java.se",
                Dependency.Modifier.Set.of(Dependency.Modifier.LINKED, Dependency.Modifier.SYNTHETIC), Map.of());

        ModuleInfo appModule = module("app.module", APP_DEP);
        ModuleInfo libA = module("lib.a", LIB_A_DEP,
                Map.of(), List.of(javaSeDep), List.of(), Map.of());
        AppModuleModel model = buildModel(appModule, List.of(libA),
                List.of("java.se"), Set.of());

        // empty JDK packages — tree shaking was not performed or no JDK refs found
        ModuleTreeShaker shaker = new ModuleTreeShaker(model, Set.of(), Set.of());
        AppModuleModel result = shaker.shake(Set.of());

        // java.se should remain untouched
        ModuleInfo cleanedA = result.modulesByName().get("lib.a");
        assertThat(cleanedA.dependencies()).hasSize(1);
        assertThat(cleanedA.dependencies().get(0).moduleName()).isEqualTo("java.se");
    }
}
