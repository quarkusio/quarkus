package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class DirectDependencyVersionOverridesTransitiveVersionTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        installAsDep(new TsArtifact("required-dep1")
                .addDependency(new TsArtifact("common-b", "2")));
        installAsDep(new TsArtifact("common-b", "1"));
    }
}
