package io.quarkus.jms.spi.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced by external extensions (e.g., IronJacamar) to signal that they
 * manage the lifecycle of a specific {@code @io.quarkiverse.jms.JmsListener} method. When
 * produced, the default {@code io.quarkiverse.jms.JmsListenerRegistry} will skip starting
 * a polling consumer for this listener.
 *
 * @see JmsListenerDiscoveredBuildItem
 */
public final class JmsListenerManagedBuildItem extends MultiBuildItem {

    private final String beanClassName;
    private final String methodName;

    public JmsListenerManagedBuildItem(String beanClassName, String methodName) {
        this.beanClassName = beanClassName;
        this.methodName = methodName;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public String getMethodName() {
        return methodName;
    }
}
