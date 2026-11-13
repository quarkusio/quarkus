package io.quarkus.gradle.model.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuarkusPlatformInfoTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUseArtifactExtensionAsPlatformArtifactType() {
        assertThat(QuarkusPlatformInfo.PLATFORM_DESCRIPTOR_ARTIFACT_TYPE).isEqualTo("json");
        assertThat(QuarkusPlatformInfo.PLATFORM_PROPERTIES_ARTIFACT_TYPE).isEqualTo("properties");
    }

    @Test
    void shouldResolveDescriptorAndPropertiesArtifacts() throws IOException {
        QuarkusPlatformInfo platformInfo = ProjectBuilder.builder().build().getObjects()
                .newInstance(QuarkusPlatformInfo.class);
        platformInfo.getResolvedArtifacts().set(Set.of(
                artifact("io.quarkus.platform", "acme-quarkus-platform-descriptor", "3.0.0",
                        writeFile("descriptor.json", "")),
                artifact("io.quarkus.platform", "acme-quarkus-platform-properties", "3.0.0",
                        writeFile("properties.properties", "platform.quarkus.sample=value\n"))));

        var platformImports = platformInfo.resolvePlatformImports();

        assertThat(platformImports.getImportedPlatformBoms())
                .extracting(Object::toString)
                .containsExactly("io.quarkus.platform:acme::pom:3.0.0");
        assertThat(platformImports.getPlatformProperties())
                .containsEntry("platform.quarkus.sample", "value");
    }

    private Path writeFile(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private static ResolvedArtifactResult artifact(String group, String moduleName, String version, Path file) {
        ModuleIdentifier moduleIdentifier = mock(ModuleIdentifier.class);
        when(moduleIdentifier.getName()).thenReturn(moduleName);

        ModuleComponentIdentifier componentIdentifier = mock(ModuleComponentIdentifier.class);
        when(componentIdentifier.getGroup()).thenReturn(group);
        when(componentIdentifier.getModuleIdentifier()).thenReturn(moduleIdentifier);
        when(componentIdentifier.getVersion()).thenReturn(version);

        ModuleComponentArtifactIdentifier artifactIdentifier = mock(ModuleComponentArtifactIdentifier.class);
        when(artifactIdentifier.getComponentIdentifier()).thenReturn(componentIdentifier);

        ResolvedArtifactResult artifact = mock(ResolvedArtifactResult.class);
        when(artifact.getId()).thenReturn(artifactIdentifier);
        when(artifact.getFile()).thenReturn(file.toFile());
        return artifact;
    }
}
