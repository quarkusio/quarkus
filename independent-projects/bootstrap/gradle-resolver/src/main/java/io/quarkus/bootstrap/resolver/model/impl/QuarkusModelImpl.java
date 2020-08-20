package io.quarkus.bootstrap.resolver.model.impl;

import io.quarkus.bootstrap.resolver.model.Dependency;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.resolver.model.Workspace;
import java.io.Serializable;
import java.util.List;

public class QuarkusModelImpl implements QuarkusModel, Serializable {

    private final Workspace workspace;
    private final List<Dependency> appDependencies;
    private final List<Dependency> extensionDependencies;
    private final List<Dependency> enforcedPlatformDependencies;

    public QuarkusModelImpl(Workspace workspace,
            List<Dependency> appDependencies,
            List<Dependency> extensionDependencies,
            List<Dependency> enforcedPlatformDependencies) {
        this.workspace = workspace;
        this.appDependencies = appDependencies;
        this.extensionDependencies = extensionDependencies;
        this.enforcedPlatformDependencies = enforcedPlatformDependencies;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public List<Dependency> getAppDependencies() {
        return appDependencies;
    }

    @Override
    public List<Dependency> getExtensionDependencies() {
        return extensionDependencies;
    }

    @Override
    public List<Dependency> getEnforcedPlatformDependencies() {
        return enforcedPlatformDependencies;
    }
}
