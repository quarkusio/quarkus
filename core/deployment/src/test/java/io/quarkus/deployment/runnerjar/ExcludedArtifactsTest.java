package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

public class ExcludedArtifactsTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {

        final TsArtifact extADep = TsArtifact.jar("ext-a-dep");
        // excluded in the extension descriptor addToExpectedLib(extADep);

        final TsArtifact depC = TsArtifact.jar("dep-c");
        addToExpectedLib(depC);
        extADep.addDependency(depC);

        final TsArtifact depE = TsArtifact.jar("org.banned", "dep-e", "1");
        depC.addDependency(depE);
        final TsArtifact depG = TsArtifact.jar("dep-g");
        depE.addDependency(depG);
        addToExpectedLib(depG);

        final TsArtifact extADeploymentDep = TsArtifact.jar("ext-a-deployment-dep");

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        addToExpectedLib(extA.getRuntime());
        extA.getRuntime().addDependency(extADep);
        extA.getDeployment().addDependency(extADeploymentDep);

        extA.getDescriptor().set("excluded-artifacts",
                extADep.getKey().toString() + ",org.banned*");

        final TsArtifact depB = TsArtifact.jar("dep-b");
        addToExpectedLib(depB);

        final TsArtifact depD = TsArtifact.jar("org.banned.too", "dep-d", "1");
        depB.addDependency(depD);

        final TsArtifact depF = TsArtifact.jar("org.banned", "dep-f", "1");
        depB.addDependency(depF);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA)
                .addDependency(new TsDependency(depB));
    }

    @Override
    protected void assertAppModel(ApplicationModel model) throws Exception {
        Set<Dependency> expected = new HashSet<>();
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a-deployment", "1"),
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a-deployment-dep", "1"),
                DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, getDeploymentOnlyDeps(model));

        expected = new HashSet<>();
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a", "1"),
                DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP, DependencyFlags.RUNTIME_EXTENSION_ARTIFACT,
                DependencyFlags.DIRECT, DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "dep-c", "1"),
                DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "dep-b", "1"),
                DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP, DependencyFlags.DIRECT));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "dep-g", "1"),
                DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, getDependenciesWithFlag(model, DependencyFlags.RUNTIME_CP));

        for (ResolvedDependency dep : model.getDependencies()) {
            switch (dep.getArtifactId()) {
                case "ext-a":
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.jarWithFlags(TsArtifact.DEFAULT_GROUP_ID, "ext-a-dep", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.MISSING_FROM_APPLICATION));
                    break;
                case "ext-a-deployment":
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.jarWithFlags(TsArtifact.DEFAULT_GROUP_ID, "ext-a", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP |
                                            DependencyFlags.RUNTIME_EXTENSION_ARTIFACT |
                                            DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT),
                            Dependency.jarWithFlags(TsArtifact.DEFAULT_GROUP_ID, "ext-a-deployment-dep", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.DEPLOYMENT_CP));
                    break;
                case "ext-a-dep":
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.jarWithFlags(TsArtifact.DEFAULT_GROUP_ID, "dep-c", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP));
                    break;
                case "ext-a-deployment-dep":
                    assertThat(dep.getDirectDependencies()).isEmpty();
                    break;
                case "dep-c":
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.jarWithFlags("org.banned", "dep-e", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.MISSING_FROM_APPLICATION));
                    break;
                case "dep-g":
                    assertThat(dep.getDirectDependencies()).isEmpty();
                    break;
                case "dep-b":
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.jarWithFlags("org.banned.too", "dep-d", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.MISSING_FROM_APPLICATION),
                            Dependency.jarWithFlags("org.banned", "dep-f", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.MISSING_FROM_APPLICATION));
                    break;
                default:
                    fail("Unexpected dependency: " + dep.toCompactCoords());
            }
        }
    }
}
