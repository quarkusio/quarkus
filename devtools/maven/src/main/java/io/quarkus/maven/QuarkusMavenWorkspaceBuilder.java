package io.quarkus.maven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultSourceDir;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.GAV;

class QuarkusMavenWorkspaceBuilder {

    static void loadModules(MavenProject project, ApplicationModelBuilder modelBuilder) {
    }

    static WorkspaceModule toProjectModule(MavenProject project) {
        final Build build = project.getBuild();

        final WorkspaceModule.Mutable moduleBuilder = WorkspaceModule.builder()
                .setModuleId(getId(project))
                .setModuleDir(project.getBasedir().toPath())
                .setBuildDir(Path.of(build.getDirectory()));

        final Path classesDir = Path.of(build.getOutputDirectory());
        final Path generatedSourcesDir = Path.of(build.getDirectory(), "generated-sources/annotations");
        final List<SourceDir> sources = new ArrayList<>(project.getCompileSourceRoots().size());
        project.getCompileSourceRoots()
                .forEach(s -> sources.add(new DefaultSourceDir(Path.of(s), classesDir, generatedSourcesDir)));
        final List<SourceDir> resources = new ArrayList<>(build.getResources().size());
        for (Resource r : build.getResources()) {
            resources.add(new DefaultSourceDir(Path.of(r.getDirectory()),
                    r.getTargetPath() == null ? classesDir : Path.of(r.getTargetPath()),
                    // FIXME: generated sources?
                    null));
        }
        moduleBuilder.addArtifactSources(new DefaultArtifactSources(ArtifactSources.MAIN, sources, resources));

        final Path testClassesDir = Path.of(build.getTestOutputDirectory());
        final List<SourceDir> testSources = new ArrayList<>(project.getCompileSourceRoots().size());
        project.getTestCompileSourceRoots().forEach(s -> testSources.add(new DefaultSourceDir(Path.of(s), testClassesDir,
                // FIXME: do tests have generated sources?
                null)));
        final List<SourceDir> testResources = new ArrayList<>(build.getTestResources().size());
        for (Resource r : build.getTestResources()) {
            testResources.add(new DefaultSourceDir(Path.of(r.getDirectory()),
                    r.getTargetPath() == null ? testClassesDir : Path.of(r.getTargetPath()),
                    // FIXME: do tests have generated sources?
                    null));
        }
        moduleBuilder.addArtifactSources(new DefaultArtifactSources(ArtifactSources.TEST, testSources, testResources));

        moduleBuilder.setBuildFile(project.getFile().toPath());

        return moduleBuilder.build();
    }

    private static WorkspaceModuleId getId(MavenProject project) {
        return new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }
}
