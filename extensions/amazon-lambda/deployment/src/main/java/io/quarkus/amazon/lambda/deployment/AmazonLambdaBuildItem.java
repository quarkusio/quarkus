package io.quarkus.amazon.lambda.deployment;

import org.jboss.builder.item.MultiBuildItem;

public final class AmazonLambdaBuildItem extends MultiBuildItem {

    private final String className;
    private final String path;

    public AmazonLambdaBuildItem(String className, String path) {
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
