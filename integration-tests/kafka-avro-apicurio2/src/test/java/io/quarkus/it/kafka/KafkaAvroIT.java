package io.quarkus.it.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
@QuarkusTestResource(KafkaAndSchemaRegistryTestResource.class)
public class KafkaAvroIT extends KafkaAvroTest {

}
