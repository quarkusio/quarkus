package io.quarkus.deployment.conditionaldeps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.ExecutableOutputOutcomeTestBase;

public class ConditionalDependencyWithSingleConditionTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyCondition(extA);
        install(extB);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setConditionalDeps(extB);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extB.getRuntime());
        addToExpectedLib(extC.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extC)
                .addDependency(extA);
    }

    @Override
    protected void assertAppModel(AppModel appModel) throws Exception {
        final List<AppDependency> deploymentDeps = appModel.getDeploymentDependencies();
        final Set<AppDependency> expected = new HashSet<>();
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-c-deployment", TsArtifact.DEFAULT_VERSION), "compile"));
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-a-deployment", TsArtifact.DEFAULT_VERSION), "compile"));
        expected.add(new AppDependency(
                new AppArtifact(TsArtifact.DEFAULT_GROUP_ID, "ext-b-deployment", TsArtifact.DEFAULT_VERSION), "runtime"));
        assertEquals(expected, new HashSet<>(deploymentDeps));
    }
}
