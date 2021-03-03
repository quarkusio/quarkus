package io.quarkus.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = BootstrapWorkspaceProvider.class, instantiationStrategy = "singleton")
public class BootstrapWorkspaceProvider {

    private final Path base;
    private boolean loaded;
    private LocalProject origin;

    public BootstrapWorkspaceProvider() {
        // load the workspace lazily on request, in case the component is injected but the logic using it is skipped 
        base = Paths.get("").normalize().toAbsolutePath();
    }

    public LocalProject origin() {
        if (!loaded) {
            try {
                origin = LocalProject.loadWorkspace(base);
            } catch (BootstrapMavenException e) {
            }
            loaded = true;
        }
        return origin;
    }

    public LocalWorkspace workspace() {
        return origin().getWorkspace();
    }
}
