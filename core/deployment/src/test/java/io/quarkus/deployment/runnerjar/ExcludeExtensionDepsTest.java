package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

/**
 * The test allowing to make sure that if we exclude an extension from another extension, the
 * exclusion will be also applied to the deployment dependencies.
 */
public class ExcludeExtensionDepsTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsArtifact extADep1 = TsArtifact.jar("ext-a-dep-1");
        final TsArtifact extADep2 = TsArtifact.jar("ext-a-dep-2");
        final TsArtifact extBDep1 = TsArtifact.jar("ext-b-dep-1");
        addToExpectedLib(extBDep1);
        final TsArtifact extBDep2 = TsArtifact.jar("ext-b-dep-2");
        addToExpectedLib(extBDep2);
        final TsArtifact extBDepTrans1 = TsArtifact.jar("ext-b-dep-trans-1");
        addToExpectedLib(extBDepTrans1);
        final TsArtifact extBDepTrans2 = TsArtifact.jar("ext-b-dep-trans-2");
        addToExpectedLib(extBDepTrans2);
        final TsArtifact depToExclude1 = TsArtifact.jar("ext-dep-exclude-1");
        final TsArtifact depToExclude2 = TsArtifact.jar("ext-dep-exclude-2");
        final TsArtifact depToExclude3 = TsArtifact.jar("ext-dep-exclude-3");
        final TsArtifact depToExclude4 = TsArtifact.jar("ext-dep-exclude-4");
        final TsArtifact depToExclude5 = TsArtifact.jar("ext-dep-exclude-5");
        final TsArtifact depToExclude6 = TsArtifact.jar("ext-dep-exclude-6");
        extBDepTrans2.addDependency(new TsDependency(extBDep2));
        extBDepTrans2.addDependency(new TsDependency(depToExclude6));
        extBDepTrans1.addDependency(new TsDependency(extBDepTrans2));
        extBDepTrans1.addDependency(new TsDependency(depToExclude2));
        extBDepTrans1.addDependency(new TsDependency(depToExclude5));
        extBDep1.addDependency(extBDepTrans1, depToExclude5);
        extBDep1.addDependency(new TsDependency(depToExclude1));

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.getRuntime()
                .addDependency(extADep1);
        extA.getDeployment()
                .addDependency(extADep2);
        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        addToExpectedLib(extB.getRuntime());
        extB.getRuntime()
                .addDependency(extBDep1, depToExclude6)
                .addDependency(extA)
                .addDependency(depToExclude4);
        extB.getDeployment()
                .addDependency(depToExclude3);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extB, extA.getRuntime(), depToExclude1, depToExclude2, depToExclude3, depToExclude4);
    }

    @Override
    protected void assertAppModel(AppModel appModel) throws Exception {
        final Set<AppDependency> expectedDeployDeps = new HashSet<>();
        expectedDeployDeps
                .add(new AppDependency(new AppArtifact("io.quarkus.bootstrap.test", "ext-b-deployment", "1"), "compile",
                        AppDependency.DEPLOYMENT_CP_FLAG));
        assertEquals(expectedDeployDeps, new HashSet<>(appModel.getDeploymentDependencies()));
        final Set<AppDependency> expectedRuntimeDeps = new HashSet<>();
        expectedRuntimeDeps.add(new AppDependency(new AppArtifact("io.quarkus.bootstrap.test", "ext-b", "1"), "compile",
                AppDependency.DIRECT_FLAG, AppDependency.RUNTIME_EXTENSION_ARTIFACT_FLAG, AppDependency.RUNTIME_CP_FLAG,
                AppDependency.DEPLOYMENT_CP_FLAG));
        expectedRuntimeDeps.add(new AppDependency(new AppArtifact("io.quarkus.bootstrap.test", "ext-b-dep-1", "1"), "compile",
                AppDependency.RUNTIME_CP_FLAG, AppDependency.DEPLOYMENT_CP_FLAG));
        expectedRuntimeDeps.add(new AppDependency(new AppArtifact("io.quarkus.bootstrap.test", "ext-b-dep-2", "1"), "compile",
                AppDependency.RUNTIME_CP_FLAG, AppDependency.DEPLOYMENT_CP_FLAG));
        expectedRuntimeDeps
                .add(new AppDependency(new AppArtifact("io.quarkus.bootstrap.test", "ext-b-dep-trans-1", "1"), "compile",
                        AppDependency.RUNTIME_CP_FLAG, AppDependency.DEPLOYMENT_CP_FLAG));
        expectedRuntimeDeps
                .add(new AppDependency(new AppArtifact("io.quarkus.bootstrap.test", "ext-b-dep-trans-2", "1"), "compile",
                        AppDependency.RUNTIME_CP_FLAG, AppDependency.DEPLOYMENT_CP_FLAG));
        assertEquals(expectedRuntimeDeps, new HashSet<>(appModel.getUserDependencies()));
        final Set<AppDependency> expectedFullDeps = new HashSet<>();
        expectedFullDeps.addAll(expectedDeployDeps);
        expectedFullDeps.addAll(expectedRuntimeDeps);
        assertEquals(expectedFullDeps, new HashSet<>(appModel.getFullDeploymentDeps()));
    }
}
