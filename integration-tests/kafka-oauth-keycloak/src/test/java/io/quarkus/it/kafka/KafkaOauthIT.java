package io.quarkus.it.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@QuarkusTestResource(KafkaKeycloakTestResource.class)
public class KafkaOauthIT extends KafkaOauthTest {

}
