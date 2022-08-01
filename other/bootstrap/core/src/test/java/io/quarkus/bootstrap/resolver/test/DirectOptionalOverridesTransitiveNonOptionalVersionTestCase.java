package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class DirectOptionalOverridesTransitiveNonOptionalVersionTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        installAsDep(new TsArtifact("required-a")
                .addDependency(new TsArtifact("common", "1")), true);

        installAsDep(new TsDependency(new TsArtifact("common", "2"), true), true);
    }
}
