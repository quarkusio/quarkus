package io.quarkus.it.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
@QuarkusTestResource(KafkaSSLTestResource.class)
public class SslKafkaConsumerITCase extends SslKafkaConsumerTest {

}
