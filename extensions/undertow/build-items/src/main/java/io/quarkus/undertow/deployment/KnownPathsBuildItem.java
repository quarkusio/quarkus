package io.quarkus.undertow.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

final class KnownPathsBuildItem extends SimpleBuildItem {

    final Set<String> knownFiles;
    final Set<String> knownDirectories;

    KnownPathsBuildItem(Set<String> knownFiles, Set<String> knownDirectories) {
        this.knownFiles = knownFiles;
        this.knownDirectories = knownDirectories;
    }
}
