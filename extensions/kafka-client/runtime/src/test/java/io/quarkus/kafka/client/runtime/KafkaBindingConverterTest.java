package io.quarkus.kafka.client.runtime;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaBindingConverterTest {

    private final static Path rootPath = Paths.get("src/test/resources/service-binding");
    private final static String BINDING_DIRECTORY_NOT_KAFKA = "not-kafka";
    private final static String BINDING_DIRECTORY_KAFKA_BOOTSTRAP_CAMELCASE = "kafka-bootstrap-camelcase";
    private final static String BINDING_DIRECTORY_KAFKA_BOOTSTRAP_WITH_HYPHEN = "kafka-bootstrap-with-hyphen";
    private final static String BINDING_DIRECTORY_KAFKA_SASL_PLAIN = "kafka-sasl-plain";

    private final KafkaBindingConverter converter = new KafkaBindingConverter();

    @Test
    public void testNoKafkaServiceBinding() {
        ServiceBinding fooServiceBinding = new ServiceBinding(rootPath.resolve(BINDING_DIRECTORY_NOT_KAFKA));
        Optional<ServiceBindingConfigSource> configSourceOptional = converter.convert(List.of(fooServiceBinding));
        assertThat(configSourceOptional).isEmpty();
    }

    @Test
    public void testKafkaBootstrapWithCamelCaseServiceBinding() {
        ServiceBinding kafkaServiceBinding = new ServiceBinding(rootPath.resolve(BINDING_DIRECTORY_KAFKA_BOOTSTRAP_CAMELCASE));
        Optional<ServiceBindingConfigSource> configSourceOptional = converter.convert(List.of(kafkaServiceBinding));
        assertThat(configSourceOptional).isPresent();
        Map<String, String> properties = configSourceOptional.get().getProperties();
        assertThat(properties.get("kafka.bootstrap.servers")).isEqualTo("localhost:9092");
        assertThat(properties.get("kafka.security.protocol")).isEqualTo("PLAINTEXT");
    }

    @Test
    public void testKafkaBootstrapWithHyphenServiceBinding() {
        ServiceBinding kafkaServiceBinding = new ServiceBinding(rootPath.resolve(BINDING_DIRECTORY_KAFKA_BOOTSTRAP_WITH_HYPHEN));
        Optional<ServiceBindingConfigSource> configSourceOptional = converter.convert(List.of(kafkaServiceBinding));
        assertThat(configSourceOptional).isPresent();
        Map<String, String> properties = configSourceOptional.get().getProperties();
        assertThat(properties.get("kafka.bootstrap.servers")).isEqualTo("localhost:9093");
        assertThat(properties.get("kafka.security.protocol")).isEqualTo("PLAINTEXT");
    }

    @Test
    public void testKafkaSaslPlainServiceBinding() {
        ServiceBinding kafkaServiceBinding = new ServiceBinding(rootPath.resolve(BINDING_DIRECTORY_KAFKA_SASL_PLAIN));
        Optional<ServiceBindingConfigSource> configSourceOptional = converter.convert(List.of(kafkaServiceBinding));
        assertThat(configSourceOptional).isPresent();
        Map<String, String> properties = configSourceOptional.get().getProperties();
        assertThat(properties.get("kafka.bootstrap.servers")).isEqualTo("localhost:9092");
        assertThat(properties.get("kafka.security.protocol")).isEqualTo("SASL_PLAINTEXT");
        assertThat(properties.get("kafka.sasl.mechanism")).isEqualTo("PLAIN");
        assertThat(properties.get("kafka.sasl.jaas.config")).isEqualTo("org.apache.kafka.common.security.plain.PlainLoginModule required username='my-user' password='my-pass';");
    }
}
