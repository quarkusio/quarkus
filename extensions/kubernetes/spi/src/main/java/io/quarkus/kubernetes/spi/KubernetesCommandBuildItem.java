package io.quarkus.kubernetes.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesCommandBuildItem extends SimpleBuildItem {

    private final List<String> command;
    private final List<String> args;

    public KubernetesCommandBuildItem(List<String> command, List<String> args) {
        this.command = command;
        this.args = args;
    }

    public List<String> getCommand() {
        return command;
    }

    public List<String> getArgs() {
        return args;
    }

    public static KubernetesCommandBuildItem command(String... cmd) {
        return command(Arrays.asList(cmd));
    }

    public static KubernetesCommandBuildItem command(List<String> cmd) {
        return new KubernetesCommandBuildItem(cmd, Collections.emptyList());
    }

    public static KubernetesCommandBuildItem commandWithArgs(String cmd, List<String> args) {
        return new KubernetesCommandBuildItem(Collections.singletonList(cmd), args);
    }
}
