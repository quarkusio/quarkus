package io.quarkus.clrunner.deployment;

import org.jboss.builder.item.SimpleBuildItem;

public final class CommandLineRunnerBuildItem extends SimpleBuildItem {

    private final String className;

    public CommandLineRunnerBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
