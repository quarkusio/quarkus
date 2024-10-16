package io.quarkus.vertx.deployment.spi;

import java.util.function.BiConsumer;

import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.InvokerBuilder;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item should be considered private and should not be used outside of core Quarkus.
 * It can be changed without notice.
 */
public final class EventConsumerInvokerCustomizerBuildItem extends MultiBuildItem {
    private final BiConsumer<MethodInfo, InvokerBuilder> invokerCustomizer;

    public EventConsumerInvokerCustomizerBuildItem(BiConsumer<MethodInfo, InvokerBuilder> invokerCustomizer) {
        this.invokerCustomizer = invokerCustomizer;
    }

    public BiConsumer<MethodInfo, InvokerBuilder> getInvokerCustomizer() {
        return invokerCustomizer;
    }
}
