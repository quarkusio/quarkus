package io.quarkus.grpc.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class TranscodingServiceClassesBuildItem extends SimpleBuildItem {

    private final Set<String> classes;

    public TranscodingServiceClassesBuildItem(Set<String> classes) {
        this.classes = classes;
    }

    public Set<String> getClasses() {
        return classes;
    }
}
