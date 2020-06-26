package io.quarkus.bootstrap.resolver.model.impl;

import io.quarkus.bootstrap.resolver.model.Dependency;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.resolver.model.Workspace;
import java.io.Serializable;
import java.util.Set;

public class QuarkusModelImpl implements QuarkusModel, Serializable {

    private final Workspace workspace;
    private final Set<Dependency> appDependencies;
    private final Set<Dependency> extensionDependencies;

    public QuarkusModelImpl(Workspace workspace,
            Set<Dependency> appDependencies,
            Set<Dependency> extensionDependencies) {
        this.workspace = workspace;
        this.appDependencies = appDependencies;
        this.extensionDependencies = extensionDependencies;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public Set<Dependency> getAppDependencies() {
        return appDependencies;
    }

    @Override
    public Set<Dependency> getExtensionDependencies() {
        return extensionDependencies;
    }
}
