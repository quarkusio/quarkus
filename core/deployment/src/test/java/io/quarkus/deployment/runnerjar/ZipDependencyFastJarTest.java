package io.quarkus.deployment.runnerjar;

import io.quarkus.bootstrap.resolver.TsArtifact;

public class ZipDependencyFastJarTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {
        final TsArtifact zipDep = TsArtifact.zip("zip-dep");
        addToExpectedLib(zipDep);

        final TsArtifact jarDep = TsArtifact.jar("jar-dep");
        addToExpectedLib(jarDep);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(zipDep)
                .addDependency(jarDep);
    }
}
