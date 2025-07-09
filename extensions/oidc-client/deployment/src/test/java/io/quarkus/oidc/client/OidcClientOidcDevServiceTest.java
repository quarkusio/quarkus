package io.quarkus.oidc.client;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * This test uses OIDC Client extension with Dev Services for OIDC.
 */
public class OidcClientOidcDevServiceTest extends AbstractOidcClientDevServiceTest {

    @RegisterExtension
    static final QuarkusUnitTest test = createQuarkusUnitTest("oidc-client-oidc-dev-service-test.properties");

}
