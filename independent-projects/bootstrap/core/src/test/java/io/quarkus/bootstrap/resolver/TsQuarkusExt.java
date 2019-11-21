package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.BootstrapConstants;

public class TsQuarkusExt {

    protected final TsArtifact runtime;
    protected final TsArtifact deployment;

    public TsQuarkusExt(String artifactId) {
        this(artifactId, TsArtifact.DEFAULT_VERSION);
    }

    public TsQuarkusExt(String artifactId, String version) {
        runtime = TsArtifact.jar(artifactId, version);
        deployment = TsArtifact.jar(artifactId + "-deployment", version);
        deployment.addDependency(runtime);
        runtime.setContent(new TsJar().addEntry(PropsBuilder.build(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment), BootstrapConstants.DESCRIPTOR_PATH));
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

    public void install(TsRepoBuilder repo) {
        repo.install(deployment);
        repo.install(runtime);
    }
}
