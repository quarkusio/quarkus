package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.deployment.ClassOutput;

public final class ClassOutputBuildItem extends SimpleBuildItem {

    private final ClassOutput classOutput;

    public ClassOutputBuildItem(ClassOutput classOutput) {
        this.classOutput = classOutput;
    }

    public ClassOutput getClassOutput() {
        return classOutput;
    }
}
