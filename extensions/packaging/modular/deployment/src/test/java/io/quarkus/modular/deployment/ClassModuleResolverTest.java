package io.quarkus.modular.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.modular.spi.items.ClassAccessBuildItem;
import io.quarkus.modular.spi.items.GeneratedClassPlacementBuildItem;
import io.quarkus.modular.spi.model.AppModuleModel;
import io.quarkus.modular.spi.model.DependencyInfo;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.ModuleAttribute;
import io.smallrye.classfile.extras.constant.ModuleDesc;
import io.smallrye.common.resource.Resource;
import io.smallrye.modules.desc.Dependency.Modifier;
import io.smallrye.modules.desc.PackageAccess;

class ClassModuleResolverTest {
    @TempDir
    Path directory;

    @Test
    void resolvesThousandEntityBridgesAcrossTwoModelArtifacts() throws IOException {
        var application = new ApplicationModelBuilder().setAppArtifact(artifact("application", "app.Main"))
                .addDependency(artifact("runtime", "runtime.Anchor"));
        List<GeneratedClassBuildItem> generated = new ArrayList<>();
        List<GeneratedClassPlacementBuildItem> placements = new ArrayList<>();
        List<ClassAccessBuildItem> accesses = new ArrayList<>();
        generated.add(generated("runtime.Reader"));
        placements.add(GeneratedClassPlacementBuildItem.inModuleOf("runtime.Reader", "runtime.Anchor"));
        for (int artifact = 0; artifact < 2; artifact++) {
            String pkg = "model" + artifact;
            String[] entities = new String[500];
            for (int entity = 0; entity < entities.length; entity++) {
                String name = entities[entity] = pkg + ".Entity" + entity;
                String bridge = name + "$$Accessor";
                generated.add(generated(bridge));
                placements.add(GeneratedClassPlacementBuildItem.inModuleOf(bridge, name));
                accesses.add(ClassAccessBuildItem.direct("runtime.Reader", bridge));
            }
            application.addDependency(artifact(pkg, entities));
        }
        var model = new ModularitySteps().buildModularityModel(new CurateOutcomeBuildItem(application.build()),
                new MainClassBuildItem("app.Main"), generated, placements, accesses,
                List.of(), List.of(), new TransformedClassesBuildItem(Map.of()),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                new JarTreeShakeBuildItem(false, Set.of(), Map.of(), Set.of())).model();
        ModuleInfo runtime = model.modulesByName().get("runtime");
        for (int artifact = 0; artifact < 2; artifact++) {
            String pkg = "model" + artifact;
            assertThat(paths(model.modulesByName().get(pkg))).hasSize(500)
                    .allMatch(path -> path.startsWith(pkg + "/Entity") && path.endsWith("$$Accessor.class"));
            assertThat(dependency(runtime, pkg).packageAccesses())
                    .containsExactlyEntriesOf(Map.of(pkg, PackageAccess.EXPORTED));
        }
        assertThat(paths(runtime)).containsExactly("runtime/Reader.class");
    }

    @Test
    void rejectsMovingGeneratedReplacementToAnotherModule() {
        assertThatThrownBy(() -> model(List.of(generated("host.Model")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("host.Model", "runtime.Anchor")),
                List.of(), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("host.Model cannot replace")
                .hasMessageContaining("hosts").hasMessageContaining("runtime");
    }

    @Test
    void placementControlsResourcesPackagesAndServices() throws IOException {
        var model = model(List.of(generated("host.Bridge"), generated("newpackage.Factory"), generated("app.Bootstrap")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("host.Bridge", "host.Model"),
                        GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "runtime.Anchor"),
                        GeneratedClassPlacementBuildItem.inApplication("app.Bootstrap")),
                List.of(ClassAccessBuildItem.direct("newpackage.Factory", "host.Bridge")), List.of(),
                List.of(new GeneratedServiceProviderBuildItem("api.Service", "newpackage.Factory")), null);
        assertThat(paths(model.modulesByName().get("hosts"))).containsExactly("host/Bridge.class");
        ModuleInfo runtime = model.modulesByName().get("runtime");
        assertThat(paths(runtime)).containsExactly("newpackage/Factory.class");
        assertThat(runtime.packages()).containsKey("newpackage");
        assertThat(runtime.provides()).containsEntry("api.Service", List.of("newpackage.Factory"));
        assertThat(paths(model.appModuleInfo())).containsExactly("app/Bootstrap.class");
        DependencyInfo access = dependency(runtime, "hosts");
        assertThat(access.modifiers()).isEqualTo(Modifier.Set.of(Modifier.READ, Modifier.LINKED, Modifier.SYNTHETIC));
        assertThat(access.packageAccesses()).containsExactlyEntriesOf(Map.of("host", PackageAccess.EXPORTED));
    }

    @Test
    void reflectiveAccessFromApplicationAndDependencyHasNoLinkage() throws IOException {
        var model = model(List.of(), List.of(),
                List.of(ClassAccessBuildItem.reflective("app.Main", "host.Model"),
                        ClassAccessBuildItem.reflective("runtime.Anchor", "host.Model")),
                List.of(), List.of(), null);
        for (ModuleInfo caller : List.of(model.appModuleInfo(), model.modulesByName().get("runtime"))) {
            DependencyInfo access = dependency(caller, "hosts");
            assertThat(access.modifiers().contains(Modifier.READ)).isTrue();
            assertThat(access.modifiers().contains(Modifier.LINKED)).isFalse();
            assertThat(access.packageAccesses()).containsEntry("host", PackageAccess.OPEN);
        }
    }

    @Test
    void namedOpensAreAlsoAppliedToApplication() throws IOException {
        var model = model(List.of(), List.of(), List.of(),
                List.of(new ModuleOpenBuildItem("hosts", "application", "host")), List.of(), null);
        assertThat(dependency(model.appModuleInfo(), "hosts").packageAccesses()).containsEntry("host", PackageAccess.OPEN);
    }

    @Test
    void generatedAnchorsAndDuplicateDeclarationsAreResolved() throws IOException {
        var placement = GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "runtime.Anchor");
        var model = model(List.of(generated("newpackage.Factory"), generated("other.Reader")),
                List.of(placement, placement,
                        GeneratedClassPlacementBuildItem.inModuleOf("other.Reader", "newpackage.Factory")),
                List.of(ClassAccessBuildItem.direct("other.Reader", "newpackage.Factory")), List.of(), List.of(), null);
        ModuleInfo runtime = model.modulesByName().get("runtime");
        assertThat(paths(runtime)).containsExactlyInAnyOrder("newpackage/Factory.class", "other/Reader.class");
        assertThat(runtime.dependencies()).extracting(DependencyInfo::moduleName).doesNotContain("runtime");
    }

    @Test
    void undeclaredPlacementRetainsPackageOwnershipAndApplicationDefault() throws IOException {
        var model = model(List.of(generated("host.Bridge"), generated("unclaimed.NewClass")), List.of(),
                List.of(ClassAccessBuildItem.direct("unclaimed.NewClass", "host.Bridge")), List.of(), List.of(), null);
        assertThat(paths(model.modulesByName().get("hosts"))).containsExactly("host/Bridge.class");
        assertThat(paths(model.appModuleInfo())).containsExactly("unclaimed/NewClass.class");
        assertThat(dependency(model.appModuleInfo(), "hosts").packageAccesses()).containsEntry("host", PackageAccess.EXPORTED);
    }

    @Test
    void reachableGeneratedClassKeepsItsDestinationModuleAlive() throws IOException {
        var model = model(List.of(generated("newpackage.Factory")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "runtime.Anchor")),
                List.of(ClassAccessBuildItem.direct("app.Main", "newpackage.Factory")), List.of(), List.of(),
                Set.of("app.Main", "newpackage.Factory"));
        assertThat(model.modulesByName()).containsKey("runtime").doesNotContainKey("hosts");
        assertThat(model.modulesByName().get("runtime").packages()).containsKey("newpackage");
        assertThat(paths(model.modulesByName().get("runtime"))).containsExactly("newpackage/Factory.class");
    }

    @Test
    void legacyGeneratedClassesFollowExplicitNewPackageOwnership() throws IOException {
        var model = model(List.of(generated("newpackage.Factory"), generated("newpackage.Reader"), generated("other.Helper")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "runtime.Anchor"),
                        GeneratedClassPlacementBuildItem.inModuleOf("other.Helper", "newpackage.Reader")),
                List.of(ClassAccessBuildItem.direct("app.Main", "newpackage.Reader")), List.of(), List.of(), null);
        assertThat(paths(model.modulesByName().get("runtime")))
                .containsExactlyInAnyOrder("newpackage/Factory.class", "newpackage/Reader.class", "other/Helper.class");
        assertThat(dependency(model.appModuleInfo(), "runtime").packageAccesses())
                .containsEntry("newpackage", PackageAccess.EXPORTED);
    }

    @Test
    void repeatedAccessesMergeWithOpenTakingPrecedence() throws IOException {
        var model = model(List.of(), List.of(),
                List.of(ClassAccessBuildItem.direct("runtime.Anchor", "host.Model"),
                        ClassAccessBuildItem.direct("runtime.Anchor", "host.Model"),
                        ClassAccessBuildItem.reflective("runtime.Anchor", "host.Model")),
                List.of(), List.of(), null);
        DependencyInfo access = dependency(model.modulesByName().get("runtime"), "hosts");
        assertThat(access.packageAccesses()).containsExactlyEntriesOf(Map.of("host", PackageAccess.OPEN));
        assertThat(access.modifiers().contains(Modifier.LINKED)).isTrue();
    }

    @Test
    void reachableGeneratedAccessTargetKeepsInferredOwnerAlive() throws IOException {
        var model = model(List.of(generated("runtime.Factory")), List.of(),
                List.of(ClassAccessBuildItem.direct("app.Main", "runtime.Factory")), List.of(), List.of(),
                Set.of("app.Main", "runtime.Factory"));
        assertThat(model.modulesByName()).containsKey("runtime");
        assertThat(paths(model.modulesByName().get("runtime"))).containsExactly("runtime/Factory.class");
        assertThat(dependency(model.appModuleInfo(), "runtime").packageAccesses())
                .containsEntry("runtime", PackageAccess.EXPORTED);
    }

    @Test
    void resolvesJdkClassAccessWithoutLoadingClasses() throws IOException {
        var model = model(List.of(), List.of(),
                List.of(ClassAccessBuildItem.direct("runtime.Anchor", "java.lang.Object"),
                        ClassAccessBuildItem.reflective("app.Main", "java.lang.String")),
                List.of(), List.of(), null);
        DependencyInfo direct = dependency(model.modulesByName().get("runtime"), "java.base");
        assertThat(direct.modifiers().contains(Modifier.LINKED)).isTrue();
        assertThat(direct.packageAccesses()).containsEntry("java.lang", PackageAccess.EXPORTED);
        assertThat(dependency(model.appModuleInfo(), "java.base").packageAccesses())
                .containsEntry("java.lang", PackageAccess.OPEN);
    }

    @Test
    void rejectsJdkCallerWhoseModuleCannotBeModified() {
        assertThatThrownBy(() -> model(List.of(), List.of(),
                List.of(ClassAccessBuildItem.direct("java.lang.Object", "app.Main")),
                List.of(), List.of(), null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot declare class access from JDK caller java.lang.Object");
    }

    @Test
    void rejectsUnknownJdkClassAndPlacementIntoJdk() {
        assertThatThrownBy(() -> model(List.of(), List.of(),
                List.of(ClassAccessBuildItem.direct("app.Main", "java.lang.NoSuchClassForModuleTest")),
                List.of(), List.of(), null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot resolve runtime module for class java.lang.NoSuchClassForModuleTest");
        assertThatThrownBy(() -> model(List.of(generated("newpackage.Factory")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "java.lang.Object")),
                List.of(), List.of(), List.of(), null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot place generated class newpackage.Factory in JDK module java.base");
    }

    @Test
    void rejectsConflictingPlacements() {
        assertThatThrownBy(() -> model(List.of(generated("newpackage.Factory")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "runtime.Anchor"),
                        GeneratedClassPlacementBuildItem.inApplication("newpackage.Factory")),
                List.of(), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Conflicting module placements");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rejectsDefaultPackageInPlacementDeclarations(boolean defaultAnchor) {
        var placement = defaultAnchor ? GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "DefaultClass")
                : GeneratedClassPlacementBuildItem.inApplication("DefaultClass");
        assertThatThrownBy(() -> model(List.of(generated("DefaultClass"), generated("newpackage.Factory")),
                List.of(placement), List.of(), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("default package")
                .hasMessageContaining("DefaultClass").hasMessageContaining("placement");
    }

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void rejectsDefaultPackageInAccessDeclarations(boolean defaultCaller, boolean reflective) {
        String caller = defaultCaller ? "DefaultClass" : "app.Main";
        String target = defaultCaller ? "app.Main" : "DefaultClass";
        var access = reflective ? ClassAccessBuildItem.reflective(caller, target) : ClassAccessBuildItem.direct(caller, target);
        assertThatThrownBy(() -> model(List.of(generated("DefaultClass")), List.of(),
                List.of(access), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("default package")
                .hasMessageContaining("DefaultClass").hasMessageContaining(access.kind().toString())
                .hasMessageContaining(caller).hasMessageContaining(target);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void unrelatedDefaultPackageGeneratedClassRetainsLegacyHandling(boolean accessDeclaration) throws IOException {
        var model = model(List.of(generated("DefaultClass")), List.of(),
                accessDeclaration ? List.of(ClassAccessBuildItem.direct("app.Main", "host.Model")) : List.of(),
                List.of(), List.of(), null);
        assertThat(paths(model.appModuleInfo())).containsExactly("DefaultClass.class");
    }

    @Test
    void rejectsUnknownAnchorAndUngeneratedPlacement() {
        assertThatThrownBy(() -> model(List.of(generated("newpackage.Factory")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "missing.Anchor")),
                List.of(), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing.Anchor");
        assertThatThrownBy(() -> model(List.of(), List.of(GeneratedClassPlacementBuildItem.inApplication("missing.Generated")),
                List.of(), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("was not generated");
    }

    @Test
    void rejectsPlacementCycles() {
        assertThatThrownBy(() -> model(List.of(generated("a.First"), generated("b.Second")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("a.First", "b.Second"),
                        GeneratedClassPlacementBuildItem.inModuleOf("b.Second", "a.First")),
                List.of(), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cyclic generated class placement");
    }

    @Test
    void rejectsPlacementIntroducingSplitPackage() {
        assertThatThrownBy(() -> model(List.of(generated("host.Bridge")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("host.Bridge", "runtime.Anchor")),
                List.of(), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("would split package host");
    }

    @Test
    void anchoredBridgesPreserveExistingSplitPackageAndLegacySiblingRouting() throws IOException {
        var legacy = splitPackageModel(List.of(), List.of());
        var placed = splitPackageModel(List.of(
                GeneratedClassPlacementBuildItem.inModuleOf("shared.FirstBridge", "shared.First"),
                GeneratedClassPlacementBuildItem.inModuleOf("shared.SecondBridge", "shared.Second")), List.of());
        assertThat(paths(placed.modulesByName().get("first"))).contains("shared/FirstBridge.class")
                .doesNotContain("shared/SecondBridge.class");
        assertThat(paths(placed.modulesByName().get("second"))).contains("shared/SecondBridge.class")
                .doesNotContain("shared/FirstBridge.class");
        for (String owner : List.of("first", "second", "application")) {
            assertThat(paths(placed.modulesByName().get(owner)).contains("shared/Sibling.class"))
                    .as("legacy sibling routing in %s", owner)
                    .isEqualTo(paths(legacy.modulesByName().get(owner)).contains("shared/Sibling.class"));
        }
    }

    @Test
    void rejectsAddingAnotherOwnerToExistingSplitPackage() {
        assertThatThrownBy(() -> splitPackageModel(
                List.of(GeneratedClassPlacementBuildItem.inApplication("shared.FirstBridge")), List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("would split package shared");
    }

    @Test
    void accessToUnplacedGeneratedClassInSplitPackageRemainsAmbiguous() {
        assertThatThrownBy(() -> splitPackageModel(
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("shared.FirstBridge", "shared.First")),
                List.of(ClassAccessBuildItem.direct("app.Main", "shared.Sibling"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ambiguous runtime module for class shared.Sibling");
    }

    @Test
    void rejectsSplittingNewGeneratedPackageBetweenPlacements() {
        assertThatThrownBy(() -> model(List.of(generated("newpackage.First"), generated("newpackage.Second")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("newpackage.First", "runtime.Anchor"),
                        GeneratedClassPlacementBuildItem.inApplication("newpackage.Second")),
                List.of(), List.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("would split package newpackage");
    }

    @Test
    void rejectsAmbiguousOriginalClassOwnership() throws IOException {
        var app = artifact("application", "app.Main").build();
        var first = artifact("first", "shared.Model").build();
        var second = artifact("second", "shared.Model").build();
        assertThatThrownBy(() -> new ClassModuleResolver(app, List.of(first, second), new ModuleContentIndex(Map.of()),
                List.of(), List.of(),
                List.of(ClassAccessBuildItem.direct("app.Main", "shared.Model"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Ambiguous runtime module");
    }

    @Test
    void placementPreservesTransformedBytesAndRemovals() throws IOException {
        byte[] transformed = { 1, 2, 3 };
        var model = model(List.of(generated("newpackage.Factory"), generated("removed.Helper")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "runtime.Anchor"),
                        GeneratedClassPlacementBuildItem.inModuleOf("removed.Helper", "runtime.Anchor")),
                List.of(), List.of(), List.of(), null,
                new TransformedClassesBuildItem(Map.of(directory, Set.of(
                        new TransformedClassesBuildItem.TransformedClass("newpackage.Factory", transformed,
                                "newpackage/Factory.class"),
                        new TransformedClassesBuildItem.TransformedClass("removed.Helper", null, "removed/Helper.class")))));
        ModuleInfo runtime = model.modulesByName().get("runtime");
        assertThat(paths(runtime)).containsExactly("newpackage/Factory.class");
        assertThat(runtime.packages()).containsKey("newpackage").doesNotContainKey("removed");
        try (var stream = runtime.generated().getFirst().openStream()) {
            assertThat(stream.readAllBytes()).containsExactly(transformed);
        }
    }

    @Test
    void deploymentAndRemovedArtifactsDoNotParticipateInOwnership() throws IOException {
        var deployment = artifact("deployment", "runtime.Anchor").setDeploymentCp();
        deployment.clearFlag(DependencyFlags.RUNTIME_CP);
        var removed = artifact("removed", "runtime.Anchor").setFlags(DependencyFlags.MISSING_FROM_APPLICATION);
        var application = new ApplicationModelBuilder().setAppArtifact(artifact("application", "app.Main"))
                .addDependency(artifact("runtime", "runtime.Anchor"))
                .addDependency(deployment).addDependency(removed).build();
        var model = new ModularitySteps().buildModularityModel(new CurateOutcomeBuildItem(application),
                new MainClassBuildItem("app.Main"), List.of(generated("newpackage.Factory")),
                List.of(GeneratedClassPlacementBuildItem.inModuleOf("newpackage.Factory", "runtime.Anchor")),
                List.of(), List.of(), List.of(), new TransformedClassesBuildItem(Map.of()),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                new JarTreeShakeBuildItem(false, Set.of(), Map.of(), Set.of())).model();
        assertThat(model.modulesByName()).doesNotContainKeys("deployment", "removed");
        assertThat(paths(model.modulesByName().get("runtime"))).containsExactly("newpackage/Factory.class");
    }

    private AppModuleModel splitPackageModel(List<GeneratedClassPlacementBuildItem> placements,
            List<ClassAccessBuildItem> accesses) throws IOException {
        var application = new ApplicationModelBuilder().setAppArtifact(artifact("application", "app.Main"))
                .addDependency(artifact("first", "shared.First"))
                .addDependency(artifact("second", "shared.Second")).build();
        return new ModularitySteps().buildModularityModel(new CurateOutcomeBuildItem(application),
                new MainClassBuildItem("app.Main"),
                List.of(generated("shared.FirstBridge"), generated("shared.SecondBridge"), generated("shared.Sibling")),
                placements, accesses, List.of(), List.of(), new TransformedClassesBuildItem(Map.of()),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                new JarTreeShakeBuildItem(false, Set.of(), Map.of(), Set.of())).model();
    }

    private AppModuleModel model(List<GeneratedClassBuildItem> generated,
            List<GeneratedClassPlacementBuildItem> placements, List<ClassAccessBuildItem> accesses,
            List<ModuleOpenBuildItem> opens, List<GeneratedServiceProviderBuildItem> services, Set<String> reachable)
            throws IOException {
        return model(generated, placements, accesses, opens, services, reachable, new TransformedClassesBuildItem(Map.of()));
    }

    private AppModuleModel model(List<GeneratedClassBuildItem> generated,
            List<GeneratedClassPlacementBuildItem> placements, List<ClassAccessBuildItem> accesses,
            List<ModuleOpenBuildItem> opens, List<GeneratedServiceProviderBuildItem> services, Set<String> reachable,
            TransformedClassesBuildItem transformed) throws IOException {
        var model = new ApplicationModelBuilder()
                .setAppArtifact(artifact("application", "app.Main"))
                .addDependency(artifact("runtime", "runtime.Anchor"))
                .addDependency(artifact("hosts", "host.Model")).build();
        return new ModularitySteps().buildModularityModel(new CurateOutcomeBuildItem(model), new MainClassBuildItem("app.Main"),
                generated, placements, accesses, List.of(), services, transformed,
                opens, List.of(), List.of(), List.of(), List.of(),
                new JarTreeShakeBuildItem(reachable != null, reachable == null ? Set.of() : reachable, Map.of(), Set.of()))
                .model();
    }

    private ResolvedDependencyBuilder artifact(String module, String... classes) throws IOException {
        Path root = Files.createTempDirectory(directory, module);
        Files.write(root.resolve("module-info.class"),
                ClassFile.of().buildModule(ModuleAttribute.of(ModuleDesc.of(module), builder -> {
                })));
        for (String name : classes) {
            Path file = root.resolve(name.replace('.', '/') + ".class");
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[0]);
        }
        return ResolvedDependencyBuilder.newInstance().setGroupId("test").setArtifactId(module).setVersion("1")
                .setResolvedPath(root).setRuntimeCp().setDeploymentCp();
    }

    private static GeneratedClassBuildItem generated(String name) {
        return new GeneratedClassBuildItem(true, name, new byte[0]);
    }

    private static List<String> paths(ModuleInfo module) {
        return module.generated().stream().map(Resource::pathName).toList();
    }

    private static DependencyInfo dependency(ModuleInfo module, String target) {
        return module.dependencies().stream().filter(dependency -> dependency.moduleName().equals(target)).findFirst()
                .orElseThrow();
    }
}
