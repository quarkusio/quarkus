package io.quarkus.resteasy.server.common.deployment;

import java.util.function.Consumer;

import org.jboss.resteasy.spi.ResteasyDeployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that is meant to customize the RESTEasy Deployment before it has been finalized
 * This gives extensions to ability to add stuff to the Deployment
 */
public final class ResteasyDeploymentCustomizerBuildItem extends MultiBuildItem {

    private final Consumer<ResteasyDeployment> consumer;

    public ResteasyDeploymentCustomizerBuildItem(Consumer<ResteasyDeployment> consumer) {
        this.consumer = consumer;
    }

    public Consumer<ResteasyDeployment> getConsumer() {
        return consumer;
    }
}
