package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;

public class OptionalDepsTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsArtifact extADep = TsArtifact.jar("ext-a-dep");
        addToExpectedLib(extADep);

        final TsArtifact extAOptionalDep = TsArtifact.jar("ext-a-optional-dep");
        final TsArtifact extAOptionalDeploymentDep = TsArtifact.jar("ext-a-optional-deployment-dep");

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        addToExpectedLib(extA.getRuntime());
        extA.getRuntime()
                .addDependency(extADep)
                .addDependency(new TsDependency(extAOptionalDep, true));
        extA.getDeployment()
                .addDependency(new TsDependency(extAOptionalDeploymentDep, true));

        final TsArtifact extBOptionalDep = TsArtifact.jar("ext-b-optional-dep");
        final TsArtifact extBDeploymentDep = TsArtifact.jar("ext-b-deployment-dep");
        install(extBDeploymentDep);
        final TsArtifact extBOptionalDeploymentDep = TsArtifact.jar("ext-b-optional-deployment-dep");
        install(extBOptionalDeploymentDep);

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.getRuntime().addDependency(new TsDependency(extBOptionalDep, true));
        addToExpectedLib(extB.getRuntime());
        extB.getDeployment().addDependency(new TsDependency(extBOptionalDeploymentDep, true));
        extB.getDeployment().addDependency(new TsDependency(extBDeploymentDep, false));
        install(extB);

        final TsArtifact appOptionalDep = TsArtifact.jar("app-optional-dep")
                .addDependency(extB.getRuntime());
        addToExpectedLib(appOptionalDep);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        install(extC);

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        extD.getRuntime().addDependency(new TsDependency(extC.getRuntime(), true));
        extD.getDeployment().addDependency(new TsDependency(extC.getDeployment(), true));
        install(extD);
        addToExpectedLib(extD.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA, true)
                .addDependency(new TsDependency(appOptionalDep, true))
                .addDependency(extD.getRuntime());
    }

    @Override
    protected void assertDeploymentDeps(Set<Dependency> deploymentDeps) throws Exception {
        final Set<Dependency> expected = new HashSet<>();
        expected.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-a-deployment", "1"), "compile",
                DependencyFlags.OPTIONAL,
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-b-deployment-dep", "1"), "compile",
                DependencyFlags.OPTIONAL,
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-b-deployment", "1"), "compile",
                DependencyFlags.OPTIONAL,
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(new GACTV("io.quarkus.bootstrap.test", "ext-d-deployment", "1"), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, deploymentDeps);
    }
}
