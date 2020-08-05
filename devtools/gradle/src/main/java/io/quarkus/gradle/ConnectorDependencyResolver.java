package io.quarkus.gradle;

import java.nio.file.Path;
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

public final class ConnectorDependencyResolver {

    private List<Dependency> dependencies = null;

    List<Dependency> getDependencies(String buildContent, Path projectDirPath) {
        if (dependencies == null) {
            EclipseProject eclipseProject = null;
            if (buildContent != null) {
                try {
                    ProjectConnection connection = GradleConnector.newConnector()
                            .forProjectDirectory(projectDirPath.toFile())
                            .connect();
                    eclipseProject = connection.getModel(EclipseProject.class);
                } catch (BuildException e) {
                    // ignore this error.
                    e.printStackTrace();
                }
            }
            if (eclipseProject != null) {
                dependencies = eclipseProject.getClasspath().stream().map(this::gradleModuleVersionToDependency)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                dependencies = Collections.emptyList();
            }
            dependencies = Collections.unmodifiableList(dependencies);
        }
        return dependencies;
    }

    private Dependency gradleModuleVersionToDependency(EclipseExternalDependency eed) {
        Dependency dependency = new Dependency();
        if (eed == null || eed.getGradleModuleVersion() == null) {
            // local dependencies are ignored
            return null;
        }
        dependency.setGroupId(eed.getGradleModuleVersion().getGroup());
        dependency.setArtifactId(eed.getGradleModuleVersion().getName());
        dependency.setVersion(eed.getGradleModuleVersion().getVersion());
        return dependency;
    }
}
