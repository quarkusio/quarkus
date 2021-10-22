package io.quarkus.gradle.tooling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.util.Files;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.junit.jupiter.api.Test;

public class GradleApplicationModelBuilderTest {

    @Test
    void testToAppDependency() {
        ResolvedArtifact artifact = mock(ResolvedArtifact.class);
        ResolvedModuleVersion version = mock(ResolvedModuleVersion.class);
        when(version.toString()).thenReturn(":commons-lang3-3.9:");
        when(artifact.getModuleVersion()).thenReturn(version);
        when(artifact.getFile()).thenReturn(Files.currentFolder());
        assertThatCode(() -> GradleApplicationModelBuilder.toDependency(artifact)).doesNotThrowAnyException();
    }
}
