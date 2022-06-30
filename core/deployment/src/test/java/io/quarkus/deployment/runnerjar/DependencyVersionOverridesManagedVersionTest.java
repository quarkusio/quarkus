package io.quarkus.deployment.runnerjar;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

public class DependencyVersionOverridesManagedVersionTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected boolean createWorkspace() {
        return true;
    }

    @Override
    protected TsArtifact composeApplication() {

        final TsQuarkusExt extA_100 = new TsQuarkusExt("ext-a", "1.0.0");
        install(extA_100);

        final TsQuarkusExt extB_100 = new TsQuarkusExt("ext-b", "1.0.0");
        install(extB_100);
        final TsArtifact extB_100_rt = extB_100.getRuntime();
        addToExpectedLib(extB_100_rt);

        final TsArtifact bom = new TsArtifact("test.quarkus", "test-bom", null, "pom", "1.0.0");
        bom.addManagedDependency(platformDescriptor());
        bom.addManagedDependency(platformProperties());
        bom.addManagedDependency(new TsDependency(extA_100.getRuntime()));
        bom.addManagedDependency(new TsDependency(extB_100_rt));
        install(bom);

        final TsQuarkusExt extA_101 = new TsQuarkusExt("ext-a", "1.0.1");
        install(extA_101);
        addToExpectedLib(extA_101.getRuntime());

        createWorkspace();

        final TsArtifact appJar = TsArtifact.jar("app")
                .addManagedDependency(new TsDependency(bom, "import"))
                .addDependency(new TsDependency(new TsArtifact(extB_100_rt.getGroupId(), extB_100_rt.getArtifactId(),
                        extB_100_rt.getClassifier(), extB_100_rt.getType(), null)))
                .addDependency(extA_101.getRuntime());

        return appJar;
    }
}
