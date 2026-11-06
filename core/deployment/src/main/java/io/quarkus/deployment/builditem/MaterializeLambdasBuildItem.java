package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Asks for the lambdas and method references of a class to be rewritten at build time into ordinary generated
 * classes, so that the application does not spin hidden classes for them at startup.
 * <p>
 * A lambda costs a few hundred microseconds the first time its call site is reached, and the hidden class spun for
 * it cannot come from the AOT cache unless the training run spun it on the same thread. This is meant for the
 * classes of libraries that sit on the startup path, one class at a time; it is not a blanket rewrite of the
 * application. See {@link io.quarkus.deployment.lambda.LambdaMaterializer} for what is rewritten and what is left
 * alone.
 */
public final class MaterializeLambdasBuildItem extends MultiBuildItem {

    private final String className;

    /**
     * @param className the binary name of the class whose lambdas are rewritten, for instance
     *        {@code io.vertx.core.internal.ServiceResource$Start}
     */
    public MaterializeLambdasBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
