package io.quarkus.maven;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

abstract class BasicDependencyTreeTestBase extends DependencyTreeMojoTestBase {

    @Override
    protected void initRepo() {

        final TsQuarkusExt coreExt = new TsQuarkusExt("test-core-ext");
        app = TsArtifact.jar("test-app")
                .addDependency(new TsArtifact(TsArtifact.DEFAULT_GROUP_ID, "artifact-with-classifier", "classifier", "jar",
                        TsArtifact.DEFAULT_VERSION))
                .addDependency(new TsQuarkusExt("test-ext2")
                        .addDependency(new TsQuarkusExt("test-ext1").addDependency(coreExt)))
                .addDependency(new TsDependency(TsArtifact.jar("optional"), true))
                .addDependency(new TsQuarkusExt("test-ext3").addDependency(coreExt))
                .addDependency(new TsDependency(TsArtifact.jar("provided"), "provided"))
                .addDependency(new TsDependency(TsArtifact.jar("runtime"), "runtime"))
                .addDependency(new TsDependency(TsArtifact.jar("test"), "test"));
        appModel = app.getPomModel();
        app.install(repoBuilder);
    }
}
