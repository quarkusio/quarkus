package io.quarkus.undertow.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

final public class KnownPathsBuildItem extends SimpleBuildItem {

    final public Set<String> knownFiles;
    final public Set<String> knownDirectories;

    KnownPathsBuildItem(Set<String> knownFiles, Set<String> knownDirectories) {
        this.knownFiles = knownFiles;
        this.knownDirectories = knownDirectories;
    }
}
