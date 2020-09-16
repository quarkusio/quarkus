package io.quarkus.gradle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.project.buildfile.AbstractKotlinGradleBuildFile;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public class KotlinBuildFileFromConnector extends AbstractKotlinGradleBuildFile {

    private final ConnectorDependencyResolver dependencyResolver = new ConnectorDependencyResolver();

    public KotlinBuildFileFromConnector(Path projectDirPath, QuarkusPlatformDescriptor platformDescriptor) {
        super(projectDirPath, platformDescriptor);
    }

    public KotlinBuildFileFromConnector(Path projectDirPath, QuarkusPlatformDescriptor platformDescriptor,
            Path rootProjectPath) {
        super(projectDirPath, platformDescriptor, rootProjectPath);
    }

    @Override
    public List<AppArtifactCoords> getDependencies() throws IOException {
        return dependencyResolver.getDependencies(getBuildContent(), getProjectDirPath());
    }
}
