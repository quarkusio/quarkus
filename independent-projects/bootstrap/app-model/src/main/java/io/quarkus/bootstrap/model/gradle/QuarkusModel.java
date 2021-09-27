package io.quarkus.bootstrap.model.gradle;

import io.quarkus.bootstrap.model.PlatformImports;
import java.util.List;

public interface QuarkusModel {

    Workspace getWorkspace();

    List<Dependency> getDependencies();

    PlatformImports getPlatformImports();
}
