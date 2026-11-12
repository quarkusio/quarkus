package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 *
 */
public class SystemPropertyOverridesPomPropertyDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact x12 = new TsArtifact("x", "2");
        final TsArtifact x13 = new TsArtifact("x", "3");

        install(x12);
        install(x13);

        // x.version in pom is 2
        setPomProperty("x.version", "2");
        addDep(new TsArtifact("x", "${x.version}"));

        // the system property of x.version is 3
        setSystemProperty("x.version", "3");

        // it is expected that the system property will dominate
        addCollectedDep(x13, DependencyFlags.DIRECT);
    }

    @Override
    protected BootstrapAppModelResolver getTestResolver() throws Exception {
        return newWorkspaceResolver();
    }
}
