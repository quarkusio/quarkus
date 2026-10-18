package io.quarkus.gradle.tooling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.util.Files;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.junit.jupiter.api.Test;

public class GradleApplicationModelBuilderTest {

    @Test
    void testToAppDependency() {
        ResolvedArtifact artifact = mock(ResolvedArtifact.class);
        when(artifact.getName()).thenReturn("commons-lang");
        ModuleVersionIdentifier id = mock(ModuleVersionIdentifier.class);
        ResolvedModuleVersion version = mock(ResolvedModuleVersion.class);
        when(version.getId()).thenReturn(id);
        when(id.getGroup()).thenReturn("commons-lang");
        when(artifact.getModuleVersion()).thenReturn(version);
        when(artifact.getFile()).thenReturn(Files.currentFolder());
        assertThatCode(() -> GradleApplicationModelBuilder.toDependency(artifact)).doesNotThrowAnyException();
    }
}
