package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

public class DevModeConditionalDependencyWithExtraConditionTestCase extends CollectDependenciesBase {

    @Override
    protected QuarkusBootstrap.Mode getBootstrapMode() {
        return QuarkusBootstrap.Mode.DEV;
    }

    @Override
    protected void setupDependencies() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        install(extA, false);
        addCollectedDeploymentDep(extA.getDeployment());

        installAsDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        install(extB, false);
        addCollectedDep(extB.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setDependencyCondition(extA);
        install(extC, false);
        addCollectedDep(extC.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        install(extD, false);

        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        extE.setDependencyCondition(extD);
        install(extE, false);

        final TsQuarkusExt extG = new TsQuarkusExt("ext-g");
        extG.setConditionalDevDeps(extB.getRuntime(), extC.getRuntime(), extE.getRuntime());
        install(extG, false);
        installAsDep(extG.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extG.getDeployment());
    }
}
