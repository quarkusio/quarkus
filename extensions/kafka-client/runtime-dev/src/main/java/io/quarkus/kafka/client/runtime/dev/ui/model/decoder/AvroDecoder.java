package io.quarkus.kafka.client.runtime.dev.ui.model.decoder;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessageContent;

/**
 * Detects and decodes Kafka messages serialized in Avro format using a schema registry.
 * <p>
 * Avro messages are identified by the Confluent wire format, which prefixes every message
 * with a 5-byte header: a magic byte ({@code 0x00}) followed by a 4-byte schema ID.
 * The schema is then fetched from the configured registry and used to deserialize
 * the binary Avro payload into a human-readable JSON string.
 * <p>
 * Two schema registry implementations are supported:
 * <ul>
 * <li><b>Confluent Schema Registry</b> - the original registry implementation,
 * widely used in cloud and enterprise Kafka deployments.</li>
 * <li><b>Apicurio Registry</b> - an open source registry by Red Hat,
 * commonly used in OpenShift and Kubernetes environments.
 * Adopts the same wire format as Confluent for compatibility.</li>
 * </ul>
 * <p>
 * This decoder is part of the Kafka Dev UI decoder chain and is only active in dev mode.
 * It will silently skip decoding ({@link #canDecode(byte[])} returns {@code false}) if:
 * <ul>
 * <li>No schema registry URL is configured</li>
 * <li>Neither Confluent nor Apicurio serializer library is on the classpath</li>
 * <li>The message does not start with the Avro magic byte {@code 0x00}</li>
 * </ul>
 *
 * @see KafkaMessageDecoder
 * @see KafkaMessageDecoderRegistry
 */
@ApplicationScoped
public class AvroDecoder implements KafkaMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(AvroDecoder.class);

    private static final String FORMAT = "AVRO";

    private static final byte MAGIC_BYTE = 0x00;

    // 1 magic byte + 4 bytes schema ID
    private static final int HEADER_LENGTH = 5;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.apicurio.registry.url", defaultValue = "")
    String apicurioRegistryUrl;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.schema.registry.url", defaultValue = "")
    String confluentRegistryUrl;

    private Deserializer<GenericRecord> deserializer;

    // for testing
    AvroDecoder(Deserializer<GenericRecord> deserializer) {
        this.deserializer = deserializer;
    }

    public AvroDecoder() {
        // default constructor
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @PostConstruct
    void init() {
        Map<String, Object> config = new HashMap<>();
        config.put("specific.avro.reader", false); // GenericRecord, not specific class

        if (!confluentRegistryUrl.isBlank() && isClassPresent("io.confluent.kafka.serializers.KafkaAvroDeserializer")) {
            config.put("schema.registry.url", confluentRegistryUrl);
            deserializer = createConfluentDeserializer(config);
        } else if (!apicurioRegistryUrl.isBlank() && isClassPresent("io.apicurio.registry.serde.avro.AvroKafkaDeserializer")) {
            config.put("apicurio.registry.url", apicurioRegistryUrl);
            deserializer = createApicurioDeserializer(config);
        }
    }

    @Override
    public boolean canDecode(byte[] data) {
        return deserializer != null
                && data != null
                && data.length > HEADER_LENGTH
                && data[0] == MAGIC_BYTE;
    }

    @Override
    public KafkaMessageContent decode(String topic, byte[] data) {
        GenericRecord genericRecord = deserializer.deserialize(topic, data);
        return new KafkaMessageContent(genericRecord.toString(), FORMAT);
    }

    private Deserializer<GenericRecord> createApicurioDeserializer(Map<String, Object> config) {
        return createDeserializer(config, "io.apicurio.registry.serde.avro.AvroKafkaDeserializer");
    }

    private Deserializer<GenericRecord> createConfluentDeserializer(Map<String, Object> config) {
        return createDeserializer(config, "io.confluent.kafka.serializers.KafkaAvroDeserializer");
    }

    @SuppressWarnings("unchecked")
    private Deserializer<GenericRecord> createDeserializer(Map<String, Object> config, String className) {
        try {
            Deserializer<GenericRecord> d = (Deserializer<GenericRecord>) Class
                    .forName(className)
                    .getDeclaredConstructor()
                    .newInstance();
            d.configure(config, false);
            return d;
        } catch (Exception e) {
            log.error("Failed to create Deserializer for class {}", className, e);
            return null;
        }
    }

}
