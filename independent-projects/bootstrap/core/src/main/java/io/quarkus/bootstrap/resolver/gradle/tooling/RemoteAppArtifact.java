package io.quarkus.bootstrap.resolver.gradle.tooling;

import java.io.Serializable;

public interface RemoteAppArtifact extends Serializable {

    String getGroupId();

    String getArtifactId();

    String getClassifier();

    String getPath();

    String getType();

    String getVersion();

}
