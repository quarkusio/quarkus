package io.quarkus.bootstrap.resolver.model;

import java.util.Collection;

public interface Workspace {

    WorkspaceModule getMainModule();

    Collection<WorkspaceModule> getAllModules();

    WorkspaceModule getModule(ArtifactCoords key);

}
