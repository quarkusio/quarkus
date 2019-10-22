package io.quarkus.amazon.lambda.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class AmazonLambdaBuildItem extends SimpleBuildItem {

    private final String handlerClass;
    private final String name;

    public AmazonLambdaBuildItem(String handlerClass, String name) {
        this.handlerClass = handlerClass;
        this.name = name;
    }

    public String getHandlerClass() {
        return handlerClass;
    }

    public String getName() {
        return name;
    }
}
