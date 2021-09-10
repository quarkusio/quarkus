package io.quarkus.deployment.runnerjar;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

public class BasicExecutableOutputOutcomeTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {
        final TsQuarkusExt coreExt = new TsQuarkusExt("core-ext");
        addToExpectedLib(coreExt.getRuntime());

        final TsQuarkusExt ext1 = new TsQuarkusExt("ext1");
        ext1.addDependency(coreExt);
        addToExpectedLib(ext1.getRuntime());

        final TsQuarkusExt ext2 = new TsQuarkusExt("ext2");
        addToExpectedLib(ext2.getRuntime());

        final TsArtifact transitiveDep1 = TsArtifact.jar("transitive1");
        addToExpectedLib(transitiveDep1);

        final TsArtifact optionalTransitiveDep = TsArtifact.jar("optional-transitive-dep");

        final TsArtifact compileDep = TsArtifact.jar("compile-dep")
                .addDependency(transitiveDep1)
                .addDependency(new TsDependency(optionalTransitiveDep, true));
        addToExpectedLib(compileDep);

        final TsArtifact providedDep = TsArtifact.jar("provided-dep");

        final TsArtifact optionalDep = TsArtifact.jar("optional-dep");
        addToExpectedLib(optionalDep);

        final TsArtifact directRtDep = TsArtifact.jar("runtime-dep");
        addToExpectedLib(directRtDep);

        final TsArtifact appJar = TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(ext1)
                .addDependency(ext2)
                .addDependency(compileDep)
                .addDependency(new TsDependency(providedDep, "provided"))
                .addDependency(new TsDependency(optionalDep, true))
                .addDependency(new TsDependency(directRtDep, "runtime"));
        return appJar;
    }
}
