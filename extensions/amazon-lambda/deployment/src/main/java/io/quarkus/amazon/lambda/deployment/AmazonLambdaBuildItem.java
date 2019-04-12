package io.quarkus.amazon.lambda.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class AmazonLambdaBuildItem extends MultiBuildItem {

    private final String className;

    public AmazonLambdaBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
