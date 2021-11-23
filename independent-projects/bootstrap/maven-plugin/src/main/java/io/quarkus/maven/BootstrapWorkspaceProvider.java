package io.quarkus.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.logging.Logger;

@Component(role = BootstrapWorkspaceProvider.class, instantiationStrategy = "singleton")
public class BootstrapWorkspaceProvider {

    private final Path base;
    private boolean initialized;
    private LocalProject origin;

    public BootstrapWorkspaceProvider() {
        // load the workspace lazily on request, in case the component is injected but the logic using it is skipped
        base = Paths.get("").normalize().toAbsolutePath();
    }

    public LocalProject origin() {
        if (!initialized) {
            Path modulePath = base;
            final String alternatePomParam = BootstrapMavenOptions.newInstance()
                    .getOptionValue(BootstrapMavenOptions.ALTERNATE_POM_FILE);
            if (alternatePomParam != null) {
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
