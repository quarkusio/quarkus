package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class DependencyPresentTwiceInTheGraphWithDifferentClassifierAndTypeTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact common1 = new TsArtifact("common", "1");
        install(common1, true);

        installAsDep(new TsArtifact("required-dep1")
                .addDependency(common1),
                true);

        final TsArtifact commonClient2 = new TsArtifact(TsArtifact.DEFAULT_GROUP_ID, "common", "client", "txt", "2");
        install(commonClient2, true);

        installAsDep(new TsArtifact("required-dep2")
                .addDependency(commonClient2),
                true);

        installAsDep(new TsArtifact(TsArtifact.DEFAULT_GROUP_ID, "common", "", "jar", "3"), true);
    }
}
