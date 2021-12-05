package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RabbitMQDevServicesBuildTimeConfig {

    @ConfigGroup
    public static class Exchange {

        /**
         * Type of exchange: direct, topic, headers, fanout, etc.
         */
        @ConfigItem(defaultValue = "direct")
        public String type;

        /**
         * Should the exchange be deleted when all queues are finished using it?
         */
        @ConfigItem(defaultValue = "false")
        public Boolean autoDelete;

        /**
         * Should the exchange remain after restarts?
         */
        @ConfigItem(defaultValue = "false")
        public Boolean durable;

        /**
         * Extra arguments for the exchange definition.
         */
        @ConfigItem
        public Map<String, String> arguments;
    }

    @ConfigGroup
    public static class Queue {

        /**
         * Should the queue be deleted when all consumers are finished using it?
         */
        @ConfigItem(defaultValue = "false")
        public Boolean autoDelete;

        /**
         * Should the queue remain after restarts?
         */
        @ConfigItem(defaultValue = "false")
        public Boolean durable;

        /**
         * Extra arguments for the queue definition.
         */
        @ConfigItem
        public Map<String, String> arguments;
    }

    @ConfigGroup
    public static class Binding {

        /**
         * Source exchange to bind to. Defaults to name of binding instance.
         */
        @ConfigItem
        public Optional<String> source;

        /**
         * Routing key specification for the source exchange.
         */
        @ConfigItem(defaultValue = "#")
        public String routingKey;

        /**
         * Destination exchange or queue to bind to. Defaults to name of binding instance.
         */
        @ConfigItem
        public Optional<String> destination;

        /**
         * Destination type for binding: queue, exchange, etc.
         */
        @ConfigItem(defaultValue = "queue")
        public String destinationType;

        /**
         * Extra arguments for the binding definition.
         */
        @ConfigItem
        public Map<String, String> arguments;
    }

    /**
     * If Dev Services for RabbitMQ has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For RabbitMQ, Dev Services starts a broker unless
     * {@code rabbitmq-host} or {@code rabbitmq-port} are set or if all the Reactive Messaging RabbitMQ channel are configured
     * with
     * {@code host} or {@code port}.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> port;

    /**
     * The image to use.
     */
    @ConfigItem(defaultValue = "rabbitmq:3.9-management")
    public String imageName;

    /**
     * Indicates if the RabbitMQ broker managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for RabbitMQ starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-rabbitmq} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-rabbitmq} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for RabbitMQ looks for a container with the
     * {@code quarkus-dev-service-rabbitmq} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-rabbitmq} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared RabbitMQ brokers.
     */
    @ConfigItem(defaultValue = "rabbitmq")
    public String serviceName;

    /**
     * Exchanges that should be predefined after starting the RabbitMQ broker.
     */
    @ConfigItem
    public Map<String, Exchange> exchanges;

    /**
     * Queues that should be predefined after starting the RabbitMQ broker.
     */
    @ConfigItem
    public Map<String, Queue> queues;

    /**
     * Bindings that should be predefined after starting the RabbitMQ broker.
     */
    @ConfigItem
    public Map<String, Binding> bindings;
}
