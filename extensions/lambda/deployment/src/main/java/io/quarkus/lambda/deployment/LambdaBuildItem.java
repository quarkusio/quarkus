package io.quarkus.lambda.deployment;

import org.jboss.builder.item.MultiBuildItem;

public final class LambdaBuildItem extends MultiBuildItem {

    private final String className;
    private final String path;

    public LambdaBuildItem(String className, String path) {
        this.className = className;
        this.path = path;
    }

    public String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }
}
