package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvidedScopeDepsAreNotCollectedTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact notCollected = new TsArtifact("not-collected");
        //install(notCollected, false);

        final TsArtifact common1 = new TsArtifact("common", "1")
                .addDependency(
                        new TsDependency(
                                notCollected, "provided"));
        install(common1);
        addCollectedDep(common1, DependencyFlags.COMPILE_ONLY);

        installAsDep(new TsArtifact("required-dep")
                .addDependency(common1),
                true);

        installAsDep(
                new TsDependency(
                        new TsArtifact("provided-dep")
                                .addDependency(
                                        new TsArtifact("common", "2")),
                        "provided"),
                false);
    }
}
