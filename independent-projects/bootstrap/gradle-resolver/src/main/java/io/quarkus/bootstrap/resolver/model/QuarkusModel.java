package io.quarkus.bootstrap.resolver.model;

import java.util.List;

public interface QuarkusModel {

    Workspace getWorkspace();

    List<Dependency> getAppDependencies();

    List<Dependency> getExtensionDependencies();

    List<Dependency> getEnforcedPlatformDependencies();
}
