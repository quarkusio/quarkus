package io.quarkus.deployment.runnerjar;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

public class FirstTransitiveDepVersionIsTheEffectiveOneTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsArtifact common1 = TsArtifact.jar("common", "1");
        addToExpectedLib(common1);

        final TsQuarkusExt ext1 = new TsQuarkusExt("ext1");
        ext1.getRuntime().addDependency(common1);
        addToExpectedLib(ext1.getRuntime());

        final TsArtifact common2 = TsArtifact.jar("common", "2");

        final TsQuarkusExt ext2 = new TsQuarkusExt("ext2");
        ext2.getRuntime().addDependency(common2);
        addToExpectedLib(ext2.getRuntime());

        final TsArtifact common3 = TsArtifact.jar("common", "3");

        final TsArtifact directAppDep = TsArtifact.jar("direct-dep").addDependency(common3);
        addToExpectedLib(directAppDep);

        final TsArtifact appJar = TsArtifact.jar("app")
                .addDependency(ext1)
                .addDependency(directAppDep)
                .addDependency(ext2);
        return appJar;
    }
}
