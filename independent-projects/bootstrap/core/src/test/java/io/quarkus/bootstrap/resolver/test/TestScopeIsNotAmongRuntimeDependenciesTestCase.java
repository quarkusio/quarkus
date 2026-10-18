package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class TestScopeIsNotAmongRuntimeDependenciesTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {
        installAsDep(new TsDependency(new TsArtifact("required-dep-a"), "compile"), true);
        installAsDep(new TsDependency(new TsArtifact("required-dep-b"), "runtime"), true);

        installAsDep(
                new TsDependency(
                        new TsArtifact("direct-test-dep")
                                .addDependency(new TsArtifact("common", "1")
                                        .addDependency(new TsDependency(new TsArtifact("not-collected"), "test"))),
                        "test"),
                false);

        final TsArtifact common = install(new TsArtifact("common", "2"), true);
        installAsDep(
                new TsArtifact("required-dep-c")
                        .addDependency(common),
                true);
    }
}
