package io.quarkus.deployment.conditionaldeps;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.BootstrapFromOriginalJarTestBase;

public class ConditionalDependencyDependingOnItsDependentTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {

        final TsQuarkusExt extGConditional = new TsQuarkusExt("ext-g-conditional");

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setConditionalDeps(extGConditional);
        extGConditional.addDependency(extA);

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extGConditional.setDependencyCondition(extB);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extB.getRuntime());
        addToExpectedLib(extGConditional.getRuntime());

        install(extA);
        install(extB);
        install(extGConditional);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA)
                .addDependency(extB);
    }

    @Override
    protected String[] expectedExtensionDependencies() {
        return new String[] {
                "ext-a",
                "ext-b",
                "ext-g-conditional"
        };
    }
}
