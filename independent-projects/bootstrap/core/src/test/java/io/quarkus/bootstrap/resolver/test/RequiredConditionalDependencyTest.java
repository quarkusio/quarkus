package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

public class RequiredConditionalDependencyTest extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        installAsDep(extA);
        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyCondition(extA);
        install(extB, false);
        addCollectedDep(extB.getRuntime(),
                DependencyFlags.RUNTIME_CP
                        | DependencyFlags.DEPLOYMENT_CP
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());
        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setConditionalDeps(extB);
        install(extC, false);
        addCollectedDep(extC.getRuntime(),
                DependencyFlags.RUNTIME_CP
                        | DependencyFlags.DEPLOYMENT_CP
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());
        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        extD.addDependency(extC);
        install(extD, false);
        addCollectedDep(extD.getRuntime(),
                DependencyFlags.RUNTIME_CP
                        | DependencyFlags.DEPLOYMENT_CP
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extD.getDeployment());
        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        extE.addDependency(extD);
        extE.setDependencyCondition(extA);
        install(extE, false);
        addCollectedDep(extE.getRuntime(),
                DependencyFlags.RUNTIME_CP
                        | DependencyFlags.DEPLOYMENT_CP
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extE.getDeployment());
        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        extF.setConditionalDeps(extE);
        installAsDep(extF);
    }
}
