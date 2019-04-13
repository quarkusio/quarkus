package io.quarkus.amazon.lambda.deployment;

import org.jboss.builder.item.MultiBuildItem;

public final class AmazonLambdaClassNameBuildItem extends MultiBuildItem {

    private final String className;

    public AmazonLambdaClassNameBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "AmazonLambdaBuildItem{" +
                "className='" + className + '\'' +
                '}';
    }
}
