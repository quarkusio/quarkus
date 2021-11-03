package io.quarkus.bootstrap.resolver.replace.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 *
 * @author Alexey Loubyansky
 */
public class ManagedReplacedDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {

        // install ext 1.0.X in the repo
        final TsQuarkusExt ext100 = new TsQuarkusExt("ext1", "100");
        install(ext100, false);
        final TsQuarkusExt ext101 = new TsQuarkusExt("ext1", "101");
        install(ext101, false);
        final TsQuarkusExt ext102 = new TsQuarkusExt("ext1", "102");
        install(ext102, false);
        final TsQuarkusExt ext103 = new TsQuarkusExt("ext1", "103");
        install(ext103, true);

        // install ext 2.0.0 and add it as a direct dependency
        final TsQuarkusExt ext200 = new TsQuarkusExt("ext2", "200");
        ext200.addDependency(ext100);
        installAsDep(ext200);

        // install ext 2.0.1 and add it to the dependency management
        final TsQuarkusExt ext201 = new TsQuarkusExt("ext2", "201");
        ext201.addDependency(ext101);
        install(ext201, false);

        // install ext 3.0.0
        final TsQuarkusExt ext300 = new TsQuarkusExt("ext3", "300");
        ext300.addDependency(ext200);
        install(ext300, false);

        // install ext 3.0.1
        final TsQuarkusExt ext301 = new TsQuarkusExt("ext3", "301");
        ext301.addDependency(ext201);
        install(ext301, false);

        // add a dependency on ext3 (no version)
        root.addDependency(TsArtifact.jar(ext300.getRuntime().getArtifactId(), null));

        // the dependency management
        addManagedDep(ext103);
        addManagedDep(ext201);
        addManagedDep(ext301);

        addCollectedDep(ext301.getRuntime(), DependencyFlags.DIRECT | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(ext301.getDeployment());
    }
}
