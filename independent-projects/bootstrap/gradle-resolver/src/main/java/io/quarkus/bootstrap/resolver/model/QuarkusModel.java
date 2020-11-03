package io.quarkus.bootstrap.resolver.model;

import java.util.List;
import java.util.Map;

public interface QuarkusModel {

    Workspace getWorkspace();

    List<Dependency> getAppDependencies();

    List<Dependency> getExtensionDependencies();

    List<Dependency> getEnforcedPlatformDependencies();

    Map<String, String> getPlatformProperties();
}
