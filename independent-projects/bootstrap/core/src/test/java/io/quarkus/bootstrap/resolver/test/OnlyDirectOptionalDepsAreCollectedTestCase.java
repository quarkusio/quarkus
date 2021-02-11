package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class OnlyDirectOptionalDepsAreCollectedTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {
        installAsDep(new TsDependency(new TsArtifact("required-dep-a"), "compile"), true);
        installAsDep(new TsDependency(new TsArtifact("required-dep-b"), "runtime"), true);

        final TsArtifact nonOptionalCommon = new TsArtifact("non-optional-common");
        install(nonOptionalCommon, "compile", true);

        installAsDep(
                new TsDependency(
                        new TsArtifact("optional-dep")
                                .addDependency(new TsDependency(new TsArtifact("common", "1"), true))
                                .addDependency(new TsDependency(nonOptionalCommon, false))
                                .addDependency(new TsDependency(new TsArtifact("other", "1"), true)),
                        true),
                true);

        TsArtifact common2 = install(new TsArtifact("common", "2"), true);
        installAsDep(new TsArtifact("required-dep-c")
                .addDependency(common2), true);
    }
}
