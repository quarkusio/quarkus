package io.quarkus.gradle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Dependency;

import io.quarkus.devtools.project.buildfile.AbstractGroovyGradleBuildFile;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public class GroovyBuildFileFromConnector extends AbstractGroovyGradleBuildFile {

    private final ConnectorDependencyResolver dependencyResolver = new ConnectorDependencyResolver();

    public GroovyBuildFileFromConnector(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor) {
        super(projectDirPath, platformDescriptor);
    }

    public GroovyBuildFileFromConnector(Path projectDirPath, QuarkusPlatformDescriptor platformDescriptor,
            Path rootProjectPath) {
        super(projectDirPath, platformDescriptor, rootProjectPath);
    }

    @Override
    public List<Dependency> getDependencies() throws IOException {
        return dependencyResolver.getDependencies(getBuildContent(), getProjectDirPath());
    }

}
