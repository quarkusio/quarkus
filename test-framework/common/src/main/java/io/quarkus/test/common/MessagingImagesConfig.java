package io.quarkus.test.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for messaging system container images used in Quarkus tests.
 */
@ConfigMapping(prefix = "quarkus.container.image.messaging")
public interface MessagingImagesConfig {


    @WithName("registry")
    @WithDefault("docker.io")
    String registry();


    @WithName("kafka")
    @WithDefault("confluentinc/cp-kafka:7.3.0")
    String kafkaImage();


    @WithName("rabbitmq")
    @WithDefault("rabbitmq:3.12-management")
    String rabbitmqImage();


    @WithName("amqp")
    @WithDefault("vromero/activemq-artemis:2.16.0")
    String amqpImage();


    @WithName("pulsar")
    @WithDefault("apachepulsar/pulsar:2.10.2")
    String pulsarImage();


    @WithName("redpanda")
    @WithDefault("vectorized/redpanda:latest")
    String redpandaImage();


    default String getKafkaFullImage() {
        return registry() + "/" + kafkaImage();
    }


    default String getRabbitMQFullImage() {
        return registry() + "/" + rabbitmqImage();
    }

}