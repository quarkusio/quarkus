package io.quarkus.bootstrap.resolver.test;

import java.util.Map;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 */
public class DefaultCapabilityProviderGraphPositionTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {

        // A is a direct application dependency
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");

        // B is just a dependency of A
        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extA.addDependency(extB);

        // C is a dependency of A requiring cap.x
        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setRequiresCapabilities("cap.x");
        extA.addDependency(extC);

        // D is a direct application dependency
        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");

        // E requires cap.y
        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        extE.setRequiresCapabilities("cap.y");
        extD.addDependency(extE);

        // F is the default provider of cap.x and depends on G
        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        extF.setProvidesCapabilities("cap.x");

        // G provides cap.y
        final TsQuarkusExt extG = new TsQuarkusExt("ext-g");
        extG.setProvidesCapabilities("cap.y");
        extF.addDependency(extG);

        // H is the default provider of cap.y and depends on I
        final TsQuarkusExt extH = new TsQuarkusExt("ext-h");
        extH.setProvidesCapabilities("cap.y");

        // I provides cap.x
        final TsQuarkusExt extI = new TsQuarkusExt("ext-i");
        extI.setProvidesCapabilities("cap.x");
        extH.addDependency(extI);

        install(extA, false);
        install(extB, false);
        install(extC, false);
        install(extD, false);
        install(extE, false);
        install(extF, false);
        install(extG, false);
        install(extH, false);
        install(extI, false);

        root.addDependency(extA);
        root.addDependency(extD);

        addCollectedDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extA.getDeployment());

        addCollectedDep(extB.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());

        addCollectedDep(extC.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        addCollectedDep(extD.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extD.getDeployment());

        addCollectedDep(extE.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extE.getDeployment());

        installDefaultCapabilityProviders(Map.of(
                "cap.x", extF,
                "cap.y", extH));

        addCollectedDep(extF.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extF.getDeployment());

        addCollectedDep(extG.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extG.getDeployment());

        installPlatformDescriptor();
    }
}
