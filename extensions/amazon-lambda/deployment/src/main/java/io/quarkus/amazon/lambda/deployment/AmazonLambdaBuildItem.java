package io.quarkus.amazon.lambda.deployment;

import org.jboss.builder.item.MultiBuildItem;

import io.quarkus.runtime.RuntimeValue;

public final class AmazonLambdaBuildItem extends MultiBuildItem {

    private final String handlerClass;
    private final RuntimeValue<Class<?>> targetType;

    public AmazonLambdaBuildItem(String handlerClass, RuntimeValue<Class<?>> targetType) {
        this.handlerClass = handlerClass;
        this.targetType = targetType;
    }

    public String getHandlerClass() {
        return handlerClass;
    }

    public RuntimeValue<Class<?>> getTargetType() {
        return targetType;
    }
}
