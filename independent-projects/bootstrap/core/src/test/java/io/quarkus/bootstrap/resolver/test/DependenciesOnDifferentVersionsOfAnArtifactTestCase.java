package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class DependenciesOnDifferentVersionsOfAnArtifactTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact common1 = new TsArtifact("common", "1");
        install(common1, true);

        installAsDep(new TsArtifact("required-dep1")
                .addDependency(common1));
        installAsDep(new TsArtifact("required-dep2")
                .addDependency(new TsArtifact("common", "2")));
    }
}
