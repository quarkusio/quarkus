package io.quarkus.amazon.lambda.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class AmazonLambdaBuildItem extends MultiBuildItem {

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
