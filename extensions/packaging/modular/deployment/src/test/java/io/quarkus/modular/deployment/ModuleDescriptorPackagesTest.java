package io.quarkus.modular.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.ModuleAttribute;
import io.smallrye.classfile.attribute.ModulePackagesAttribute;
import io.smallrye.classfile.extras.constant.ModuleDesc;
import io.smallrye.classfile.extras.constant.PackageDesc;
import io.smallrye.classfile.extras.reflect.AccessFlag;
import io.smallrye.modules.desc.ModuleDescriptor;
import io.smallrye.modules.desc.PackageAccess;
import io.smallrye.modules.desc.PackageInfo;

class ModuleDescriptorPackagesTest {
    private static final List<String> PACKAGES = List.of("example.closed", "example.exported", "example.opened",
            "example.qualifiedexport", "example.qualifiedopen");

    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(strings = { "module-info.class", "META-INF/versions/9/module-info.class" })
    void missingModuleAttributeIdentifiesArtifactAndDescriptor(String descriptorPath) throws IOException {
        Path dependencyRoot = Files.createDirectories(directory.resolve("dependency"));
        Path descriptor = dependencyRoot.resolve(descriptorPath);
        Files.createDirectories(descriptor.getParent());
        try (var bytes = ModuleDescriptorPackagesTest.class.getResourceAsStream("ModuleDescriptorPackagesTest.class")) {
            Files.copy(bytes, descriptor);
        }
        var application = new ApplicationModelBuilder()
                .setAppArtifact(artifact("application", Files.createDirectories(directory.resolve("application"))))
                .addDependency(artifact("dependency", dependencyRoot)).build();
        assertThatThrownBy(() -> new ModularitySteps().buildModularityModel(new CurateOutcomeBuildItem(application),
                new MainClassBuildItem("app.Main"), List.of(), List.of(), List.of(), List.of(), List.of(),
                new TransformedClassesBuildItem(Map.of()), List.of(), List.of(), List.of(), List.of(), List.of(),
                new JarTreeShakeBuildItem(false, Set.of(), Map.of(), Set.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test:dependency:").hasMessageContaining(descriptorPath)
                .hasMessageContaining("missing Module attribute");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void explicitDescriptorControlsPackageAccess(boolean packageAttribute) throws IOException {
        ModuleInfo module = model(packageAttribute, false);
        assertThat(module.packages()).containsEntry("example.closed", PackageInfo.PRIVATE)
                .containsEntry("example.exported", PackageInfo.EXPORTED)
                .containsEntry("example.opened", PackageInfo.OPEN)
                .containsEntry("example.qualifiedexport", PackageInfo.of(PackageAccess.PRIVATE, Set.of("friend"), Set.of()))
                .containsEntry("example.qualifiedopen", PackageInfo.of(PackageAccess.PRIVATE, Set.of(), Set.of("friend")));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void openModuleRetainsItsAccess(boolean packageAttribute) throws IOException {
        ModuleInfo module = model(packageAttribute, true);
        assertThat(module.modifiers().contains(ModuleDescriptor.Modifier.OPEN)).isTrue();
        assertThat(module.packages()).hasSize(PACKAGES.size());
        assertThat(module.packages().values()).containsOnly(PackageInfo.OPEN);
    }

    private ModuleInfo model(boolean packageAttribute, boolean open) throws IOException {
        Path dependencyRoot = Files.createDirectories(directory.resolve("dependency"));
        for (String pkg : PACKAGES) {
            Path file = dependencyRoot.resolve(pkg.replace('.', '/') + "/Model.class");
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[0]);
        }
        byte[] descriptor = ClassFile.of().buildModule(ModuleAttribute.of(ModuleDesc.of("dependency"), builder -> {
            if (open) {
                builder.moduleFlags(AccessFlag.OPEN.mask());
            } else {
                builder.exports(PackageDesc.of("example.exported"), Set.of());
                builder.opens(PackageDesc.of("example.opened"), Set.of());
                builder.exports(PackageDesc.of("example.qualifiedexport"), Set.of(), ModuleDesc.of("friend"));
                builder.opens(PackageDesc.of("example.qualifiedopen"), Set.of(), ModuleDesc.of("friend"));
            }
        }), builder -> {
            if (packageAttribute) {
                builder.with(ModulePackagesAttribute.ofNames(PACKAGES.stream().map(PackageDesc::of).toList()));
            }
        });
        Files.write(dependencyRoot.resolve("module-info.class"), descriptor);
        var application = new ApplicationModelBuilder()
                .setAppArtifact(artifact("application", Files.createDirectories(directory.resolve("application"))))
                .addDependency(artifact("dependency", dependencyRoot)).build();
        return new ModularitySteps().buildModularityModel(new CurateOutcomeBuildItem(application),
                new MainClassBuildItem("app.Main"), List.of(), List.of(), List.of(), List.of(), List.of(),
                new TransformedClassesBuildItem(Map.of()), List.of(), List.of(), List.of(), List.of(), List.of(),
                new JarTreeShakeBuildItem(false, Set.of(), Map.of(), Set.of())).model().modulesByName().get("dependency");
    }

    private static ResolvedDependencyBuilder artifact(String name, Path root) {
        return ResolvedDependencyBuilder.newInstance().setGroupId("test").setArtifactId(name).setVersion("1")
                .setResolvedPath(root).setRuntimeCp().setDeploymentCp();
    }
}
