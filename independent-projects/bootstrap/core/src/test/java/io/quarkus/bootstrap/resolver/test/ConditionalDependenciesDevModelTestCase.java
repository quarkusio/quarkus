package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.maven.dependency.DependencyFlags;

public class ConditionalDependenciesDevModelTestCase extends CollectDependenciesBase {

    @Override
    protected BootstrapAppModelResolver newAppModelResolver(LocalProject currentProject) throws Exception {
        var resolver = super.newAppModelResolver(currentProject);
        resolver.setIncubatingModelResolver(false);
        return resolver;
    }

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
        extC.setDependencyCondition(extB);
        install(extC, false);
        addCollectedDep(extC.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        install(extD, false);
        installAsDep(extD.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extD.getDeployment());

        final TsArtifact libE = TsArtifact.jar("lib-e");
        install(libE, true);
        final TsArtifact libEBuildTIme = TsArtifact.jar("lib-e-build-time");
        install(libEBuildTIme);
        addCollectedDeploymentDep(libEBuildTIme);

        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        extE.setDependencyCondition(extD);
        extE.getRuntime().addDependency(libE);
        extE.getDeployment().addDependency(libEBuildTIme);
        install(extE, false);
        addCollectedDep(extE.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extE.getDeployment());

        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        extF.setConditionalDeps(extC, extE);
        install(extF, false);
        installAsDep(extF.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extF.getDeployment());

        final TsQuarkusExt extG = new TsQuarkusExt("ext-g");
        extG.setConditionalDevDeps(extB);
        install(extG, false);
        installAsDep(extG.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extG.getDeployment());
    }
}
