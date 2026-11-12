package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * The properties a dependency version and an imported BOM version are derived from are not defined in the
 * POM at all, they are expected to be passed in on the command line. This is how the Quarkus platform BOM
 * version is often supplied, e.g. {@code mvn package -Dquarkus.platform.version=999-SNAPSHOT}.
 * <p>
 * An unresolved property in an imported BOM version fails the build, as opposed to an unresolved property
 * in a plain dependency version, which may simply end up in the resolved application model.
 */
public class MissingPomPropertyProvidedOnCommandLineTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact x3 = new TsArtifact("x", "3");
        install(x3);
        final TsArtifact y4 = new TsArtifact("y", "4");
        install(y4);
        install(TsArtifact.pom("bom", "4").addManagedDependency(new TsDependency(y4)));

        // neither x.version nor bom.version is among the POM properties
        addDep(new TsArtifact("x", "${x.version}"));
        root.addManagedDependency(new TsDependency(TsArtifact.pom("bom", "${bom.version}"), "import"));
        // the version of y is managed by the imported BOM
        addDep(TsArtifact.ga("y"));

        // both are expected to be provided on the command line
        setSystemProperty("x.version", "3");
        setSystemProperty("bom.version", "4");

        addCollectedDep(x3, DependencyFlags.DIRECT);
        addCollectedDep(y4, DependencyFlags.DIRECT);
    }

    @Override
    protected BootstrapAppModelResolver getTestResolver() throws Exception {
        return newWorkspaceResolver();
    }
}
