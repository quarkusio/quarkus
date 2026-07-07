package io.quarkus.gradle.application.internal.modelgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Set;

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedVariantResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.AttributeContainer;
import org.junit.jupiter.api.Test;

class ResolvedClasspathTest {

    @Test
    void artifactMetadataIncludesExactTypeComponentAndFileAssociation() {
        ModuleComponentIdentifier component = mock(ModuleComponentIdentifier.class);
        when(component.getGroup()).thenReturn("org.acme");
        when(component.getModule()).thenReturn("dependency");
        when(component.getVersion()).thenReturn("1.0");
        ComponentArtifactIdentifier artifactId = mock(ComponentArtifactIdentifier.class);
        when(artifactId.getComponentIdentifier()).thenReturn(component);
        AttributeContainer attributes = mock(AttributeContainer.class);
        when(attributes.getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE)).thenReturn("custom-type");
        ResolvedVariantResult variant = mock(ResolvedVariantResult.class);
        when(variant.getAttributes()).thenReturn(attributes);
        ResolvedArtifactResult artifact = mock(ResolvedArtifactResult.class);
        when(artifact.getId()).thenReturn(artifactId);
        when(artifact.getFile()).thenReturn(new File("build/dependency.bin").getAbsoluteFile());
        when(artifact.getVariant()).thenReturn(variant);

        String initial = ResolvedClasspath.artifactMetadata(Set.of(artifact)).get(0);
        assertThat(initial).hasSize(64);

        when(attributes.getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE)).thenReturn("other-type");
        assertThat(ResolvedClasspath.artifactMetadata(Set.of(artifact))).doesNotContain(initial);

        when(attributes.getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE)).thenReturn("custom-type");
        when(artifact.getFile()).thenReturn(new File("build/dependency-classifier.bin").getAbsoluteFile());
        assertThat(ResolvedClasspath.artifactMetadata(Set.of(artifact))).doesNotContain(initial);

        when(artifact.getFile()).thenReturn(new File("build/dependency.bin").getAbsoluteFile());
        when(component.getVersion()).thenReturn("2.0");
        assertThat(ResolvedClasspath.artifactMetadata(Set.of(artifact))).doesNotContain(initial);
    }
}
