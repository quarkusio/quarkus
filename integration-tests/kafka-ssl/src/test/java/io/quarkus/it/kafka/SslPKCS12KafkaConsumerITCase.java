package io.quarkus.it.kafka;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@WithTestResource(value = KafkaSSLTestResource.class, initArgs = {
        @ResourceArg(name = "kafka.tls-configuration-name", value = "custom-p12")
})
public class SslPKCS12KafkaConsumerITCase extends SslPKCS12KafkaConsumerTest {

}
