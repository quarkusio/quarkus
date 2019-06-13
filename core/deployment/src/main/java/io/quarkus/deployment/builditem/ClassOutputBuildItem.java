package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ClassOutput;

public final class ClassOutputBuildItem extends SimpleBuildItem {

    private final ClassOutput classOutput;

    public ClassOutputBuildItem(ClassOutput classOutput) {
        this.classOutput = classOutput;
    }

    public ClassOutput getClassOutput() {
        return classOutput;
    }
}
