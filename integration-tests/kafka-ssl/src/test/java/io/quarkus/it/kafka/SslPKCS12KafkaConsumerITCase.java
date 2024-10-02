package io.quarkus.it.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@QuarkusTestResource(value = KafkaSSLTestResource.class, initArgs = {
        @ResourceArg(name = "kafka.tls-configuration-name", value = "custom-p12")
}, restrictToAnnotatedClass = true)
public class SslPKCS12KafkaConsumerITCase extends SslPKCS12KafkaConsumerTest {

}
