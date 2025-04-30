package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface RabbitMQDevServicesBuildTimeConfig {

    @ConfigGroup
    public interface Exchange {

        /**
         * Type of exchange: direct, topic, headers, fanout, etc.
         */
        @WithDefault("direct")
        String type();

        /**
         * Should the exchange be deleted when all queues are finished using it?
         */
        @WithDefault("false")
        Boolean autoDelete();

        /**
         * Should the exchange remain after restarts?
         */
        @WithDefault("false")
        Boolean durable();

        /**
         * What virtual host should the exchange be associated with?
         */
        @WithDefault("/")
        String vhost();

        /**
         * Extra arguments for the exchange definition.
         */
        @ConfigDocMapKey("argument-name")
        Map<String, String> arguments();
    }

    @ConfigGroup
    public interface Queue {

        /**
         * Should the queue be deleted when all consumers are finished using it?
         */
        @WithDefault("false")
        Boolean autoDelete();

        /**
         * Should the queue remain after restarts?
         */
        @WithDefault("false")
        Boolean durable();

        /**
         * What virtual host should the queue be associated with?
         */
        @WithDefault("/")
        String vhost();

        /**
         * Extra arguments for the queue definition.
         */
        @ConfigDocMapKey("argument-name")
        Map<String, String> arguments();
    }

    @ConfigGroup
    public interface Binding {

        /**
         * Source exchange to bind to. Defaults to name of binding instance.
         */
        Optional<String> source();

        /**
         * Routing key specification for the source exchange.
         */
        @WithDefault("#")
        String routingKey();

        /**
         * Destination exchange or queue to bind to. Defaults to name of binding instance.
         */
        Optional<String> destination();

        /**
         * Destination type for binding: queue, exchange, etc.
         */
        @WithDefault("queue")
        String destinationType();

        /**
         * What virtual host should the binding be associated with?
         */
        @WithDefault("/")
        String vhost();

        /**
         * Extra arguments for the binding definition.
         */
        @ConfigDocMapKey("argument-name")
        Map<String, String> arguments();
    }

    /**
     * If Dev Services for RabbitMQ has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For RabbitMQ, Dev Services starts a broker unless
     * {@code rabbitmq-host} or {@code rabbitmq-port} are set or if all the Reactive Messaging RabbitMQ channel are configured
     * with
     * {@code host} or {@code port}.
     */
    Optional<Boolean> enabled();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    OptionalInt port();

    /**
     * Optional fixed port for the RabbitMQ management plugin.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    OptionalInt httpPort();

    /**
     * The image to use.
     * Note that only official RabbitMQ images are supported.
     * Specifically, the image repository must end with {@code rabbitmq}.
     */
    @WithDefault("rabbitmq:3.12-management")
    String imageName();

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
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-rabbitmq} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for RabbitMQ looks for a container with the
     * {@code quarkus-dev-service-rabbitmq} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-rabbitmq} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared RabbitMQ brokers.
     */
    @WithDefault("rabbitmq")
    String serviceName();

    /**
     * Exchanges that should be predefined after starting the RabbitMQ broker.
     */
    @ConfigDocMapKey("exchange-name")
    Map<String, Exchange> exchanges();

    /**
     * Queues that should be predefined after starting the RabbitMQ broker.
     */
    @ConfigDocMapKey("queue-name")
    Map<String, Queue> queues();

    /**
     * Bindings that should be predefined after starting the RabbitMQ broker.
     */
    @ConfigDocMapKey("binding-name")
    Map<String, Binding> bindings();

    /**
     * Virtual hosts that should be predefined after starting the RabbitMQ broker.
     */
    Optional<List<String>> vhosts();

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> containerEnv();
}
