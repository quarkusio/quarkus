package io.quarkus.bootstrap.model.gradle;

import java.io.File;
import java.util.Set;

public interface Dependency {

    String getName();

    String getGroupId();

    String getVersion();

    String getClassifier();

    Set<File> getPaths();

    String getType();

    String getScope();

    int getFlags();
}
