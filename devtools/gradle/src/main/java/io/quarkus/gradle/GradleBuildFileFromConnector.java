package io.quarkus.gradle;

import java.io.IOException;
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

import io.quarkus.devtools.project.buildfile.AbstractGradleBuildFile;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public class GradleBuildFileFromConnector extends AbstractGradleBuildFile {

    private List<Dependency> dependencies = null;

    public GradleBuildFileFromConnector(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor) {
        super(projectDirPath, platformDescriptor);
    }

    public GradleBuildFileFromConnector(Path projectDirPath, QuarkusPlatformDescriptor platformDescriptor,
            Path rootProjectPath) {
        super(projectDirPath, platformDescriptor, rootProjectPath);
    }

    @Override
    public List<Dependency> getDependencies() throws IOException {
        if (dependencies == null) {
            EclipseProject eclipseProject = null;
            if (getBuildContent() != null) {
                try {
                    ProjectConnection connection = GradleConnector.newConnector()
                            .forProjectDirectory(getProjectDirPath().toFile())
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
            System.err.println("Found null dependency:" + eed);
            return null;
        }
        dependency.setGroupId(eed.getGradleModuleVersion().getGroup());
        dependency.setArtifactId(eed.getGradleModuleVersion().getName());
        dependency.setVersion(eed.getGradleModuleVersion().getVersion());
        return dependency;
    }

}
