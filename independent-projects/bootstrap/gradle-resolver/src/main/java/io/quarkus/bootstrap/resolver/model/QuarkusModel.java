package io.quarkus.bootstrap.resolver.model;

import java.util.Set;

public interface QuarkusModel {

    Workspace getWorkspace();

    Set<Dependency> getAppDependencies();

    Set<Dependency> getExtensionDependencies();

}
