package io.quarkus.bootstrap.resolver.gradle.tooling;

import io.quarkus.bootstrap.model.AppDependency;

public class DefaultRemoteAppDependency implements RemoteAppDependency {

    private RemoteAppArtifact artifact;
    private String scope;
    private boolean optional;

    public DefaultRemoteAppDependency() {
    }

    DefaultRemoteAppDependency(RemoteAppArtifact artifact, String scope, boolean optional) {
        this.artifact = artifact;
        this.scope = scope;
        this.optional = optional;
    }

    public static RemoteAppDependency from(AppDependency appDependency) {
        return new DefaultRemoteAppDependency(DefaultRemoteAppArtifact.from(appDependency.getArtifact()),
                appDependency.getScope(),
                appDependency.isOptional());
    }

    @Override
    public RemoteAppArtifact getArtifact() {
        return artifact;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }
}
