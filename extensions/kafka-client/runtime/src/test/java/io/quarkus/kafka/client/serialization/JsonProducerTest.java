package io.quarkus.kafka.client.serialization;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonProducerTest {

    @Test
    public void shouldProduceJsonb() {
        Assertions.assertThat(JsonbProducer.get()).isNotNull();
    }

    @Test
    public void shouldProduceObjectMapper() {
        Assertions.assertThat(ObjectMapperProducer.get()).isNotNull();
    }
}