package io.quarkus.kafka.client.runtime.dev.ui.model.decoder;

import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessageContent;

/**
 * Strategy interface for decoding raw Kafka message bytes into a human-readable representation.
 * <p>
 * Each decoder is responsible for detecting whether it
 * can handle a given message and decoding it accordingly.
 * <p>
 * To add a new decoding strategy, implement this interface and annotate the implementation
 * with {@code @ApplicationScoped}. The implementation should be registered in
 * {@link io.quarkus.kafka.client.deployment.KafkaProcessor#kafkaClientBeans()} too.
 * <p>
 * Implementations should be stateless or thread-safe, as a single instance is shared
 * across all {@code decode} calls in the Dev UI.
 *
 * @see KafkaMessageDecoderRegistry
 */
public interface KafkaMessageDecoder {

    /**
     * Returns {@code true} if this decoder can handle the given raw message bytes.
     *
     * @param data the raw message bytes
     * @return true if this decoder can handle the given raw message bytes, false otherwise
     */
    boolean canDecode(byte[] data);

    /**
     * Decodes the raw message bytes into a human-readable string representation.
     * Only called when {@link #canDecode(byte[])} returns {@code true}.
     *
     * @param topic the Kafka topic the message was sent to
     * @param data the raw message bytes
     * @return the human-readable string representation of the message
     */
    KafkaMessageContent decode(String topic, byte[] data);

}
