package io.quarkus.it.kafka;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@WithTestResource(value = KafkaSSLTestResource.class, restrictToAnnotatedClass = false)
public class SslKafkaConsumerITCase extends SslKafkaConsumerTest {

}
