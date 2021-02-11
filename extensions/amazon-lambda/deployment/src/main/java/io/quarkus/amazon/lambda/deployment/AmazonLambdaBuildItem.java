package io.quarkus.amazon.lambda.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class AmazonLambdaBuildItem extends MultiBuildItem {

    private final String handlerClass;
    private final String name;
    private final boolean streamHandler;

    public AmazonLambdaBuildItem(String handlerClass, String name, boolean streamHandler) {
        this.handlerClass = handlerClass;
        this.name = name;
        this.streamHandler = streamHandler;
    }

    public String getHandlerClass() {
        return handlerClass;
    }

    public String getName() {
        return name;
    }

    public boolean isStreamHandler() {
        return streamHandler;
    }
}
