package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class ExclusionsTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact requiredTransitive = new TsArtifact("required-transitive")
                .addDependency(
                        new TsArtifact("excluded-dep", "2")
                                .addDependency(new TsArtifact("other-dep")));
        install(requiredTransitive, true);

        final TsArtifact otherDep2 = new TsArtifact("other-dep", "2");
        install(otherDep2, true);

        final TsArtifact otherRequiredTransitive = new TsArtifact("other-required-transitive")
                .addDependency(otherDep2);
        install(otherRequiredTransitive, true);

        installAsDep(
                new TsArtifact("required-dep1")
                        .addDependency(
                                new TsDependency(requiredTransitive)
                                        .exclude("excluded-dep"))
                        .addDependency(otherRequiredTransitive));
    }
}
