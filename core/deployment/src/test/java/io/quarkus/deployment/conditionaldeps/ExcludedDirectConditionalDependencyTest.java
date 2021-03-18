package io.quarkus.deployment.conditionaldeps;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.ExecutableOutputOutcomeTestBase;

public class ExcludedDirectConditionalDependencyTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyCondition(extA);
        install(extB);

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");

        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        extE.setDependencyCondition(extD);
        install(extE);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setConditionalDeps(extB, extE);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extC.getRuntime());
        addToExpectedLib(extD.getRuntime());
        addToExpectedLib(extE.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extC, extB.getRuntime())
                .addDependency(extA)
                .addDependency(extD);
    }

    @Override
    protected String[] expectedExtensionDependencies() {
        return new String[] {
                "ext-a",
                "ext-c",
                "ext-d",
                "ext-e"
        };
    }
}
