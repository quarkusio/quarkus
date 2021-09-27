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

public class CascadingConditionalDependenciesTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        final TsQuarkusExt extG = new TsQuarkusExt("ext-g");
        final TsQuarkusExt extH = new TsQuarkusExt("ext-h");

        extA.setConditionalDeps(extB, extF);

        extB.setDependencyCondition(extC);
        extB.setConditionalDeps(extD);

        extD.setDependencyCondition(extE);

        extE.addDependency(extC);
        extE.setDependencyCondition(extA);
        extE.setDependencyCondition(extG);

        extF.setDependencyCondition(extD);

        extG.setDependencyCondition(extH);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extB.getRuntime());
        addToExpectedLib(extC.getRuntime());
        addToExpectedLib(extD.getRuntime());
        addToExpectedLib(extE.getRuntime());
        addToExpectedLib(extF.getRuntime());

        install(extA);
        install(extB);
        install(extC);
        install(extD);
        install(extE);
        install(extF);
        install(extG);
        install(extH);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA)
                .addDependency(extE);
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
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-b-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                AppDependency.DEPLOYMENT_CP_FLAG));
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-d-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                AppDependency.DEPLOYMENT_CP_FLAG));
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-e-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                AppDependency.DEPLOYMENT_CP_FLAG));
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-f-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                AppDependency.DEPLOYMENT_CP_FLAG));
        assertEquals(expected, new HashSet<>(deploymentDeps));
    }
}
