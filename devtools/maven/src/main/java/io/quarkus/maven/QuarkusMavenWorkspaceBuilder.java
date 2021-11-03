package io.quarkus.maven;

import java.io.File;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.workspace.DefaultProcessedSources;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
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
        project.getCompileSourceRoots()
                .forEach(s -> module.addMainSources(new DefaultProcessedSources(new File(s), classesDir)));
        final File testClassesDir = new File(build.getTestOutputDirectory());
        project.getTestCompileSourceRoots()
                .forEach(s -> module.addTestSources(new DefaultProcessedSources(new File(s), testClassesDir)));

        for (Resource r : build.getResources()) {
            module.addMainResources(new DefaultProcessedSources(new File(r.getDirectory()),
                    r.getTargetPath() == null ? classesDir : new File(r.getTargetPath())));
        }

        for (Resource r : build.getTestResources()) {
            module.addTestResources(new DefaultProcessedSources(new File(r.getDirectory()),
                    r.getTargetPath() == null ? testClassesDir : new File(r.getTargetPath())));
        }

        module.setBuildFiles(PathList.of(project.getFile().toPath()));

        return module;
    }

    private static WorkspaceModuleId getId(MavenProject project) {
        return new GAV(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }
}
