package io.quarkus.deployment.conditionaldeps;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.ExecutableOutputOutcomeTestBase;

public class ExcludedTransitiveConditionalDependencyTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyCondition(extA);
        install(extB);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setConditionalDeps(extB);

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        extD.addDependency(extC);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extC.getRuntime());
        addToExpectedLib(extD.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extD, extB.getRuntime())
                .addDependency(extA);
    }

    @Override
    protected String[] expectedExtensionDependencies() {
        return new String[] {
                "ext-a",
                "ext-c",
                "ext-d"
        };
    }
}
