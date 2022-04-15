package io.quarkus.it.rest.client.selfsigned;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@Disabled("certificate expired")
public class ExternalSelfSignedITCase extends ExternalSelfSignedTestCase {

}
