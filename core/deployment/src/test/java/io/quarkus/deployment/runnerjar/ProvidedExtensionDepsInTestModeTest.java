package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;

public class ProvidedExtensionDepsInTestModeTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected boolean isBootstrapForTestMode() {
        return true;
    }

    @Override
    protected TsArtifact composeApplication() {

        final TsArtifact extADep = TsArtifact.jar("ext-a-dep");
        addToExpectedLib(extADep);

        final TsArtifact extAProvidedDep = TsArtifact.jar("ext-a-provided-dep");

        final TsArtifact extADeploymentDep = TsArtifact.jar("ext-a-deployment-dep");
        final TsArtifact extAOptionalDeploymentDep = TsArtifact.jar("ext-a-provided-deployment-dep");

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        addToExpectedLib(extA.getRuntime());
        extA.getRuntime()
                .addDependency(new TsDependency(extADep))
                .addDependency(new TsDependency(extAProvidedDep, "provided"));
        extA.getDeployment()
                .addDependency(new TsDependency(extADeploymentDep))
                .addDependency(new TsDependency(extAOptionalDeploymentDep, "provided"));

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        addToExpectedLib(extB.getRuntime());
        this.install(extB);

        final TsArtifact someProvidedDep = TsArtifact.jar("some-provided-dep");
        addToExpectedLib(someProvidedDep);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA)
                .addDependency(extB, "provided")
                .addDependency(new TsDependency(someProvidedDep, "provided"));
    }

    @Override
    protected void assertAppModel(ApplicationModel model) throws Exception {
        final Set<Dependency> expected = new HashSet<>();
        expected.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a-deployment", "1"), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a-deployment-dep", "1"), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-b-deployment", "1"), "provided",
                DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, getDeploymentOnlyDeps(model));
    }
}
