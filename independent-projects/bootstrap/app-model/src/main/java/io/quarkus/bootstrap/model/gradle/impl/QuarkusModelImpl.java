package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.bootstrap.model.gradle.Dependency;
import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import io.quarkus.bootstrap.model.gradle.Workspace;
import java.io.Serializable;
import java.util.List;

public class QuarkusModelImpl implements QuarkusModel, Serializable {

    private final Workspace workspace;
    private final List<Dependency> dependencies;
    private final PlatformImports platformImports;

    public QuarkusModelImpl(Workspace workspace,
            List<Dependency> dependencies,
            PlatformImports platformImports) {
        this.workspace = workspace;
        this.dependencies = dependencies;
        this.platformImports = platformImports;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public PlatformImports getPlatformImports() {
        return platformImports;
    }
}
