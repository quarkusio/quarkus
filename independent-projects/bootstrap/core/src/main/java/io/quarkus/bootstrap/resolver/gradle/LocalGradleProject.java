package io.quarkus.bootstrap.resolver.gradle;

import io.quarkus.bootstrap.resolver.gradle.tooling.RemoteAppModelGradleResolver;
import java.nio.file.Path;

import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.LocalProject;

public class LocalGradleProject implements LocalProject {

    private final ProjectConnection project;

    public LocalGradleProject(ProjectConnection project) {
        this.project = project;
    }

    public static LocalGradleProject load(Path path) throws BootstrapException {
        try {
            ProjectConnection project = GradleConnector.newConnector()
                .forProjectDirectory(path.toFile())
                .connect();
            return new LocalGradleProject(project);

        } catch (GradleConnectionException ex) {
            throw new BootstrapException("Unable to load Gradle project", ex);
        }
    }

    @Override
    public AppModelResolver createAppModelResolver() throws AppModelResolverException {
        return new RemoteAppModelGradleResolver(project);
    }

    @Override
    public AppArtifact getAppArtifact() {
        return new AppArtifact("test", "test", "1.0");
    }

    @Override
    public void close() {
        project.close();
    }
}
