package io.quarkus.bootstrap.model.gradle;

import java.util.Collection;

public interface Workspace {

    WorkspaceModule getMainModule();

    Collection<WorkspaceModule> getAllModules();

    WorkspaceModule getModule(ArtifactCoords key);

}
