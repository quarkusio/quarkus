package io.quarkus.clrunner;

import org.jboss.builder.item.MultiBuildItem;

public final class CommandLineRunnerBuildItem extends MultiBuildItem {

    private final String className;

    public CommandLineRunnerBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
