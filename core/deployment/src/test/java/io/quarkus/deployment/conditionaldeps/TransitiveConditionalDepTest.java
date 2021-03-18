package io.quarkus.deployment.conditionaldeps;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.ExecutableOutputOutcomeTestBase;

public class TransitiveConditionalDepTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        // these H and I are not going to be satisfied
        final TsQuarkusExt extH = new TsQuarkusExt("ext-h");
        install(extH);
        final TsQuarkusExt extIConditional = new TsQuarkusExt("ext-i-conditional");
        extIConditional.setDependencyCondition(extH);
        install(extIConditional);

        final TsQuarkusExt extGConditional = new TsQuarkusExt("ext-g-conditional");

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setConditionalDeps(extGConditional);

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.addDependency(extA);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.addDependency(extB);

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        extD.addDependency(extB);

        final TsQuarkusExt extEConditional = new TsQuarkusExt("ext-e-conditional");
        extEConditional.setDependencyCondition(extB);
        install(extEConditional);

        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        extF.setConditionalDeps(extEConditional, extIConditional);

        extGConditional.setDependencyCondition(extC);
        extGConditional.addDependency(extF);
        install(extGConditional);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extB.getRuntime());
        addToExpectedLib(extC.getRuntime());
        addToExpectedLib(extD.getRuntime());
        addToExpectedLib(extEConditional.getRuntime());
        addToExpectedLib(extF.getRuntime());
        addToExpectedLib(extGConditional.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extC)
                .addDependency(extD);
    }

    @Override
    protected String[] expectedExtensionDependencies() {
        return new String[] {
                "ext-a",
                "ext-b",
                "ext-c",
                "ext-d",
                "ext-e-conditional",
                "ext-f",
                "ext-g-conditional"
        };
    }
}
