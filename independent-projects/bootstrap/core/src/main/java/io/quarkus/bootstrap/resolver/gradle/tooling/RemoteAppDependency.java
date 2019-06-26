package io.quarkus.bootstrap.resolver.gradle.tooling;

import java.io.Serializable;

public interface RemoteAppDependency extends Serializable {

    RemoteAppArtifact getArtifact();

    String getScope();

    boolean isOptional();

}
