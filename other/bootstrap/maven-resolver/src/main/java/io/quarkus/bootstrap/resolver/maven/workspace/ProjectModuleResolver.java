package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.workspace.WorkspaceModule;

public interface ProjectModuleResolver {

    WorkspaceModule getProjectModule(String groupId, String artifactId, String version);
}
