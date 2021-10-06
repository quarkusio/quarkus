package io.quarkus.deployment.conditionaldeps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.ExecutableOutputOutcomeTestBase;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;

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
    protected void assertAppModel(ApplicationModel appModel) throws Exception {
        final Set<Dependency> deploymentDeps = appModel.getDependencies().stream()
                .filter(d -> d.isDeploymentCp() && !d.isRuntimeCp()).map(d -> new ArtifactDependency(d))
                .collect(Collectors.toSet());
        final Set<Dependency> expected = new HashSet<>();
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-c-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-a-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-b-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, deploymentDeps);
    }
}
