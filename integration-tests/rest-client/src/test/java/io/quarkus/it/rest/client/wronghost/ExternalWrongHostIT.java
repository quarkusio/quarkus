package io.quarkus.it.rest.client.wronghost;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@Disabled("certificate expired")
public class ExternalWrongHostIT extends ExternalWrongHostTestCase {
}
