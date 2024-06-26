package io.quarkus.it.kafka;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@WithTestResource(value = KafkaKeycloakTestResource.class, restrictToAnnotatedClass = false)
public class KafkaOauthIT extends KafkaOauthTest {

}
