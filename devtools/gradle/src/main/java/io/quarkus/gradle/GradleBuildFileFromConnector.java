package io.quarkus.gradle;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.eclipse.EclipseExternalDependency;
import org.gradle.tooling.model.eclipse.EclipseProject;

import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;

public class GradleBuildFileFromConnector extends GradleBuildFile {

    private List<Dependency> dependencies = null;

    public GradleBuildFileFromConnector(ProjectWriter writer) throws IOException {
        super(writer);
    }

    @Override
    public List<Dependency> getDependencies() {
        if (dependencies == null) {
            EclipseProject eclipseProject = null;
            if (getBuildContent() != null) {
                if (getWriter().hasFile()) {
                    try {
                        ProjectConnection connection = GradleConnector.newConnector()
                                .forProjectDirectory(getWriter().getProjectFolder())
                                .connect();
                        eclipseProject = connection.getModel(EclipseProject.class);
                    } catch (BuildException e) {
                        // ignore this error.
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("ECLIPSE PROJECT: " + eclipseProject);
            if (eclipseProject != null) {
                dependencies = eclipseProject.getClasspath().stream().map(this::gradleModuleVersionToDependency)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                dependencies = Collections.emptyList();
            }
            dependencies = Collections.unmodifiableList(dependencies);
            System.out.println("init deps " + dependencies.size());
        } else {
            System.out.println("DEPS " + dependencies.size());
        }
        return dependencies;
    }

    private Dependency gradleModuleVersionToDependency(EclipseExternalDependency eed) {
        Dependency dependency = new Dependency();
        System.out.println("module version: " + eed.getGradleModuleVersion() + " / exists: " + eed.getFile().exists() + " : "
                + eed.getFile());
        if (eed == null || eed.getGradleModuleVersion() == null) {
            // local dependencies are ignored
            System.err.println("Found null dependency:" + eed);
            return null;
        }
        dependency.setGroupId(eed.getGradleModuleVersion().getGroup());
        dependency.setArtifactId(eed.getGradleModuleVersion().getName());
        dependency.setVersion(eed.getGradleModuleVersion().getVersion());
        return dependency;
    }

}
