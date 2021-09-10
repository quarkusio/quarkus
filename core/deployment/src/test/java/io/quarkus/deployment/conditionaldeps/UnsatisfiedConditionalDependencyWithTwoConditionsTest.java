package io.quarkus.deployment.conditionaldeps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.ExecutableOutputOutcomeTestBase;

public class UnsatisfiedConditionalDependencyWithTwoConditionsTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyCondition(extA, extD);
        install(extB);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setConditionalDeps(extB);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extC.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extC)
                .addDependency(extA);
    }

    @Override
    protected void assertDeploymentDeps(List<AppDependency> deploymentDeps) throws Exception {
        final Set<AppDependency> expected = new HashSet<>();
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-c-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                AppDependency.DEPLOYMENT_CP_FLAG));
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-a-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                AppDependency.DEPLOYMENT_CP_FLAG));
        assertEquals(expected, new HashSet<>(deploymentDeps));
    }
}
