package io.quarkus.kafka.client.runtime.dev.ui.model.response;

/**
 * Represents the decoded content of a Kafka message value.
 *
 * @param value the human-readable representation of the message bytes.
 * @param format the format of the decoded value, indicating how it was decoded.
 *        For example {@code "AVRO"}, {@code "STRING"}.
 *        New formats can be introduced by implementing
 *        {@link io.quarkus.kafka.client.runtime.dev.ui.model.decoder.KafkaMessageDecoder}.
 */
public record KafkaMessageContent(String value, String format) {
}
