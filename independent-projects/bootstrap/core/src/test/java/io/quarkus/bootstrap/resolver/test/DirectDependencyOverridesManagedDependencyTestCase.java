package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;

/**
 *
 */
public class DirectDependencyOverridesManagedDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact x12 = new TsArtifact("x", "2");
        final TsArtifact x13 = new TsArtifact("x", "3");

        installAsDep(x12);
        install(x13);
        addManagedDep(x13);
    }
}