package io.quarkus.gradle.tooling;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.MappableCollectionFactory;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.LazySourceDir;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;

public class DefaultProjectDescriptorTest {

    private static final Path PROJECT_DIR = Path.of("project").toAbsolutePath();

    @Test
    public void copyHasTheSameContent() {
        WorkspaceModule.Mutable module = fullyPopulatedModule();

        WorkspaceModule.Mutable copy = new DefaultProjectDescriptor(module).copy().getWorkspaceModule();

        // asMap() is what the serialized application model is written from, so it covers every field that matters
        assertThat(copy.asMap(MappableCollectionFactory.defaultInstance()))
                .isEqualTo(module.asMap(MappableCollectionFactory.defaultInstance()));
    }

    @Test
    public void dependencyAddedToTheCopyDoesNotReachTheOriginal() {
        WorkspaceModule.Mutable module = fullyPopulatedModule();
        DefaultProjectDescriptor descriptor = new DefaultProjectDescriptor(module);

        DefaultProjectDescriptor copy = descriptor.copy();
        copy.getWorkspaceModule().addDependency(new ArtifactDependency(ArtifactCoords.jar("org.acme", "added", "1.0")));

        assertThat(copy.getWorkspaceModule()).isNotSameAs(module);
        assertThat(copy.getWorkspaceModule().getDirectDependencies()).hasSize(2);
        assertThat(module.getDirectDependencies()).hasSize(1);
    }

    @Test
    public void copiesOfTheSameDescriptorDoNotAccumulateDependencies() {
        // What the application model tasks of one project do, one after the other, without the configuration cache
        DefaultProjectDescriptor descriptor = new DefaultProjectDescriptor(minimalModule());
        ArtifactDependency dependency = new ArtifactDependency(ArtifactCoords.jar("org.acme", "lib", "1.0"));

        List<Integer> recorded = new ArrayList<>();
        for (int task = 0; task < 3; task++) {
            WorkspaceModule.Mutable module = descriptor.copy().getWorkspaceModule();
            module.addDependency(dependency);
            recorded.add(module.getDirectDependencies().size());
        }

        assertThat(recorded).containsExactly(1, 1, 1);
        assertThat(descriptor.getWorkspaceModule().getDirectDependencies()).isEmpty();
    }

    @Test
    public void copyOfModuleWithoutDirectoriesOrDependencies() {
        WorkspaceModule.Mutable module = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "app", "1.0"));

        WorkspaceModule.Mutable copy = new DefaultProjectDescriptor(module).copy().getWorkspaceModule();

        assertThat(copy.getId()).isEqualTo(module.getId());
        assertThat(copy.getModuleDir()).isNull();
        assertThat(copy.getBuildDir()).isNull();
        assertThat(copy.getBuildFiles().isEmpty()).isTrue();
        assertThat(copy.getDirectDependencies()).isEmpty();
    }

    private static WorkspaceModule.Mutable minimalModule() {
        return WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "app", "1.0"))
                .setModuleDir(PROJECT_DIR)
                .setBuildDir(PROJECT_DIR.resolve("build"));
    }

    private static WorkspaceModule.Mutable fullyPopulatedModule() {
        WorkspaceModule parent = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "parent", "1.0"))
                .build();
        List<SourceDir> sources = List.of(new LazySourceDir(PROJECT_DIR.resolve("src/main/java"),
                PROJECT_DIR.resolve("build/classes/java/main")));
        List<SourceDir> resources = List.of(new LazySourceDir(PROJECT_DIR.resolve("src/main/resources"),
                PROJECT_DIR.resolve("build/resources/main")));
        return minimalModule()
                .setBuildFile(PROJECT_DIR.resolve("build.gradle.kts"))
                .addArtifactSources(new DefaultArtifactSources(ArtifactSources.MAIN, sources, resources))
                .addDependencyConstraint(new ArtifactDependency(ArtifactCoords.pom("org.acme", "bom", "1.0")))
                .addDependency(new ArtifactDependency(ArtifactCoords.jar("org.acme", "lib", "1.0")))
                .setTestClasspathDependencyExclusions(new ArrayList<>(List.of("org.acme:excluded")))
                .setAdditionalTestClasspathElements(new ArrayList<>(List.of("extra.jar")))
                .setParent(parent);
    }
}
