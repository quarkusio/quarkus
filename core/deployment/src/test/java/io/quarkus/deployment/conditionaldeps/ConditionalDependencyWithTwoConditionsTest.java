package io.quarkus.deployment.conditionaldeps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.aether.util.artifact.JavaScopes;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.BootstrapFromOriginalJarTestBase;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;

public class ConditionalDependencyWithTwoConditionsTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyCondition(extA, extD);
        install(extB);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setConditionalDeps(extB);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extB.getRuntime());
        addToExpectedLib(extC.getRuntime());
        addToExpectedLib(extD.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extC)
                .addDependency(extA)
                .addDependency(extD);
    }

    @Override
    protected void assertAppModel(ApplicationModel model) throws Exception {
        final Set<Dependency> expected = new HashSet<>();
        expected.add(new ArtifactDependency(
                ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-c-deployment", TsArtifact.DEFAULT_VERSION),
                JavaScopes.COMPILE,
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-a-deployment", TsArtifact.DEFAULT_VERSION),
                JavaScopes.COMPILE,
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-b-deployment", TsArtifact.DEFAULT_VERSION),
                JavaScopes.COMPILE,
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-d-deployment", TsArtifact.DEFAULT_VERSION),
                JavaScopes.COMPILE,
                DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, getDeploymentOnlyDeps(model));
    }
}
