package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesCommandBuildItem extends SimpleBuildItem {

    private final String command;
    private final String[] args;

    public KubernetesCommandBuildItem(String command, String... args) {
        this.command = command;
        this.args = args;
    }

    public String getCommand() {
        return command;
    }

    public String[] getArgs() {
        return args;
    }
}
