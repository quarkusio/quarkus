package io.quarkus.smallrye.reactivemessaging.mqtt.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class MqttDevServicesBuildTimeConfig {

    /**
     * If Dev Services for MQTT has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For MQTT, Dev Services starts a broker unless
     * {@code *.host} or {@code *.port} are set for one of the connectors or if all the Reactive Messaging MQTT channel are
     * configured
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
    public OptionalInt port;

    /**
     * The image to use.
     */
    @ConfigItem(defaultValue = "eclipse-mosquitto:2.0.15")
    public String imageName;

    /**
     * Indicates if the MQTT broker managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for MQTT starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-mqtt} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-mqtt} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for MQTT looks for a container with the
     * {@code quarkus-dev-service-mqtt} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-mqtt} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared MQTT brokers.
     */
    @ConfigItem(defaultValue = "mqtt")
    public String serviceName;

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigItem
    @ConfigDocMapKey("environment-variable-name")
    public Map<String, String> containerEnv;
}
