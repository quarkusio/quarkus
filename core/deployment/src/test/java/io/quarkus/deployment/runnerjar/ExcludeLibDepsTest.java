package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;

/**
 * The test allowing to make sure that if we exclude a library from an extension, the
 * exclusion will be also applied to the deployment dependencies.
 */
public class ExcludeLibDepsTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsArtifact extADep1 = TsArtifact.jar("ext-a-dep-1");
        addToExpectedLib(extADep1);
        final TsArtifact extADep2 = TsArtifact.jar("ext-a-dep-2");
        addToExpectedLib(extADep2);
        final TsArtifact extADepTrans1 = TsArtifact.jar("ext-a-dep-trans-1");
        addToExpectedLib(extADepTrans1);
        final TsArtifact extADepTrans2 = TsArtifact.jar("ext-a-dep-trans-2");
        addToExpectedLib(extADepTrans2);
        final TsArtifact depToExclude1 = TsArtifact.jar("ext-dep-exclude-1");
        final TsArtifact depToExclude2 = TsArtifact.jar("ext-dep-exclude-2");
        final TsArtifact depToExclude3 = TsArtifact.jar("ext-dep-exclude-3");
        final TsArtifact depToExclude4 = TsArtifact.jar("ext-dep-exclude-4");
        final TsArtifact depToExclude5 = TsArtifact.jar("ext-dep-exclude-5");
        final TsArtifact depToExclude6 = TsArtifact.jar("ext-dep-exclude-6");
        extADepTrans2.addDependency(new TsDependency(extADep2));
        extADepTrans2.addDependency(new TsDependency(depToExclude6));
        extADepTrans1.addDependency(new TsDependency(extADepTrans2));
        extADepTrans1.addDependency(new TsDependency(depToExclude2));
        extADepTrans1.addDependency(new TsDependency(depToExclude5));
        extADep1.addDependency(extADepTrans1, depToExclude5);
        extADep1.addDependency(new TsDependency(depToExclude1));

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        addToExpectedLib(extA.getRuntime());
        extA.getRuntime()
                .addDependency(extADep1, depToExclude6)
                .addDependency(depToExclude3);
        extA.getDeployment()
                .addDependency(depToExclude4);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA, depToExclude1, depToExclude2, depToExclude3, depToExclude4);
    }

    @Override
    protected void assertAppModel(ApplicationModel appModel) throws Exception {
        final Set<Dependency> expectedDeployDeps = new HashSet<>();
        expectedDeployDeps
                .add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a-deployment", "1"), "compile",
                        DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expectedDeployDeps, appModel.getDependencies().stream().filter(d -> d.isDeploymentCp() && !d.isRuntimeCp())
                .map(d -> new ArtifactDependency(d)).collect(Collectors.toSet()));
        final Set<Dependency> expectedRuntimeDeps = new HashSet<>();
        expectedRuntimeDeps.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a", "1"), "compile",
                DependencyFlags.DIRECT, DependencyFlags.RUNTIME_EXTENSION_ARTIFACT, DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP));
        expectedRuntimeDeps.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a-dep-1", "1"), "compile",
                DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP));
        expectedRuntimeDeps.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a-dep-2", "1"), "compile",
                DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP));
        expectedRuntimeDeps
                .add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a-dep-trans-1", "1"), "compile",
                        DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP));
        expectedRuntimeDeps
                .add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a-dep-trans-2", "1"), "compile",
                        DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expectedRuntimeDeps,
                appModel.getRuntimeDependencies().stream().map(d -> new ArtifactDependency(d)).collect(Collectors.toSet()));
        final Set<Dependency> expectedFullDeps = new HashSet<>();
        expectedFullDeps.addAll(expectedDeployDeps);
        expectedFullDeps.addAll(expectedRuntimeDeps);
        assertEquals(expectedFullDeps,
                appModel.getDependencies().stream().map(d -> new ArtifactDependency(d)).collect(Collectors.toSet()));
    }
}
