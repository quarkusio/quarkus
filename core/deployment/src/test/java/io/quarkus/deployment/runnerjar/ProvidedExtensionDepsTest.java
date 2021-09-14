package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

public class ProvidedExtensionDepsTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

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

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA);
    }

    @Override
    protected void assertDeploymentDeps(List<AppDependency> deploymentDeps) throws Exception {
        final Set<AppDependency> expected = new HashSet<>();
        expected.add(new AppDependency(new AppArtifact("io.quarkus.bootstrap.test", "ext-a-deployment", "1"), "compile",
                AppDependency.DEPLOYMENT_CP_FLAG));
        expected.add(new AppDependency(new AppArtifact("io.quarkus.bootstrap.test", "ext-a-deployment-dep", "1"), "compile",
                AppDependency.DEPLOYMENT_CP_FLAG));
        assertEquals(expected, new HashSet<>(deploymentDeps));
    }
}
