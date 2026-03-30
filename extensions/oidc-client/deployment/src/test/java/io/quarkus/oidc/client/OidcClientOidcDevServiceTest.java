package io.quarkus.oidc.client;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * This test uses OIDC Client extension with Dev Services for OIDC.
 */
public class OidcClientOidcDevServiceTest extends AbstractOidcClientDevServiceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = createQuarkusExtensionTest("oidc-client-oidc-dev-service-test.properties");

}
