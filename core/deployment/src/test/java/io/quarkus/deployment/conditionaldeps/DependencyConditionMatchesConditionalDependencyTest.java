package io.quarkus.deployment.conditionaldeps;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.ExecutableOutputOutcomeTestBase;

public class DependencyConditionMatchesConditionalDependencyTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyCondition(extA);

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        extD.setDependencyCondition(extB);
        extD.setConditionalDeps(extB);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setConditionalDeps(extD);

        install(extA);
        install(extB);
        install(extC);
        install(extD);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extB.getRuntime());
        addToExpectedLib(extC.getRuntime());
        addToExpectedLib(extD.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extC)
                .addDependency(extA);
    }

    @Override
    protected String[] expectedExtensionDependencies() {
        return new String[] {
                "ext-a",
                "ext-b",
                "ext-c",
                "ext-d"
        };
    }
}
