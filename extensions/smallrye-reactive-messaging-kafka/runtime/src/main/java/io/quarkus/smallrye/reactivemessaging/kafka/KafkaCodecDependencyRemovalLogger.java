package io.quarkus.smallrye.reactivemessaging.kafka;

import org.jboss.logging.Logger;

public class KafkaCodecDependencyRemovalLogger {

    private static final Logger LOGGER = Logger.getLogger(KafkaCodecDependencyRemovalLogger.class.getName());

    private static final String TARGET_SERDE_PACKAGE = "io.vertx.kafka.client.serialization";
    private static final String REPLACEMENT_SERDE_PACKAGE = "io.quarkus.kafka.client.serialization";

    public static void logDependencyRemoval(String deprecatedClassName) {
        LOGGER.warnf("Dependency to be removed: The Serde class `%s` will no longer be included " +
                "in the classpath of the Smallrye Reactive Messaging Kafka extension. " +
                "Consider replacing it's usage with `%s` provided by the Kafka extension " +
                "or including the Vert.x Kafka client dependency (io.vertx:vertx-kafka-client) yourself.",
                deprecatedClassName, deprecatedClassName.replace(TARGET_SERDE_PACKAGE, REPLACEMENT_SERDE_PACKAGE));
    }

}
