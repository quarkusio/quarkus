package io.quarkus.modular.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.modular.spi.model.DependencyInfo;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.smallrye.modules.desc.Dependency;
import io.smallrye.modules.desc.ModuleDescriptor.Modifier;
import io.smallrye.modules.desc.PackageAccess;
import io.smallrye.modules.desc.PackageInfo;

class ModuleWriterTest {
    @Test
    void openModuleDescriptorIsValid() {
        ModuleDescriptor descriptor = descriptor(Modifier.Set.of(Modifier.OPEN), Map.of("example.model", PackageInfo.OPEN));
        assertThat(descriptor.isOpen()).isTrue();
        assertThat(descriptor.opens()).isEmpty();
        assertThat(descriptor.packages()).containsExactly("example.model");
    }

    @Test
    void qualifiedPackageAccessSurvivesDescriptorWriting() {
        ModuleDescriptor descriptor = descriptor(Modifier.Set.of(), Map.of(
                "example.closed", PackageInfo.PRIVATE,
                "example.exported", PackageInfo.of(PackageAccess.PRIVATE, Set.of("export.friend"), Set.of()),
                "example.opened", PackageInfo.of(PackageAccess.PRIVATE, Set.of(), Set.of("open.friend")),
                "example.both", PackageInfo.of(PackageAccess.EXPORTED, Set.of(), Set.of("open.friend"))));
        assertThat(descriptor.exports()).hasSize(2).anySatisfy(export -> {
            assertThat(export.source()).isEqualTo("example.exported");
            assertThat(export.targets()).containsExactly("export.friend");
        }).anySatisfy(export -> {
            assertThat(export.source()).isEqualTo("example.both");
            assertThat(export.isQualified()).isFalse();
        });
        assertThat(descriptor.opens()).hasSize(2).allSatisfy(open -> {
            assertThat(open.source()).isIn("example.opened", "example.both");
            assertThat(open.targets()).containsExactly("open.friend");
        });
    }

    private static ModuleDescriptor descriptor(Modifier.Set modifiers, Map<String, PackageInfo> packages) {
        var artifact = ResolvedDependencyBuilder.newInstance().setGroupId("test").setArtifactId("module")
                .setVersion("1").setResolvedPath(Path.of("target/test-module")).build();
        var model = new ModuleInfo("test.module", "1", modifiers, artifact, null, packages,
                List.of(new DependencyInfo("java.base",
                        Dependency.Modifier.Set.of(Dependency.Modifier.READ, Dependency.Modifier.LINKED,
                                Dependency.Modifier.MANDATED),
                        Map.of())),
                List.of(), Set.of(), Map.of(), List.of());
        return ModuleDescriptor.read(ByteBuffer.wrap(ModuleWriter.getModuleInfoClass(model)));
    }
}
