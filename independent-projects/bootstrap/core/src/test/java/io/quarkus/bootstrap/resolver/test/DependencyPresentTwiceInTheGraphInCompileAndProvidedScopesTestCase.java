package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class DependencyPresentTwiceInTheGraphInCompileAndProvidedScopesTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact common1 = new TsArtifact("common", "1");
        final TsArtifact common2 = new TsArtifact("common", "2");

        installAsDep(new TsArtifact("required-dep1")
                .addDependency(new TsDependency(common1, "provided")),
                true);
        install(common1, false);
        install(common2, true);
        installAsDep(new TsArtifact("required-dep2")
                .addDependency(common2),
                true);
    }
}
