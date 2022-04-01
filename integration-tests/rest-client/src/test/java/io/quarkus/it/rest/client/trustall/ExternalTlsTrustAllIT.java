package io.quarkus.it.rest.client.trustall;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@Disabled("certificate expired")
public class ExternalTlsTrustAllIT extends ExternalTlsTrustAllTestCase {
}
