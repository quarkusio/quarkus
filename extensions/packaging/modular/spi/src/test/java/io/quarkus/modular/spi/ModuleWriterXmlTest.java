package io.quarkus.modular.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.modular.spi.model.AppModuleModel;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.smallrye.modules.ModuleFinder;
import io.smallrye.modules.ModuleLoader;
import io.smallrye.modules.desc.ModuleDescriptor;
import io.smallrye.modules.desc.PackageAccess;
import io.smallrye.modules.desc.PackageInfo;

class ModuleWriterXmlTest {
    @TempDir
    Path directory;

    @Test
    void qualifiedExportsRoundTripThroughRuntimeParser() throws IOException {
        Map<String, PackageInfo> packages = Map.of(
                "example.closed", PackageInfo.PRIVATE,
                "example.qualified", PackageInfo.of(PackageAccess.PRIVATE, Set.of("export.friend"), Set.of("open.friend")),
                "example.exported", PackageInfo.EXPORTED,
                "example.opened", PackageInfo.OPEN);
        assertThat(roundTrip(packages).packages()).containsExactlyInAnyOrderEntriesOf(packages);
    }

    @Test
    void qualifiedOpensDoNotBecomeUnqualifiedExports() throws IOException {
        Map<String, PackageInfo> packages = Map.of(
                "example.privateopen", PackageInfo.of(PackageAccess.PRIVATE, Set.of(), Set.of("open.friend")),
                "example.publicopen", PackageInfo.of(PackageAccess.EXPORTED, Set.of(), Set.of("open.friend")));
        assertThat(roundTrip(packages).packages()).containsExactlyInAnyOrderEntriesOf(packages);
    }

    @Test
    void openModuleLoadsAndOpensEveryPackage() throws IOException {
        Map<String, PackageInfo> packages = Map.of(
                "example.opened", PackageInfo.OPEN,
                "example.generated", PackageInfo.PRIVATE);
        writeModule(packages, ModuleDescriptor.Modifier.Set.of(ModuleDescriptor.Modifier.OPEN));
        try (ModuleLoader loader = new ModuleLoader("test",
                ModuleFinder.fromFileSystem(List.of(directory.resolve("output"))))) {
            Module module = loader.requireModule("test.module").module();
            assertThat(module.isNamed()).isTrue();
            assertThat(module.getPackages()).containsExactlyInAnyOrderElementsOf(packages.keySet());
            for (String pkg : packages.keySet()) {
                assertThat(module.isOpen(pkg)).as("Package %s is open", pkg).isTrue();
            }
        }
    }

    private ModuleDescriptor roundTrip(Map<String, PackageInfo> packages) throws IOException {
        Path jar = writeModule(packages, ModuleDescriptor.Modifier.Set.of());
        try (JarFile archive = new JarFile(jar.toFile());
                var descriptor = archive.getInputStream(archive.getJarEntry("module.xml"))) {
            return ModuleDescriptor.fromXml(descriptor);
        }
    }

    private Path writeModule(Map<String, PackageInfo> packages, ModuleDescriptor.Modifier.Set modifiers) throws IOException {
        Path content = Files.createDirectories(directory.resolve("content"));
        var artifact = ResolvedDependencyBuilder.newInstance().setGroupId("test").setArtifactId("module")
                .setVersion("1").setResolvedPath(content).build();
        var module = new ModuleInfo("test.module", "1", modifiers, artifact, null,
                packages, List.of(), List.of(), Set.of(), Map.of(), List.of());
        var model = AppModuleModel.builder().appModuleInfo(module).build();
        return ModuleWriter.writeModule(model, module, directory.resolve("output/test.module"), new Manifest(), false);
    }

}
