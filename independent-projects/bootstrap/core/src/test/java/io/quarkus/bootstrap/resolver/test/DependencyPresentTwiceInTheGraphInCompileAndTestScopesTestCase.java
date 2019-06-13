package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class DependencyPresentTwiceInTheGraphInCompileAndTestScopesTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact common1 = new TsArtifact("common", "1");

        installAsDep(new TsArtifact("required-dep1")
                .addDependency(new TsDependency(common1, "test")),
                true);
        install(common1, true);
        installAsDep(new TsArtifact("required-dep2")
                .addDependency(common1),
                true);
    }
}
