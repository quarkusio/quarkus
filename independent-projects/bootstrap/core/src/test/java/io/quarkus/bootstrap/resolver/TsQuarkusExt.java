package io.quarkus.bootstrap.resolver;

import java.io.IOException;

import io.quarkus.bootstrap.BootstrapConstants;

public class TsQuarkusExt {

    protected final TsArtifact runtime;
    protected final TsArtifact deployment;

    public TsQuarkusExt(String artifactId) {
        runtime = TsArtifact.jar(artifactId);
        deployment = TsArtifact.jar(artifactId + "-deployment");
        deployment.addDependency(runtime);
        runtime.setContent(new TsJar().addFile(PropsBuilder.build(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment), BootstrapConstants.DESCRIPTOR_PATH));
    }

    public TsArtifact getRuntime() {
        return runtime;
    }

    public TsArtifact getDeployment() {
        return deployment;
    }

    public TsQuarkusExt addDependency(TsQuarkusExt ext) {
        runtime.addDependency(ext.runtime);
        deployment.addDependency(ext.deployment);
        return this;
    }

    public void install(TsRepoBuilder repo) throws IOException {
        repo.install(deployment);
        repo.install(runtime);
    }
}
