package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 *
 */
public class VersionPropertyDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact x12 = new TsArtifact("x", "2");
        final TsArtifact x13 = new TsArtifact("x", "3");

        install(x12);
        install(x13);

        setPomProperty("x.version", "2");
        addDep(new TsArtifact("x", "${x.version}"));

        addCollectedDep(x12, DependencyFlags.DIRECT);
    }
}
