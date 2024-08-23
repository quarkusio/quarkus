package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to register a lambda capturing type in native mode
 */
public final class LambdaCapturingTypeBuildItem extends MultiBuildItem {

    private final String className;

    public LambdaCapturingTypeBuildItem(Class<?> lambdaCapturingType) {
        this.className = lambdaCapturingType.getName();
    }

    public LambdaCapturingTypeBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
