package io.quarkus.claesh.deployment;

import org.jboss.builder.item.MultiBuildItem;

public final class AeshCommandLineRunnerBuildItem extends MultiBuildItem {

    private final String className;

    public AeshCommandLineRunnerBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
