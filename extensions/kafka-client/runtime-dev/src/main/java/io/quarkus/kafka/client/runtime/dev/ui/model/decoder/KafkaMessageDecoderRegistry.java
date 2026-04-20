package io.quarkus.kafka.client.runtime.dev.ui.model.decoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.All;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessageContent;

/**
 * Registry that manages and coordinates all {@link KafkaMessageDecoder} implementations.
 * <p>
 * Iterates through all registered decoders in order, delegating to the first one
 * that can handle the given message bytes. Falls back to plain string decoding
 * if no decoder matches.
 * <p>
 * New decoders are automatically picked up via CDI {@code @All} injection.
 * See {@link KafkaMessageDecoder} for instructions on adding new decode strategies.
 *
 * @see KafkaMessageDecoder
 */
@ApplicationScoped
public class KafkaMessageDecoderRegistry {

    @Inject
    @All
    List<KafkaMessageDecoder> decoders;

    // For testing
    public KafkaMessageDecoderRegistry(List<KafkaMessageDecoder> decoders) {
        this.decoders = decoders;
    }

    public KafkaMessageDecoderRegistry() {
        // default constructor
    }

    /**
     * Tries to decode the given raw message bytes using the registered decoders.
     * <p>
     * If no decoder is found, it falls back to the raw message bytes as a string.
     * <p>
     * The first decoder that can decode the message is used.
     *
     * @param data the raw message bytes
     * @return the decoded message content
     */
    public KafkaMessageContent decode(String topic, byte[] data) {
        if (data == null)
            return null;

        for (KafkaMessageDecoder decoder : decoders) {
            if (decoder.canDecode(data)) {
                return decoder.decode(topic, data);
            }
        }
        return new KafkaMessageContent(new String(data, StandardCharsets.UTF_8), "STRING");
    }

}
