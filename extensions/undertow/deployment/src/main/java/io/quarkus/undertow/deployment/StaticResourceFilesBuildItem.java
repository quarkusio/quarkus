package io.quarkus.undertow.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class StaticResourceFilesBuildItem extends SimpleBuildItem {

    public final Set<String> files;

    StaticResourceFilesBuildItem(Set<String> files) {
        this.files = files;
    }
}
