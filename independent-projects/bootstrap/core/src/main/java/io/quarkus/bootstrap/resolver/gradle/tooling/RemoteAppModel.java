package io.quarkus.bootstrap.resolver.gradle.tooling;

import java.io.Serializable;
import java.util.List;

public interface RemoteAppModel extends Serializable {

    RemoteAppArtifact getAppArtifact();

    List<RemoteAppDependency> getUserDependencies();

    List<RemoteAppDependency> getDeploymentDependencies();
}
