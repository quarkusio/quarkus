package io.quarkus.amazon.lambda.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Handler provided by another extension i.e. AWS Lambda HTTP
 *
 */
public final class ProvidedAmazonLambdaHandlerBuildItem extends SimpleBuildItem {

    private final Class handlerClass;
    private final String provider;

    public ProvidedAmazonLambdaHandlerBuildItem(Class handlerClass, String provider) {
        this.handlerClass = handlerClass;
        this.provider = provider;
    }

    public Class getHandlerClass() {
        return handlerClass;
    }

    /**
     * Just used for error logging purposes. Name of your extension i.e. "AWS Lambda HTTP".
     * 
     * @return
     */
    public String getProvider() {
        return provider;
    }
}
