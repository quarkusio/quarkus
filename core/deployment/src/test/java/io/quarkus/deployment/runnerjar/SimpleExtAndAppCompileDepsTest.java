package io.quarkus.deployment.runnerjar;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

public class SimpleExtAndAppCompileDepsTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        final TsArtifact coreExtRtTransitiveDep = TsArtifact.jar("core-ext-rt-transitive-dep");
        addToExpectedLib(coreExtRtTransitiveDep);

        final TsArtifact coreExtRtDirectDep = TsArtifact.jar("core-ext-rt-direct-dep").addDependency(coreExtRtTransitiveDep);
        addToExpectedLib(coreExtRtDirectDep);

        final TsArtifact coreExtDepTransitiveDep = TsArtifact.jar("core-ext-dep-transitive-dep");

        final TsArtifact coreExtDepDirectDep = TsArtifact.jar("core-ext-dep-direct-dep").addDependency(coreExtDepTransitiveDep);

        final TsQuarkusExt coreExt = new TsQuarkusExt("core-ext");
        coreExt.getRuntime().addDependency(coreExtRtDirectDep);
        coreExt.getDeployment().addDependency(coreExtDepDirectDep);
        addToExpectedLib(coreExt.getRuntime());

        final TsArtifact ext1RtTransitiveDep = TsArtifact.jar("ext1-rt-transitive-dep");
        addToExpectedLib(ext1RtTransitiveDep);

        final TsArtifact ext1RtDirectDep = TsArtifact.jar("ext1-rt-direct-dep").addDependency(ext1RtTransitiveDep);
        addToExpectedLib(ext1RtDirectDep);

        final TsArtifact ext1DepTransitiveDep = TsArtifact.jar("ext1-dep-transitive-dep");

        final TsArtifact ext1DepDirectDep = TsArtifact.jar("ext1-dep-direct-dep").addDependency(ext1DepTransitiveDep);

        final TsQuarkusExt ext1 = new TsQuarkusExt("ext1");
        ext1.addDependency(coreExt);
        ext1.getRuntime().addDependency(ext1RtDirectDep);
        ext1.getDeployment().addDependency(ext1DepDirectDep);
        addToExpectedLib(ext1.getRuntime());

        final TsQuarkusExt ext2 = new TsQuarkusExt("ext2");
        addToExpectedLib(ext2.getRuntime());

        final TsArtifact appTransitiveDep = TsArtifact.jar("app-transitive-dep");
        addToExpectedLib(appTransitiveDep);

        final TsArtifact appDirectDep = TsArtifact.jar("app-direct-dep").addDependency(appTransitiveDep);
        addToExpectedLib(appDirectDep);

        final TsArtifact appJar = TsArtifact.jar("app")
                .addDependency(ext1)
                .addDependency(ext2)
                .addDependency(appDirectDep);
        return appJar;
    }
}
