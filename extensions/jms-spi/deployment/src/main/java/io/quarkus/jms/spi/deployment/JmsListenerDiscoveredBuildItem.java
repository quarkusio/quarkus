package io.quarkus.jms.spi.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced by the JMS extension for each discovered {@code @JmsListener} method.
 * External extensions (e.g., IronJacamar) can consume these to provide
 * alternative listener activation strategies such as JCA message endpoints.
 *
 * @see JmsListenerManagedBuildItem
 */
public final class JmsListenerDiscoveredBuildItem extends MultiBuildItem {

    private final String beanClassName;
    private final String methodName;
    private final String destination;
    private final boolean topic;
    private final String selector;
    private final String connectionFactory;
    private final int concurrency;
    private final String subscription;
    private final String acknowledgeMode;
    private final boolean runOnVirtualThread;
    private final String parameterType;
    private final String invokerClassName;

    public JmsListenerDiscoveredBuildItem(String beanClassName, String methodName,
            String destination, boolean topic, String selector, String connectionFactory,
            int concurrency, String subscription, String acknowledgeMode,
            boolean runOnVirtualThread, String parameterType, String invokerClassName) {
        this.beanClassName = beanClassName;
        this.methodName = methodName;
        this.destination = destination;
        this.topic = topic;
        this.selector = selector;
        this.connectionFactory = connectionFactory;
        this.concurrency = concurrency;
        this.subscription = subscription;
        this.acknowledgeMode = acknowledgeMode;
        this.runOnVirtualThread = runOnVirtualThread;
        this.parameterType = parameterType;
        this.invokerClassName = invokerClassName;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDestination() {
        return destination;
    }

    public boolean isTopic() {
        return topic;
    }

    public String getSelector() {
        return selector;
    }

    public String getConnectionFactory() {
        return connectionFactory;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public String getSubscription() {
        return subscription;
    }

    public String getAcknowledgeMode() {
        return acknowledgeMode;
    }

    public boolean isRunOnVirtualThread() {
        return runOnVirtualThread;
    }

    public String getParameterType() {
        return parameterType;
    }

    public String getInvokerClassName() {
        return invokerClassName;
    }
}
