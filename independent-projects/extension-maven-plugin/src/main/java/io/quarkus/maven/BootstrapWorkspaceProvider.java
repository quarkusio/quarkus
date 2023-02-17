package io.quarkus.maven;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Named;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;

@Singleton
@Named
public class BootstrapWorkspaceProvider {

    // Mostly for use by tests
    static BootstrapWorkspaceProvider newInstance(String dirName) {
        // If we get passed in a directory explicitly, don't try and tack the "-f" value onto it
        return new BootstrapWorkspaceProvider(dirName, false);
    }

    private final Path base;
    private boolean initialized;
    private LocalProject origin;
    private final boolean honourAlternatePomFile;

    public BootstrapWorkspaceProvider() {
        this("", true);
    }

    private BootstrapWorkspaceProvider(String dirName, boolean honourAlternatePomFile) {
        this.honourAlternatePomFile = honourAlternatePomFile;
        // load the workspace lazily on request, in case the component is injected but the logic using it is skipped
        base = Paths.get(dirName).normalize().toAbsolutePath();
    }

    public LocalProject origin() {
        if (!initialized) {
            Path modulePath = base;
            final String alternatePomParam = BootstrapMavenOptions.newInstance()
                    .getOptionValue(BootstrapMavenOptions.ALTERNATE_POM_FILE);
            if (alternatePomParam != null && honourAlternatePomFile) {
                final Path path = Paths.get(alternatePomParam);
                if (path.isAbsolute()) {
                    modulePath = path;
                } else {
                    modulePath = base.resolve(path);
                }
            }
            try {
                origin = LocalProject.loadWorkspace(modulePath);
            } catch (BootstrapMavenException e) {
                Logger.getLogger(BootstrapWorkspaceProvider.class).warn("Failed to load workspace for " + modulePath);
            }
            initialized = true;
        }
        return origin;
    }

    public LocalWorkspace workspace() {
        final LocalProject origin = origin();
        return origin == null ? null : origin.getWorkspace();
    }

    public LocalProject getProject(String groupId, String artifactId) {
        final LocalWorkspace workspace = workspace();
        return workspace == null ? null : workspace.getProject(groupId, artifactId);
    }
}
