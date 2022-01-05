package io.quarkus.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultSourceDir;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.paths.PathList;

class QuarkusMavenWorkspaceBuilder {

    static void loadModules(MavenProject project, ApplicationModelBuilder modelBuilder) {
    }

    static WorkspaceModule toProjectModule(MavenProject project) {
        final Build build = project.getBuild();
        final DefaultWorkspaceModule module = new DefaultWorkspaceModule(getId(project), project.getBasedir(),
                new File(build.getDirectory()));

        final File classesDir = new File(build.getOutputDirectory());
        final List<SourceDir> sources = new ArrayList<>(project.getCompileSourceRoots().size());
        project.getCompileSourceRoots().forEach(s -> sources.add(new DefaultSourceDir(new File(s), classesDir)));
        final List<SourceDir> resources = new ArrayList<>(build.getResources().size());
        for (Resource r : build.getResources()) {
            resources.add(new DefaultSourceDir(new File(r.getDirectory()),
                    r.getTargetPath() == null ? classesDir : new File(r.getTargetPath())));
        }
        module.addArtifactSources(new DefaultArtifactSources(DefaultWorkspaceModule.MAIN, sources, resources));

        final File testClassesDir = new File(build.getTestOutputDirectory());
        final List<SourceDir> testSources = new ArrayList<>(project.getCompileSourceRoots().size());
        project.getTestCompileSourceRoots().forEach(s -> testSources.add(new DefaultSourceDir(new File(s), testClassesDir)));
        final List<SourceDir> testResources = new ArrayList<>(build.getTestResources().size());
        for (Resource r : build.getTestResources()) {
            testResources.add(new DefaultSourceDir(new File(r.getDirectory()),
                    r.getTargetPath() == null ? testClassesDir : new File(r.getTargetPath())));
        }
        module.addArtifactSources(new DefaultArtifactSources(DefaultWorkspaceModule.TEST, testSources, testResources));

        module.setBuildFiles(PathList.of(project.getFile().toPath()));

        return module;
    }

    private static WorkspaceModuleId getId(MavenProject project) {
        return new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }
}
