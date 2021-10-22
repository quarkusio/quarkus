package io.quarkus.it.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@QuarkusTestResource(KafkaSSLTestResource.class)
public class SslKafkaConsumerITCase extends SslKafkaConsumerTest {

}
