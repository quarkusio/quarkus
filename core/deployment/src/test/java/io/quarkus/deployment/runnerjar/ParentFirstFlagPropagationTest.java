package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;

public class ParentFirstFlagPropagationTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {

        final TsArtifact extADep1 = TsArtifact.jar("ext-a-dep");
        addToExpectedLib(extADep1);
        final TsArtifact extBDep1 = TsArtifact.jar("ext-b-dep");
        addToExpectedLib(extBDep1);
        final TsArtifact extBDepTrans1 = TsArtifact.jar("ext-b-dep-trans");
        addToExpectedLib(extBDepTrans1);
        extBDep1.addDependency(extBDepTrans1);

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.getRuntime()
                .addDependency(extADep1)
                .addDependency(extBDep1, extBDepTrans1);
        addToExpectedLib(extA.getRuntime());
        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        addToExpectedLib(extB.getRuntime());
        extB.getRuntime().addDependency(extBDep1);
        extB.addDependency(extA);
        extB.setDependencyFlag(extBDep1.getKey(), DependencyFlags.CLASSLOADER_PARENT_FIRST);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extB);
    }

    @Override
    protected void assertAppModel(ApplicationModel appModel) throws Exception {
        final Set<Dependency> expectedDeployDeps = Set.of(
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-b-deployment", "1"), "compile",
                        DependencyFlags.DEPLOYMENT_CP),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a-deployment", "1"), "compile",
                        DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expectedDeployDeps, appModel.getDependencies().stream().filter(d -> d.isDeploymentCp() && !d.isRuntimeCp())
                .map(d -> new ArtifactDependency(d)).collect(Collectors.toSet()));

        final Set<Dependency> expectedRuntimeDeps = Set.of(
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a", "1"), "compile",
                        DependencyFlags.RUNTIME_EXTENSION_ARTIFACT, DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a-dep", "1"), "compile",
                        DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-b", "1"), "compile",
                        DependencyFlags.DIRECT, DependencyFlags.RUNTIME_EXTENSION_ARTIFACT, DependencyFlags.RUNTIME_CP,
                        DependencyFlags.DEPLOYMENT_CP, DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-b-dep", "1"), "compile",
                        DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP, DependencyFlags.CLASSLOADER_PARENT_FIRST),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-b-dep-trans", "1"), "compile",
                        DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP, DependencyFlags.CLASSLOADER_PARENT_FIRST));
        assertEquals(expectedRuntimeDeps,
                appModel.getRuntimeDependencies().stream().map(d -> new ArtifactDependency(d)).collect(Collectors.toSet()));
        final Set<Dependency> expectedFullDeps = new HashSet<>();
        expectedFullDeps.addAll(expectedDeployDeps);
        expectedFullDeps.addAll(expectedRuntimeDeps);
        assertEquals(expectedFullDeps,
                appModel.getDependencies().stream().map(d -> new ArtifactDependency(d)).collect(Collectors.toSet()));
    }
}
